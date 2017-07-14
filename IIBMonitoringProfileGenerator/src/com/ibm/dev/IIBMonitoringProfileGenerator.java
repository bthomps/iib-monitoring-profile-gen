
package com.ibm.dev;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Enumeration;
import com.ibm.broker.config.proxy.BrokerProxy;
import com.ibm.broker.config.proxy.ConfigManagerProxyLoggedException;
import com.ibm.broker.config.proxy.ConfigManagerProxyPropertyNotInitializedException;
import com.ibm.broker.config.proxy.ExecutionGroupProxy;
import com.ibm.broker.config.proxy.IntegrationNodeConnectionParameters;
import com.ibm.broker.config.proxy.MessageFlowProxy;
import com.ibm.broker.config.proxy.MessageFlowProxy.Node;
import com.ibm.broker.config.proxy.RestApiProxy;
import com.ibm.broker.config.proxy.SubFlowProxy;

public class IIBMonitoringProfileGenerator {

	private static final String DEFAULT_BROKER_NAME = "TESTNODE_gb043390";
	
	public static void main(String[] args) {

		String brokerParameter = null;
        String brokerHost = null;
        int    brokerPort = 0;       
	    // Parse the command line arguments
	    for (int i=0; i<args.length; i++) {
	    	if ((args[i].equals("-h")) || (args[i].equals("/h"))) {
	    		System.out.println("TO DO: Need to display Usage Info!");
	    	} else {
	            if (brokerParameter == null) {
	                brokerParameter = args[i];
                    String tokens[] = brokerParameter.split(":",2);
                    if(tokens.length == 2)
	                    {
	                      brokerHost = tokens[0];
	                      brokerPort = Integer.parseInt(tokens[1]);
	                    }	                
	            }	
	    	}
	    }	    
		BrokerProxy b;		
		try {			
			if (brokerPort > 0) {			
				b = BrokerProxy.getInstance(new IntegrationNodeConnectionParameters(brokerHost, brokerPort));
			} else { 
				b = BrokerProxy.getLocalInstance(DEFAULT_BROKER_NAME);
			}
			ExecutionGroupProxy e = b.getExecutionGroupByName("default");		    
			Properties filter = new Properties();
			Enumeration<RestApiProxy> allRestApisInThisEG = e.getRestApis(filter);
            while (allRestApisInThisEG.hasMoreElements()) {
                RestApiProxy thisRestApi = allRestApisInThisEG.nextElement();
                System.out.println(createMonitoringProfileXMLForRestApi(thisRestApi));
            }					 
		} catch (ConfigManagerProxyLoggedException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (ConfigManagerProxyPropertyNotInitializedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
}


	private static String createMonitoringProfileXMLForRestApi (RestApiProxy thisRestApi) {
		
		String xmlProfileStart = "    <p:monitoringProfile xmlns:p=\"http://www.ibm.com/xmlns/prod/websphere/messagebroker/6.1.0.3/monitoring/profile\" p:version=\"2.0\">\n";
		String eventSourceTemplate =
"	    <p:eventSource p:enabled=\"true\" p:eventSourceAddress=\"TEMPLATENODENAME.terminal.out\">\n"+
"	        <p:eventPointDataQuery>\n"+
"	            <p:eventIdentity>\n"+
"	                <p:eventName p:literal=\"\" p:queryText=\"\"/>\n"+
"	            </p:eventIdentity>\n"+
"	            <p:eventCorrelation>\n"+
"	                <p:localTransactionId p:queryText=\"\" p:sourceOfId=\"automatic\"/>\n"+
"	                <p:parentTransactionId p:queryText=\"\" p:sourceOfId=\"automatic\"/>\n"+
"	                <p:globalTransactionId p:queryText=\"\" p:sourceOfId=\"automatic\"/>\n"+
"	            </p:eventCorrelation>\n"+
"	           <p:eventFilter p:queryText=\"true()\"/>\n"+           
"	           <p:eventUOW p:unitOfWork=\"messageFlow\" />\n"+
"	        </p:eventPointDataQuery>\n"+
"	        <p:bitstreamDataQuery p:bitstreamContent=\"all\" p:encoding=\"base64Binary\"/>\n"+
"	    </p:eventSource>\n";
		String xmlProfileEnd = "    </p:monitoringProfile>";
		
		try {
	        Map<String, String> subflowMap = new HashMap<String, String>();
			MessageFlowProxy topLevelFlow = thisRestApi.getMessageFlowByName("gen."+thisRestApi.getName());
			Enumeration<Node> topLevelFlowNodes = topLevelFlow.getNodes();
			while (topLevelFlowNodes.hasMoreElements()) {									
				Node currentNode = topLevelFlowNodes.nextElement();
				String currentNodeName = currentNode.getName();
				String currentNodeType = currentNode.getType();
				if (currentNodeType.equals("SubFlowNode")) {					
					Properties props = currentNode.getProperties();
					String subflowImplementationFileName = props.getProperty("subflowImplFile");
					subflowMap.put(subflowImplementationFileName.substring(0,subflowImplementationFileName.indexOf(".subflow")), currentNodeName);
				}
			}			
			Properties filter = new Properties();
			Enumeration<SubFlowProxy> subflows = thisRestApi.getSubFlows(filter);			
			while (subflows.hasMoreElements()) {
				SubFlowProxy thisSubflow = subflows.nextElement();								
				Enumeration<Node> nodes = thisSubflow.getNodes();
				while (nodes.hasMoreElements()) {									
					Node currentNode = nodes.nextElement();
					String currentNodeType = currentNode.getType(); 
					if (!currentNodeType.equals("InputNode") && !currentNodeType.equals("OutputNode") ) {										
						// We don't need to add entries to the monitoring profile for Input and Output nodes in subflows						
						String eventSourceAddress = subflowMap.get(thisSubflow.getName())+"."+currentNode.getName();
						xmlProfileStart = xmlProfileStart + eventSourceTemplate.replace("TEMPLATENODENAME", eventSourceAddress);		
					}				
				}				
			}			
		} catch (ConfigManagerProxyPropertyNotInitializedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}				
		return (xmlProfileStart+xmlProfileEnd);
	}		
}




    
    
    
    