package org.pm4knime.node.conformance.replayer;

import java.awt.Dimension;
import java.util.List;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.table.DefaultTableModel;

import org.deckfour.xes.classification.XEventClassifier;
import org.deckfour.xes.classification.XEventNameClassifier;
import org.deckfour.xes.model.XLog;
import org.knime.core.node.DataAwareNodeDialogPane;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.DialogComponent;
import org.knime.core.node.defaultnodesettings.DialogComponentNumberEdit;
import org.knime.core.node.defaultnodesettings.DialogComponentStringSelection;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.pm4knime.portobject.PetriNetPortObject;
import org.pm4knime.portobject.XLogPortObject;
import org.pm4knime.settingsmodel.SMAlignmentReplayParameter;
import org.pm4knime.settingsmodel.SMAlignmentReplayParameterWithCT;
import org.pm4knime.util.PetriNetUtil;
import org.pm4knime.util.ReplayerUtil;
import org.pm4knime.util.XLogUtil;
import org.processmining.acceptingpetrinet.models.AcceptingPetriNet;

/**
 * <code>NodeDialog</code> for the "PNReplayer" node. 
 * This dialog incldues the cost tables for each event class and transition. 
 * 
 * @author Kefang Ding
 */
public class PNReplayerNodeDialog extends DataAwareNodeDialogPane {

	protected JPanel m_compositePanel;
	protected SMAlignmentReplayParameterWithCT m_parameter;
	String[] strategyList = ReplayerUtil.strategyList;
    /**
     * New pane for configuring the PNReplayer node.
     */
    protected PNReplayerNodeDialog() {
    	m_compositePanel = new JPanel();
        m_compositePanel.setLayout(new BoxLayout(m_compositePanel,
                BoxLayout.Y_AXIS));
    	
        specialInit();
        
    	super.addTab("Options", m_compositePanel);
    }
    
    protected void specialInit() {
		// TODO Auto-generated method stub
    	m_parameter  = new SMAlignmentReplayParameterWithCT(PNReplayerNodeModel.CFG_PARAMETER_NAME);
    	
    	// TODO : remove tmp. just use the old parameter here
    	SMAlignmentReplayParameterWithCT tmp =  m_parameter;
		commonInitPanel(m_parameter);
		
		// here to add special codes from the additional items
    	Box tbox = new Box(BoxLayout.Y_AXIS);
    	for( int i=0; i< SMAlignmentReplayParameterWithCT.CFG_COST_TYPE_NUM ; i++) {
    		tbox.add(createTable(i));
		}
    	
    	m_compositePanel.add(tbox);
    	
    	// add action listener. If the value of defaultCost changes, the table values should changes, too. 
    	// to deal with log move at first. 
    	for(int i=0; i< SMAlignmentReplayParameter.CFG_COST_TYPE_NUM ; i++) {
    		DefaultTableModel tTable = tmp.getMCostTMs()[i];
    		// int oldValue = tmp.getMDefaultCosts()[i].getIntValue();
    		final int idx = i;
    		// the mistake made is to refer the oldValue before the changeListener 
    		tmp.getMDefaultCosts()[i].addChangeListener(new ChangeListener() {

				@Override
				public void stateChanged(ChangeEvent e) {
					// TODO change the CostTable values
					// System.out.println("Old value is "+ oldValue);
					int newValue = tmp.getMDefaultCosts()[idx].getIntValue();
					// System.out.println("Current value is "+ newValue);
					// here we need to adjust the value due to the tau transitions in Petri net
					// set them to zero. 
					for(int rIdx =0 ; rIdx< tTable.getRowCount(); rIdx++) {
						if(idx == 1) {
							// in Petri net table 
							String tName = (String) tTable.getValueAt(rIdx, 0);
			                if(PetriNetUtil.isTauName(tName)) {
			                	tTable.setValueAt(0, rIdx, 1);
			                	continue;
			                }
						}
						tTable.setValueAt(newValue, rIdx, 1);
					}
                	
                	tTable.fireTableDataChanged();
				}
    			
    		});
    		
    			
    	}
    	
	}
    
