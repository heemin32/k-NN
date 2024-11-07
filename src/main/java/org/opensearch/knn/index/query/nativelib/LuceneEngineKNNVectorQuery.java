/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.knn.index.query.nativelib;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.QueryTimeout;
import org.apache.lucene.search.DocIdSetIterator;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.KnnFloatVectorQuery;
import org.apache.lucene.search.MatchNoDocsQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreMode;
import org.apache.lucene.search.Scorer;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.Weight;
import org.apache.lucene.search.join.BitSetProducer;
import org.apache.lucene.search.join.DiversifyingChildrenFloatKnnVectorQuery;
import org.apache.lucene.util.BitSet;
import org.apache.lucene.util.DocIdSetBuilder;
import org.opensearch.knn.index.query.ResultUtil;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

public class LuceneEngineKNNVectorQuery extends DiversifyingChildrenFloatKnnVectorQuery {
	private final BitSetProducer parentsFilter;
	private final Query childFilter;
	private final int k;
	private final float[] query;
	private final LuceneKnnFloatVectorQuery knnFloatVectorQuery;
	private boolean doReWrite;

	public LuceneEngineKNNVectorQuery(String field, float[] query, Query childFilter, int k, BitSetProducer parentsFilter) {
		super(field, query, childFilter, k, parentsFilter);
		this.childFilter = childFilter;
		this.parentsFilter = parentsFilter;
		this.k = k;
		this.query = query;
		this.knnFloatVectorQuery = new LuceneKnnFloatVectorQuery(field, query, 1000);
	}

	@Override
	public Query rewrite(IndexSearcher indexSearcher) throws IOException {
		return doReWrite ? super.rewrite(indexSearcher) : this;
	}

	private Query doReWrite(IndexSearcher indexSearcher) throws IOException {
		try {
			doReWrite = true;
			return indexSearcher.rewrite(this);
		} finally {
			doReWrite = false;
		}
	}

	@Override
	public Weight createWeight(IndexSearcher indexSearcher, ScoreMode scoreMode, float boost) throws IOException {
		final IndexReader reader = indexSearcher.getIndexReader();
		Query docAndScoreQuery = doReWrite(indexSearcher);
		Weight weight = docAndScoreQuery.createWeight(indexSearcher, scoreMode, boost);
		List<LeafReaderContext> leafReaderContexts = reader.leaves();
		List<Map<Integer, Float>> perLeafResults;
		perLeafResults = doSearch(indexSearcher, leafReaderContexts, weight, k);
		ResultUtil.reduceToTopK(perLeafResults, k);
		TopDocs[] topDocs = retrieveAll(indexSearcher, leafReaderContexts, weight, perLeafResults);
		long sum = 0;
		for (TopDocs topDoc : topDocs) {
			sum += topDoc.totalHits.value;
		}
		TopDocs topK = TopDocs.merge((int)sum, topDocs);
		if (topK.scoreDocs.length == 0) {
			return new MatchNoDocsQuery().createWeight(indexSearcher, scoreMode, boost);
		}

		return createDocAndScoreQuery(reader, topK).createWeight(indexSearcher, scoreMode, boost);
	}

	private TopDocs[] retrieveAll(final IndexSearcher indexSearcher, final List<LeafReaderContext> leafReaderContexts, final Weight weight, final List<Map<Integer, Float>> perLeafResults) throws IOException {
		// Construct query
		List<Callable<TopDocs>> rescoreTasks = new ArrayList<>(leafReaderContexts.size());
		for (int i = 0; i < perLeafResults.size(); i++) {
			LeafReaderContext leafReaderContext = leafReaderContexts.get(i);
			int finalI = i;
			rescoreTasks.add(() -> {
				// Here, it should be bitset of parent doc id.
				DocIdSetIterator iterator = getAllSiblings(leafReaderContext, perLeafResults.get(finalI));
				return knnFloatVectorQuery.exactSearch(leafReaderContext, iterator, null);
			});
		}
		return indexSearcher.getTaskExecutor().invokeAll(rescoreTasks).toArray(TopDocs[]::new);
	}

