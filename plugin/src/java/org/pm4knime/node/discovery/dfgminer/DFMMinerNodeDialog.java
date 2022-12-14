package org.pm4knime.node.discovery.dfgminer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.DialogComponentStringSelection;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.port.PortObjectSpec;
import org.pm4knime.portobject.XLogPortObjectSpec;
import org.pm4knime.util.XLogSpecUtil;
import org.pm4knime.util.defaultnode.DefaultMinerNodeDialog;

/**
 * <code>NodeDialog</code> for the "DFMMiner" node.
 * 
 * @author Kefang Ding
 */
public class DFMMinerNodeDialog extends DefaultMinerNodeDialog {
	// set the variants to convert the directly-follows graph 
	SettingsModelString m_variant;

	@Override
	public void init() {
		m_variant = new SettingsModelString(DFMMinerNodeModel.CFG_VARIANT_KEY, DFMMinerNodeModel.CFG_VARIANT_VALUES[0]);
		// the variants values are fixed
		DialogComponentStringSelection variantComp = new DialogComponentStringSelection(m_variant, "Variant",
				DFMMinerNodeModel.CFG_VARIANT_VALUES);
    	addDialogComponent(variantComp);
    	
	}
	
	@Override
    public void loadAdditionalSettingsFrom(final NodeSettingsRO settings,
            final PortObjectSpec[] specs) throws NotConfigurableException {
		try {
			
			classifierSet.loadSettingsFrom(settings);
    		List<String> configClassifierSet = Arrays.asList(classifierSet.getStringArrayValue());
    		
    		XLogPortObjectSpec logSpec = (XLogPortObjectSpec) specs[0];
			List<String> specClassifierSet = new ArrayList<String>(
					XLogSpecUtil.getClassifierWithClsList(logSpec.getClassifiersMap()));
			
			specClassifierSet.addAll(DFMMinerNodeModel.sClfNames);
			
			if(! configClassifierSet.containsAll(specClassifierSet) 
		    		 ||	!specClassifierSet.containsAll(configClassifierSet)){
				
				classifierSet.setStringArrayValue(specClassifierSet.toArray(new String[0]));
			}
			
			classifierComp.replaceListItems(logSpec.getClassifiersMap().keySet(), 
					logSpec.getClassifiersMap().keySet().iterator().next());
			m_classifier.loadSettingsFrom(settings);
			
		} catch (InvalidSettingsException | NullPointerException e ) {
			// TODO Auto-generated catch block
			throw new NotConfigurableException("Please make sure the connected event log in excution state");
			
		}
	}
	
	
}

