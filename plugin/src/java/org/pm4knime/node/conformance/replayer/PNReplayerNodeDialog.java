package org.pm4knime.node.conformance.replayer;

import java.awt.Dimension;
import java.util.ArrayList;
import java.util.Arrays;
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
import org.pm4knime.portobject.PetriNetPortObject;
import org.pm4knime.portobject.XLogPortObject;
import org.pm4knime.portobject.XLogPortObjectSpec;
import org.pm4knime.settingsmodel.SMAlignmentReplayParameter;
import org.pm4knime.settingsmodel.SMAlignmentReplayParameterWithCT;
import org.pm4knime.util.PetriNetUtil;
import org.pm4knime.util.ReplayerUtil;
import org.pm4knime.util.XLogSpecUtil;
import org.pm4knime.util.XLogUtil;
import org.processmining.acceptingpetrinet.models.AcceptingPetriNet;

/**
 * <code>NodeDialog</code> for the "PNReplayer" node. This dialog incldues the
 * cost tables for each event class and transition.
 * 
 * @author Kefang Ding
 */
public class PNReplayerNodeDialog extends DataAwareNodeDialogPane {

	protected JPanel m_compositePanel;
	protected SMAlignmentReplayParameterWithCT m_parameter;
	String[] strategyList = ReplayerUtil.strategyList;

	DialogComponentStringSelection classifierComp;

	XLogPortObject logPO;

	/**
	 * New pane for configuring the PNReplayer node.
	 */
	protected PNReplayerNodeDialog() {
		m_compositePanel = new JPanel();
		m_compositePanel.setLayout(new BoxLayout(m_compositePanel, BoxLayout.Y_AXIS));

		specialInit();

		super.addTab("Options", m_compositePanel);
	}

