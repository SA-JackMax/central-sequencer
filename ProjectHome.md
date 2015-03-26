It makes available a sequence manager capable of updating numeric properties on your model according to sequences managed internally by the repository. Cluster aware locks during updating of the properties should be properly managed. It's already available a webscript (HTTP API) for updating node numeric properties through central sequences (each sequence is identified by the QName of the property). Just call:

`alfresco/s/sequencer?nodeRef=<nodeRef>&property=<full_property_qname>`

The property name is identified by its full name: {namespace}localName. The webscript will update the node property using the internal sequence which gets incremented for next updates of the same property for other nodes.

If the property is already set for the document nothing is done.