    protected void commonInitPanel(SMAlignmentReplayParameter parameter) {
    	List<String> classifierNames  =  XLogUtil.getECNames(DefaultPNReplayerNodeModel.classifierList);
    	
    	parameter.getMClassifierName().setStringValue(classifierNames.get(0));
    	DialogComponentStringSelection m_classifierComp = new DialogComponentStringSelection(
    			m_parameter.getMClassifierName(), "Select Classifier Name", classifierNames );
    	addDialogComponent(m_classifierComp);
    	
    	parameter.getMStrategy().setStringValue(strategyList[0]);
    	DialogComponentStringSelection m_strategyComp = new DialogComponentStringSelection(
    			m_parameter.getMStrategy(), "Select Replay Strategy", strategyList);
    	addDialogComponent(m_strategyComp);
    	
    	
    	Box cbox = new Box(BoxLayout.X_AXIS);
    	DialogComponentNumberEdit[] defaultCostComps = new DialogComponentNumberEdit[SMAlignmentReplayParameter.CFG_COST_TYPE_NUM];
    	for( int i=0; i< SMAlignmentReplayParameter.CFG_COST_TYPE_NUM ; i++) {
    		defaultCostComps[i] = new DialogComponentNumberEdit(m_parameter.getMDefaultCosts()[i], 
    				SMAlignmentReplayParameter.CFG_MCOST_KEY[i] ,5);
    		cbox.add(defaultCostComps[i].getComponentPanel());
    	}
    	m_compositePanel.add(cbox);
    	
    }
    
    protected void addDialogComponent(final DialogComponent diaC) {
		// TODO Auto-generated method stub
    	m_compositePanel.add(diaC.getComponentPanel());
	}

    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings,
			final PortObject[] input) throws NotConfigurableException {
    	// not know the situation of m_parameter.loadSettingsFrom, what can we get?? 
    	// what's the values now in settings?? how to cooperate the values from PortObjectInput and current settings?
    	
    	// 1. settings are from the saved workflow, we can get the settings there
    	// 2. settings is empty, so we don't touch it 
    	// check the values in settings
    	
    	try {
			m_parameter.loadSettingsFrom(settings);
			
		} catch (InvalidSettingsException e) {
			// TODO if there is not with TM, we set it from the input PortObject
			SMAlignmentReplayParameterWithCT tmp = (SMAlignmentReplayParameterWithCT) m_parameter;
			
			if(!tmp.isMWithTM()) {
				if (!(input[DefaultPNReplayerNodeModel.INPORT_LOG] instanceof XLogPortObject))
					throw new NotConfigurableException("Input is not a valid event log!");

				if (!(input[DefaultPNReplayerNodeModel.INPORT_PETRINET] instanceof PetriNetPortObject))
					throw new NotConfigurableException("Input is not a valid Petri net!");
				
				XLogPortObject logPO = (XLogPortObject) input[DefaultPNReplayerNodeModel.INPORT_LOG];
				PetriNetPortObject netPO = (PetriNetPortObject) input[DefaultPNReplayerNodeModel.INPORT_PETRINET];
				
				XLog log = logPO.getLog();
				// TODO: different classifier available
				XEventClassifier eventClassifier = new XEventNameClassifier();
				
				// here, no need to show the dummy event class name here
//				XEventClass evClassDummy = TesterCCWithCTNodeModel.getDummyEC();
				List<String> ecNames = XLogUtil.extractAndSortECNames(log, eventClassifier);
//				ecNames.add(evClassDummy.getId());
				tmp.setCostTM(ecNames, 0);
				
				AcceptingPetriNet anet = netPO.getANet();
				List<String> tNames = PetriNetUtil.extractTransitionNames(anet.getNet());
				tmp.setCostTM(tNames, 1);
				
				// if only names from transition side are in need, show the transitions names for dialog
				// no need to refer dummy event classes
				tmp.setCostTM(tNames, 2);	
				tmp.setMWithTM(true);
			}
		}finally {
			m_compositePanel.repaint();
		}
    	
    }
    

	@Override
	protected void saveSettingsTo(NodeSettingsWO settings) throws InvalidSettingsException {
		// TODO Auto-generated method stub
		m_parameter.saveSettingsTo(settings);
	}

	
	@Override
	protected void loadSettingsFrom(final NodeSettingsRO settings,
            final PortObjectSpec[] specs) throws NotConfigurableException {
		try {
			m_parameter.loadSettingsFrom(settings);
		} catch (InvalidSettingsException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/*
	 * create a cost Table given input: -- default cost Value for all rows -- column
	 * names -- costTable passed through the NodeModel and JTable
	 */
	private JScrollPane createTable(int idx) {
		SMAlignmentReplayParameterWithCT tmp = (SMAlignmentReplayParameterWithCT) m_parameter;
		DefaultTableModel costTM = tmp.getMCostTMs()[idx];
		
		JTable table = new JTable(costTM) {
			@Override
			public Dimension getPreferredScrollableViewportSize() {
				return new Dimension(super.getPreferredSize().width, getRowHeight() * getRowCount());
			}
		};

		table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);

		JScrollPane tPane = new JScrollPane(table);
		if (idx < 1)
			tPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
		else
			tPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);

		return tPane;
	}
}

