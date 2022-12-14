package org.pm4knime.node.logmanipulation.filter;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.pm4knime.node.logmanipulation.classify.ClassifyUtil;
import org.pm4knime.util.XLogUtil;
import org.processmining.log.utils.TraceVariantByClassifier;

import com.google.common.collect.ImmutableListMultimap;

public class XLogFilterUtil {
	public static XLog filterByTraceLength(XLog log, boolean isKeep, int minLen, int maxLen, ExecutionContext exec) throws CanceledExecutionException {
		
		XLog nlog = XLogUtil.clonePureLog(log, "event log filtered by trace length");
		if(isKeep) {
			for(XTrace trace : log) {
				exec.checkCanceled();
				if(trace.size() >= minLen && trace.size() <=maxLen) {
					nlog.add(trace);
				}
			}
	
		}else {
			for(XTrace trace : log) {
				exec.checkCanceled();
				if(trace.size() >= minLen && trace.size() <=maxLen) {
					continue;
				}else {
					nlog.add(trace);
				}
			}
		}
		return nlog;
	}

	public static XLog filterBySingleTVFreq(XLog log, boolean isKeep, int iThreshold, ExecutionContext exec) throws CanceledExecutionException {
		// TODO Auto-generated method stub
		XLog nlog = XLogUtil.clonePureLog(log, "event log filtered by trace frequency");
		
		
		ImmutableListMultimap<TraceVariantByClassifier, XTrace> variantsMap = 
				ClassifyUtil.getTraceVariant(log);
		
		if(isKeep) {
			for(TraceVariantByClassifier variant : variantsMap.keySet()) {
	    		exec.checkCanceled();
	    		List<XTrace> traceList = variantsMap.get(variant);
	    		
	    		if(traceList.size() >= iThreshold) {
					nlog.addAll(traceList);
	    		}
	    	}
		}else {
			for(TraceVariantByClassifier variant : variantsMap.keySet()) {
	    		exec.checkCanceled();
	    		List<XTrace> traceList = variantsMap.get(variant);
	    		
	    		if(traceList.size() < iThreshold) {
					nlog.addAll(traceList);
	    		}
	    	}
		}
    	
		return nlog;
	}

	/**
	 * we guarantee that to give the filtered event log at least the threshold required trace number. 
	 * For example, whole event log 100 trace, threshold 20, but top trace variant has size 25. 
	 * But we will give out the top trace variant with 25 out. 
	 * 
	 * If we need to remove the trace variant. We give the order like that, so If we want to remove the 20 trace variant
	 * we will 
	 * @param log
	 * @param isKeep
	 * @param iThreshold
	 * @param exec 
	 * @return
	 * @throws CanceledExecutionException 
	 */
	public static XLog filterByWholeLogFreq(XLog log, boolean isKeep, int iThreshold, ExecutionContext exec) throws CanceledExecutionException {
		// TODO Auto-generated method stub
		XLog nlog = XLogUtil.clonePureLog(log, "event log filtered by trace frequency");
		
		ImmutableListMultimap<TraceVariantByClassifier, XTrace> variantsMap = 
				ClassifyUtil.getTraceVariant(log);
		// sort the trace variant  by frequency
		exec.checkCanceled();
		Map<TraceVariantByClassifier, Collection<XTrace>> sortedMap = variantsMap.asMap().entrySet().stream()
				.sorted((e1, e2) -> e2.getValue().size() - e1.getValue().size())
				 .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (oldValue, newValue) -> oldValue, LinkedHashMap::new));
    	int sum = 0;
    	if(isKeep) {
    		for(TraceVariantByClassifier variant : sortedMap.keySet()) {
    			exec.checkCanceled();
        		List<XTrace> traceList = variantsMap.get(variant);
        		if(sum <= iThreshold) {
        			nlog.addAll(traceList);
        		}else
        			break;
        		sum += traceList.size();
    		}
    		
    	}else {
    		for(TraceVariantByClassifier variant : sortedMap.keySet()) {
    			exec.checkCanceled();
        		List<XTrace> traceList = variantsMap.get(variant);
        		if(sum <= iThreshold) {
        			sum += traceList.size();
        			continue ;
        		}else
        			nlog.addAll(traceList);
    		}
    	}
    	
		return nlog;
	}
}
