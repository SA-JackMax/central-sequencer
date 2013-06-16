package org.alfresco.sequencer;

import java.io.Serializable;

import org.alfresco.repo.transaction.RetryingTransactionHelper.RetryingTransactionCallback;
import org.alfresco.service.cmr.attributes.AttributeService;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;

import org.alfresco.service.namespace.QName;
import org.alfresco.service.transaction.TransactionService;

/**
 * 
 * @author Rui Fernandes
 * 
 */
public class CentralSequencerImpl implements CentralSequencer {

	private AttributeService attributeService;
	private NodeService nodeService;
	private TransactionService transactionService;
	private int lockTries = 5;
	private long waitTime = 100;

	public void setWaitTime(long waitTime) {
		this.waitTime = waitTime;
	}

	public void setLockTries(int lockTries) {
		this.lockTries = lockTries;
	}

	public void setTransactionService(TransactionService transactionService) {
		this.transactionService = transactionService;
	}

	public void setAttributeService(AttributeService attributeService) {
		this.attributeService = attributeService;
	}

	public void setNodeService(NodeService nodeService) {
		this.nodeService = nodeService;
	}

	private static String getAttributeLockName(QName property) {
		return property.getPrefixString() + property.getLocalName()
				+ "-sequence-lock";
	}

	@Override
	public long incrementProperty(NodeRef node, QName property) {
		final String attributeLockName = getAttributeLockName(property);
		long result = NOT_SET;

		for (int i = 0; i < lockTries && result == NOT_SET; i++) {
			result = updateProperty(node, property, attributeLockName);
		}

		return result == NOT_SET ? lockAndUpdateProperty(node, property,
				attributeLockName) : result;

	}

	private long updateProperty(NodeRef node, QName property,
			final String attributeLockName) {
		Serializable lock = attributeService.getAttribute(attributeLockName);
		if (lock == null) {
			return lockAndUpdateProperty(node, property, attributeLockName);
		} else {
			sleep();
		}
		return NOT_SET;
	}

	private void sleep() {
		try {
			Thread.sleep(waitTime);
		} catch (InterruptedException e) {
			// do nothing
		}
	}

	private long lockAndUpdateProperty(NodeRef node, QName property,
			final String attributeLockName) {
		long lock = lockPropertySequencer(attributeLockName);
		try {
			return updatePropertyWithSequencer(node, property);
		} finally {
			unlockPropertySequencer(attributeLockName, lock);
		}
	}

	private void unlockPropertySequencer(final String attributeLockName,
			final long lock) {
		RetryingTransactionCallback<Object> work = new RetryingTransactionCallback<Object>() {
			public Object execute() throws Exception {
				Serializable lock1 = attributeService
						.getAttribute(attributeLockName);
				if (lock1 != null && ((Long) lock1).longValue() == lock) {
					attributeService.removeAttribute(attributeLockName);
				}
				return null;
			}
		};
		transactionService.getRetryingTransactionHelper().doInTransaction(work,
				false, true);

	}

	private long updatePropertyWithSequencer(NodeRef node, QName property) {
		Serializable value=nodeService.getProperty(node, property);
		if(value!=null){
			return ALREADY_SET;
		}
		final String attributeSequenceName = getAttributeSequenceName(property);
		Serializable sequence = attributeService
				.getAttribute(attributeSequenceName);
		final long sequenceNumber = sequence == null ? 1 : ((Long) sequence)
				.longValue() + 1;
		nodeService.setProperty(node, property, new Long(sequenceNumber));
		incrementSequence(attributeSequenceName, sequenceNumber);
		return sequenceNumber;
	}

	private void incrementSequence(final String attributeSequenceName,
			final long sequenceNumber) {
		RetryingTransactionCallback<Object> work = new RetryingTransactionCallback<Object>() {
			public Object execute() throws Exception {
				attributeService.setAttribute(sequenceNumber,attributeSequenceName);
				return null;
			}
		};
		transactionService.getRetryingTransactionHelper().doInTransaction(work,
				false, true);
	}

	private String getAttributeSequenceName(QName property) {
		return property.getPrefixString() + property.getLocalName()
				+ "-sequence";
	}

	private long lockPropertySequencer(final String attributeLockName) {
		final long lock1 = System.currentTimeMillis();
		RetryingTransactionCallback<Object> work = new RetryingTransactionCallback<Object>() {
			public Object execute() throws Exception {
				attributeService.setAttribute(lock1,attributeLockName);
				return null;
			}
		};
		transactionService.getRetryingTransactionHelper().doInTransaction(work,
				false, true);
		return lock1;
	}

}