	protected void specialInit() {
		// TODO Auto-generated method stub
		m_parameter = new SMAlignmentReplayParameterWithCT(PNReplayerNodeModel.CFG_PARAMETER_NAME);

		// TODO : remove tmp. just use the old parameter here
		SMAlignmentReplayParameterWithCT tmp = m_parameter;
		commonInitPanel(m_parameter);

		// here to add special codes from the additional items
		Box tbox = new Box(BoxLayout.Y_AXIS);
		for (int i = 0; i < SMAlignmentReplayParameterWithCT.CFG_COST_TYPE_NUM; i++) {
			tbox.add(createTable(i));
		}

		m_compositePanel.add(tbox);

		// add action listener. If the value of defaultCost changes, the table values
		// should changes, too.
		// to deal with log move at first.
		for (int i = 0; i < SMAlignmentReplayParameter.CFG_COST_TYPE_NUM; i++) {
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
					for (int rIdx = 0; rIdx < tTable.getRowCount(); rIdx++) {
						if (idx == 1) {
							// in Petri net table
							String tName = (String) tTable.getValueAt(rIdx, 0);
							if (PetriNetUtil.isTauName(tName)) {
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
		// TODO : complete the codes with classifer names

		classifierComp = new DialogComponentStringSelection(m_parameter.getMClassifierName(),
				"Select Classifier Name", new String[] { "" });
		addDialogComponent(classifierComp);

		parameter.getMStrategy().setStringValue(strategyList[0]);
		DialogComponentStringSelection m_strategyComp = new DialogComponentStringSelection(m_parameter.getMStrategy(),
				"Select Replay Strategy", strategyList);
		addDialogComponent(m_strategyComp);

		Box cbox = new Box(BoxLayout.X_AXIS);
		DialogComponentNumberEdit[] defaultCostComps = new DialogComponentNumberEdit[SMAlignmentReplayParameter.CFG_COST_TYPE_NUM];
		for (int i = 0; i < SMAlignmentReplayParameter.CFG_COST_TYPE_NUM; i++) {
			defaultCostComps[i] = new DialogComponentNumberEdit(m_parameter.getMDefaultCosts()[i],
					SMAlignmentReplayParameter.CFG_MCOST_KEY[i], 5);
			cbox.add(defaultCostComps[i].getComponentPanel());
		}
		m_compositePanel.add(cbox);

	}

	protected void addDialogComponent(final DialogComponent diaC) {
		// TODO Auto-generated method stub
		m_compositePanel.add(diaC.getComponentPanel());
	}

	@Override
	protected void loadSettingsFrom(final NodeSettingsRO settings, final PortObject[] input)
			throws NotConfigurableException {
		
		try {
			// before we need to consider the selected values here, or we check if the spec is the same one
			// with comparison to the input. If not, we will change it. But it requires that we need to
			// know the spec information. WHich is not right!!
 			m_parameter.loadSettingsFrom(settings);

//			String selectedItem = m_parameter.getMClassifierName().getStringValue();

			if (!(input[DefaultPNReplayerNodeModel.INPORT_LOG] instanceof XLogPortObject))
				throw new NotConfigurableException("Input is not a valid event log!");

			if (!(input[DefaultPNReplayerNodeModel.INPORT_PETRINET] instanceof PetriNetPortObject))
				throw new NotConfigurableException("Input is not a valid Petri net!");

			logPO = (XLogPortObject) input[DefaultPNReplayerNodeModel.INPORT_LOG];
			PetriNetPortObject netPO = (PetriNetPortObject) input[DefaultPNReplayerNodeModel.INPORT_PETRINET];

			XLogPortObjectSpec logSpec = (XLogPortObjectSpec) logPO.getSpec();
			
			List<String> specClassifierSet = new ArrayList<String>(
					XLogSpecUtil.getClassifierWithClsList(logSpec.getClassifiersMap()));
			
			List<String> configClassifierSet = Arrays.asList(m_parameter.getClassifierSet().getStringArrayValue());

			if (!configClassifierSet.containsAll(specClassifierSet)
					|| !specClassifierSet.containsAll(configClassifierSet)) {
				m_parameter.getClassifierSet().setStringArrayValue(specClassifierSet.toArray(new String[0]));
				
			}
			// to avoid the split names there, but we save the classifier with 
			classifierComp.replaceListItems(logSpec.getClassifiersMap().keySet(), 
					logSpec.getClassifiersMap().keySet().iterator().next());
			m_parameter.getMClassifierName().loadSettingsFrom(settings);
			// if the classifier is the same, then we don't need to check the event log. because they are the same
			SMAlignmentReplayParameterWithCT tmp = (SMAlignmentReplayParameterWithCT) m_parameter;
			
			XLog log = logPO.getLog();
			XEventClassifier eventClassifier = XLogUtil.getEventClassifier(log,
					m_parameter.getMClassifierName().getStringValue());
			List<String> ecNames = XLogUtil.extractAndSortECNames(log, eventClassifier);
			List<String> ecModelNames = getColumnStrValueTM(m_parameter.getMCostTMs()[0], 0);
			if(!ecNames.containsAll(ecModelNames) || !ecModelNames.containsAll(ecNames)) {
				tmp.setCostTM(ecNames, 0);
			}
			
			// how to exact the names of the net and compare it to the current values
			AcceptingPetriNet anet = netPO.getANet();
			List<String> tNames = PetriNetUtil.extractTransitionNames(anet.getNet());
			List<String> tModelNames = getColumnStrValueTM(m_parameter.getMCostTMs()[1], 0);
			if(!tNames.containsAll(tModelNames) || !tModelNames.containsAll(tNames)) {
				// they are not the same, update the values in tModelNames there
				tmp.setCostTM(tNames, 1);
				tmp.setCostTM(tNames, 2);
			}
			
			// when it works ?? 
			m_parameter.getMClassifierName().addChangeListener(new ChangeListener() {
				// to update the classifier according to the chosen transitions expression there
				@Override
				public void stateChanged(ChangeEvent e) {
					// TODO it implements the update if we change the event classifier in choice
					SMAlignmentReplayParameterWithCT tmp = (SMAlignmentReplayParameterWithCT) m_parameter;

					if (logPO != null) {
						XLog log = logPO.getLog();
						// here the value is still the old value,
//						System.out.println("the current value is " + m_parameter.getMClassifierName().getStringValue());
						XEventClassifier eventClassifier = XLogUtil.getEventClassifier(log,
								m_parameter.getMClassifierName().getStringValue());

						List<String> ecNames = XLogUtil.extractAndSortECNames(log, eventClassifier);
						tmp.setCostTM(ecNames, 0);
						// need some update to the view? 
						
					}
				}

			});

		} catch (InvalidSettingsException |

				NullPointerException e) {
			// TODO if there is not with TM, we set it from the input PortObject
			System.out.println("Some wrong is here");
			e.printStackTrace();
			throw new NotConfigurableException("Please make sure the connected event log in excution state");

		}

	}

	@Override
	protected void saveSettingsTo(NodeSettingsWO settings) throws InvalidSettingsException {
		// TODO Auto-generated method stub
		m_parameter.saveSettingsTo(settings);
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
		tPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
		return tPane;
	}
	
	private List<String> getColumnStrValueTM(DefaultTableModel costTM, int colIdx){
		List<String> colValueList = new ArrayList();
		for(int i =0; i<costTM.getRowCount(); i++) {
			colValueList.add(costTM.getValueAt(i, colIdx).toString());
		}
		return colValueList;
	}

}
