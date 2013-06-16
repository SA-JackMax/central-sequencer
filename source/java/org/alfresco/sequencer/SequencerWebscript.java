package org.alfresco.sequencer;

import java.io.IOException;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;
import org.springframework.extensions.webscripts.AbstractWebScript;
import org.springframework.extensions.webscripts.WebScriptException;
import org.springframework.extensions.webscripts.WebScriptRequest;
import org.springframework.extensions.webscripts.WebScriptResponse;

/**
 * 
 * @author Rui Fernandes
 * 
 */
public class SequencerWebscript extends AbstractWebScript {

	private static final String SET_ANSWER = "Property %s set to %s for node %s.";
	private static final String ALREADY_SET_ANSWER = "Node %s already had a value set for property %s";
	private static final String NOT_SET_ANSWER = "Could not set sequence number value for property %s for node %s.";
	private CentralSequencer centralSequencer;

	public void setCentralSequencer(CentralSequencer centralSequencer) {
		this.centralSequencer = centralSequencer;
	}

	@Override
	public void execute(WebScriptRequest req, WebScriptResponse res)
			throws IOException {
		try {
			String node = req.getParameter("nodeRef");
			String propertyName = req.getParameter("property");
			NodeRef nodeRef = new NodeRef(node);
			QName property = QName.createQName(propertyName);
			long response = centralSequencer.incrementProperty(nodeRef,
					property);
			formatAnswer(res, node, propertyName, response);
		} catch (Exception e) {
			throw new WebScriptException(500,
					"Error setting property on node through sequence.", e);
		}
	}

	private void formatAnswer(WebScriptResponse res, String node,
			String propertyName, long response) throws IOException {
		if (response == CentralSequencer.NOT_SET) {
			res.getWriter().write(
					String.format(NOT_SET_ANSWER, propertyName, node));
		} else if (response == CentralSequencer.ALREADY_SET) {
			res.getWriter().write(
					String.format(ALREADY_SET_ANSWER, node, propertyName));
		} else {
			res.getWriter().write(
					String.format(SET_ANSWER, propertyName, response, node));
		}
	}

}