	private DocIdSetIterator getAllSiblings(final LeafReaderContext leafReaderContext, final Map<Integer, Float> integerFloatMap) throws IOException {
		if (integerFloatMap.isEmpty()) {
			return DocIdSetIterator.empty();
		}
		final int maxDoc = Collections.max(integerFloatMap.keySet());
		BitSet parentBitSet = parentsFilter.getBitSet(leafReaderContext);
		final int maxParentDoc = parentBitSet.nextSetBit(maxDoc) + 1;
		return resultMapToDocIds(leafReaderContext, integerFloatMap, maxParentDoc);
	}

	public DocIdSetIterator resultMapToDocIds(final LeafReaderContext leafReaderContext, Map<Integer, Float> resultMap, final int maxDoc) throws IOException {
		if (resultMap.isEmpty()) {
			return DocIdSetIterator.empty();
		}
		final DocIdSetBuilder docIdSetBuilder = new DocIdSetBuilder(maxDoc);
		final DocIdSetBuilder.BulkAdder setAdder = docIdSetBuilder.grow(maxDoc - resultMap.size());
		BitSet parentBitSet = parentsFilter.getBitSet(leafReaderContext);
		resultMap.keySet().forEach(key -> {
			for (int i = parentBitSet.prevSetBit(key) + 1; i < parentBitSet.nextSetBit(key); i++) {
				setAdder.add(i);
			}
		});
		return docIdSetBuilder.build().iterator();
	}

	private Query createDocAndScoreQuery(IndexReader reader, TopDocs topK) {
		int len = topK.scoreDocs.length;
		Arrays.sort(topK.scoreDocs, Comparator.comparingInt(a -> a.doc));
		int[] docs = new int[len];
		float[] scores = new float[len];
		for (int i = 0; i < len; i++) {
			docs[i] = topK.scoreDocs[i].doc;
			scores[i] = topK.scoreDocs[i].score;
		}
		int[] segmentStarts = findSegmentStarts(reader, docs);
		return new DocAndScoreQuery(k, docs, scores, segmentStarts, reader.getContext().id());
	}

	private List<Map<Integer, Float>> doSearch(
		final IndexSearcher indexSearcher,
		List<LeafReaderContext> leafReaderContexts,
		Weight weight,
		int k
	) throws IOException {
		List<Callable<Map<Integer, Float>>> tasks = new ArrayList<>(leafReaderContexts.size());
		for (LeafReaderContext leafReaderContext : leafReaderContexts) {
			tasks.add(() -> searchLeaf(leafReaderContext, weight, k));
		}
		return indexSearcher.getTaskExecutor().invokeAll(tasks);
	}

	static int[] findSegmentStarts(IndexReader reader, int[] docs) {
		int[] starts = new int[reader.leaves().size() + 1];
		starts[starts.length - 1] = docs.length;
		if (starts.length == 2) {
			return starts;
		}
		int resultIndex = 0;
		for (int i = 1; i < starts.length - 1; i++) {
			int upper = reader.leaves().get(i).docBase;
			resultIndex = Arrays.binarySearch(docs, resultIndex, docs.length, upper);
			if (resultIndex < 0) {
				resultIndex = -1 - resultIndex;
			}
			starts[i] = resultIndex;
		}
		return starts;
	}

	private Map<Integer, Float> searchLeaf(LeafReaderContext ctx, Weight weight, int k) throws IOException {
		final Map<Integer, Float> leafDocScores =  new HashMap<>();
		Scorer scorer = weight.scorer(ctx);
		DocIdSetIterator iterator = scorer.iterator();
		iterator.nextDoc();
		while (iterator.docID() != DocIdSetIterator.NO_MORE_DOCS) {
			leafDocScores.put(scorer.docID(), scorer.score());
			iterator.nextDoc();
		}
		return leafDocScores;
	}

	public class LuceneKnnFloatVectorQuery extends KnnFloatVectorQuery {

		public LuceneKnnFloatVectorQuery(final String field, final float[] target, final int k) {
			super(field, target, k);
		}

		@Override
		protected TopDocs exactSearch(final LeafReaderContext context, final DocIdSetIterator acceptIterator, final QueryTimeout queryTimeout) throws IOException {
			return super.exactSearch(context, acceptIterator, queryTimeout);
		}
	}
}
