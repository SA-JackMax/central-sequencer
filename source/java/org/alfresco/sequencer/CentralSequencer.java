package org.alfresco.sequencer;

import org.alfresco.service.cmr.repository.NodeRef;

import org.alfresco.service.namespace.QName;;

/**
 * 
 * @author Rui Fernandes
 *
 */
public interface CentralSequencer {
	

	public static final long ALREADY_SET=-1;
	public static final long NOT_SET=0;

	public long incrementProperty(NodeRef node, QName property);
}
