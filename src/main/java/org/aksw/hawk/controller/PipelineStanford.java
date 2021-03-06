package org.aksw.hawk.controller;

import java.util.List;

import org.aksw.hawk.datastructures.Answer;
import org.aksw.hawk.datastructures.HAWKQuestion;
import org.aksw.hawk.nlp.MutableTreePruner;
import org.aksw.hawk.querybuilding.Annotater;
import org.aksw.hawk.querybuilding.SPARQL;
import org.aksw.hawk.querybuilding.SPARQLQueryBuilder;
import org.aksw.hawk.spotter.Fox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PipelineStanford extends AbstractPipeline {
	static Logger log = LoggerFactory.getLogger(PipelineStanford.class);
	private Fox nerdModule;
	private MutableTreePruner pruner;
	private Annotater annotater;
	private SPARQLQueryBuilder queryBuilder;
	private Cardinality cardinality;
	private QueryTypeClassifier queryTypeClassifier;
	private StanfordNLPConnector stanfordConnector;

	public PipelineStanford() {
		queryTypeClassifier = new QueryTypeClassifier();

		nerdModule = new Fox();
		// controller.nerdModule = new Spotlight();
		// controller.nerdModule =new TagMe();
		// controller.nerdModule = new MultiSpotter(fox, tagMe, wiki, spot);

		this.stanfordConnector = new StanfordNLPConnector();
		cardinality = new Cardinality();

		pruner = new MutableTreePruner();

		SPARQL sparql = new SPARQL();
		annotater = new Annotater(sparql);

		queryBuilder = new SPARQLQueryBuilder(sparql);
	}

	public List<Answer> getAnswersToQuestion(HAWKQuestion q) {
		log.info("Question: " + q.getLanguageToQuestion().get("en"));

		log.info("Classify question type.");
		q.setIsClassifiedAsASKQuery(queryTypeClassifier.isASKQuery(q.getLanguageToQuestion().get("en")));

		// Disambiguate parts of the query
		log.info("Named entity recognition.");
		q.setLanguageToNamedEntites(nerdModule.getEntities(q.getLanguageToQuestion().get("en")));

		// Noun combiner, decrease #nodes in the DEPTree
		log.info("Noun phrase combination / Dependency Parsing");
		// TODO make this method return the combine sequence and work on this,
		// i.e., q.sequence = sentenceToSequence.combineSequences(q);

		// @Ricardo this will calculate cardinality of reduced(combinedNN) tree.
		// is this right?
		q.setTree(stanfordConnector.combineSequences(q));

		// Cardinality identifies the integer i used for LIMIT i
		log.info("Cardinality calculation.");
		q.setCardinality(cardinality.cardinality(q));

		// Apply pruning rules
		log.info("Pruning tree.");
		q.setTree(pruner.prune(q));

		// Annotate tree
		log.info("Semantically annotating the tree.");
		annotater.annotateTree(q);

		// Calculating all possible SPARQL BGPs with given semantic annotations
		log.info("Calculating SPARQL representations.");
		List<Answer> answers = queryBuilder.build(q);

		return answers;
	}

	public static void main(String[] args) {
		PipelineStanford p = new PipelineStanford();
		HAWKQuestion q = new HAWKQuestion();
		q.getLanguageToQuestion().put("en", "Which anti-apartheid activist was born in Mvezo?");
		p.getAnswersToQuestion(q);

	}

}
