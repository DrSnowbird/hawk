package org.aksw.hawk.util;

import java.io.File;
import java.io.IOException;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.aksw.hawk.controller.Answer;
import org.aksw.hawk.querybuilding.SPARQLQuery;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.impl.ResourceImpl;

public class QALDWriter {
	private String dataset;
	private List<Element> questions;
	private Document doc;

	public QALDWriter(String dataset) throws IOException, ParserConfigurationException {
		this.dataset = dataset;
		questions = Lists.newArrayList();
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		DocumentBuilder db = dbf.newDocumentBuilder();
		doc = db.newDocument();

	}

	public static void main(String[] args) throws IOException, ParserConfigurationException, TransformerFactoryConfigurationError, TransformerException {

		String dataset = "qald-5_train";
		QALDWriter qw = new QALDWriter(dataset);
		SPARQLQuery query = new SPARQLQuery("?const <http://dbpedia.org/ontology/starring> ?proj.");
		query.addFilterOverAbstractsContraint("?proj", "Coquette Productions");
		Answer a = new Answer();
		a.answerSet = Sets.newHashSet();
		a.answerSet.add(new ResourceImpl("http://dbpedia.org/resource/1"));
		a.answerSet.add(new ResourceImpl("http://dbpedia.org/resource/2"));
		a.query = query;
		a.question = "Where was the assassin of Martin Luther King born?";
		a.question_id = "1";
		qw.write(a);
		qw.close();
	}

	private void close() throws IOException, TransformerFactoryConfigurationError, TransformerException {
		Element root = doc.createElement("dataset");
		root.setAttribute("id", dataset);
		doc.appendChild(root);
		for (Element question : questions) {
			root.appendChild(question);
		}

		Transformer transformer = TransformerFactory.newInstance().newTransformer();
		transformer.setOutputProperty(OutputKeys.INDENT, "yes");
		DOMSource source = new DOMSource(doc);
		StreamResult file = new StreamResult(new File(dataset + "_answer.xml"));
		transformer.transform(source, file);

		System.out.println("\nXML DOM Created Successfully..");
	}

	public void write(Answer a) throws ParserConfigurationException, IOException {

		Element question = doc.createElement("question");
		if (a.question_id != null) {
			question.setAttribute("id", a.question_id);
		}
		// TODO adapt to be more flexible
		question.setAttribute("answertype", "resouce");
		question.setAttribute("aggregation", "false");
		question.setAttribute("onlydbo", "true");
		question.setAttribute("hybrid", "true");

		Element string = doc.createElement("string");
		string.setAttribute("lang", "en");
		string.setTextContent(a.question);
		question.appendChild(string);

		Element pseudoquery = doc.createElement("pseudoquery");
		pseudoquery.setTextContent(a.query.toString());
		question.appendChild(pseudoquery);

		Element answers = doc.createElement("answers");

		for (RDFNode node : a.answerSet) {
			Element answer = doc.createElement("answer");
			answer.setTextContent(node.asResource().getURI());
			answers.appendChild(answer);
		}
		question.appendChild(answers);
		questions.add(question);
	}
}