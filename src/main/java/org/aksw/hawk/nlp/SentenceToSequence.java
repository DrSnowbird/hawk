package org.aksw.hawk.nlp;

import java.util.List;
import java.util.Queue;
import java.util.Stack;

import org.aksw.autosparql.commons.qald.Question;
import org.aksw.hawk.index.DBAbstractsIndex;
import org.aksw.hawk.nlp.posTree.MutableTreeNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.clearnlp.tokenization.EnglishTokenizer;
import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.google.common.collect.Queues;

public class SentenceToSequence {
	Logger log = LoggerFactory.getLogger(SentenceToSequence.class);
	DBAbstractsIndex index = new DBAbstractsIndex();

	// combine noun phrases
	// TODO improve nounphrases e.g. combine following nulls, i.e., URLs to get
	// early life of Jane Austin instead of early life
	public void combineSequences(Question q) {
		String question = q.languageToQuestion.get("en");
		EnglishTokenizer tok = new EnglishTokenizer();
		List<String> list = tok.getTokens(question);
		List<String> subsequence = Lists.newArrayList();
		for (int tcounter = 0; tcounter < list.size(); tcounter++) {
			String token = list.get(tcounter);
			// look for start "RB|JJ|NN(.)*"
			if (subsequence.isEmpty() && null != pos(token, q) && pos(token, q).matches("RB|JJ|NN(.)*")) {
				subsequence.add(token);
			}
			// split "of the" or "of all" via pos_i=IN and pos_i+1=DT
			else if (!subsequence.isEmpty() && null != pos(token, q) && tcounter + 1 < list.size() && null != pos(list.get(tcounter + 1), q) && pos(token, q).matches("IN") && pos(list.get(tcounter + 1), q).matches("DT")) {
				if (subsequence.size() > 2) {
					transformTree(subsequence, q);
				}
				subsequence = Lists.newArrayList();
			}
			// do not combine NN and NNP+, e.g., the opera Madame Butterfly
			else if (!subsequence.isEmpty() && null != pos(token, q) && null != pos(list.get(tcounter - 1), q) && pos(list.get(tcounter - 1), q).matches("NN(S)?") && pos(token, q).matches("NNP(S)?")) {
				if (subsequence.size() > 2) {
					transformTree(subsequence, q);
				}
				subsequence = Lists.newArrayList();
			}
			// finish via VB* or IN -> null or IN -> DT
			else if (!subsequence.isEmpty() && (null == pos(token, q) || pos(token, q).matches("VB(.)*|\\.") || (pos(token, q).matches("IN") && pos(list.get(tcounter + 1), q) == null) || (pos(token, q).matches("IN") && pos(list.get(tcounter + 1), q).matches("DT")))) {
				// more than one token, so summarizing makes sense
				if (subsequence.size() > 1) {
					transformTree(subsequence, q);
				}
				subsequence = Lists.newArrayList();
			}
			// continue via "NN(.)*|RB|CD|CC|JJ|DT|IN|PRP|HYPH"
			else if (!subsequence.isEmpty() && null != pos(token, q) && pos(token, q).matches("NN(.)*|RB|CD|CC|JJ|DT|IN|PRP|HYPH")) {
				subsequence.add(token);
			} else {
				subsequence = Lists.newArrayList();
			}

		}

	}

	private void transformTree(List<String> subsequence, Question q) {
		String newLabel = Joiner.on(" ").join(subsequence);
		MutableTreeNode top = findTopMostNode(q.tree.getRoot(), subsequence);
		//if top node equals null the target subsequence is not in one dependence subtree and thus will not be joined together
		if(top==null){
			return;
		}
		for (String sub : subsequence) {
			Queue<MutableTreeNode> queue = Queues.newLinkedBlockingQueue();
			queue.add(q.tree.getRoot());
			while (!queue.isEmpty()) {
				MutableTreeNode tmp = queue.poll();
				if (tmp.label.equals(sub) && !tmp.equals(top)) {
					q.tree.remove(tmp);
					break;
				} else {
					for (MutableTreeNode n : tmp.getChildren()) {
						queue.add(n);
					}
				}
			}
		}
		top.label = newLabel;
		top.posTag = "CombinedNN";
	}

	// use breadth first search to find the top most node
	private MutableTreeNode findTopMostNode(MutableTreeNode root, List<String> tokenSet) {
		Queue<MutableTreeNode> queue = Queues.newLinkedBlockingQueue();
		queue.add(root);
		while (!queue.isEmpty()) {
			MutableTreeNode tmp = queue.poll();
			for (String token : tokenSet) {
				if (token.equals(tmp.label)) {
					if (subTreeContainsOtherToken(tmp, tokenSet)) {
						return tmp;
					}
				}
			}
			for (MutableTreeNode n : tmp.getChildren()) {
				queue.add(n);
			}
		}
		return null;
	}

	/**
	 * to match correct subtree it is important to find the top most node in the
	 * subtree that contains every token of the combined noun
	 * 
	 * @param root
	 * @param tokenSet
	 * @return
	 */
	private boolean subTreeContainsOtherToken(MutableTreeNode root, List<String> tokenSet) {
		boolean[] tokenCheck = new boolean[tokenSet.size()];
		for (int i = 0; i < tokenSet.size(); i++) {
			Queue<MutableTreeNode> queue = Queues.newLinkedBlockingQueue();
			queue.add(root);
			while (!queue.isEmpty()) {
				MutableTreeNode tmp = queue.poll();
				if (tokenSet.get(i).equals(tmp.label)) {
					tokenCheck[i] = true;
				}
				for (MutableTreeNode n : tmp.getChildren()) {
					queue.add(n);
				}

			}
		}
		for (boolean item : tokenCheck) {
			if (!item) {
				return false;
			}
		}
		return true;
	}

	private String pos(String token, Question q) {
		Stack<MutableTreeNode> stack = new Stack<MutableTreeNode>();
		stack.push(q.tree.getRoot());
		while (!stack.isEmpty()) {
			MutableTreeNode tmp = stack.pop();
			if (tmp.label.equals(token)) {
				return tmp.posTag;
			} else {
				for (MutableTreeNode child : tmp.getChildren()) {
					stack.push(child);
				}
			}

		}
		return null;
	}

}