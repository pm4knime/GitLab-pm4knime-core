package org.pm4knime.node.io.processtree.writer;

import java.net.URL;
import java.nio.file.Path;

import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;
import org.knime.core.node.util.CheckUtils;
import org.knime.core.util.FileUtil;
import org.pm4knime.portobject.ProcessTreePortObject;
import org.pm4knime.portobject.ProcessTreePortObjectSpec;
import org.pm4knime.util.defaultnode.DefaultNodeModel;

/**
 * This is the model implementation of ProcessTreeWriter.
 * Export process tree into file
 *
 * @author DKF
 */
public class ProcessTreeWriterNodeModel extends DefaultNodeModel {
    
	private static final NodeLogger logger = NodeLogger
            .getLogger(ProcessTreeWriterNodeModel.class);
        
    private final SettingsModelString m_fileName = ProcessTreeWriterNodeDialog.createFileNameModel();
    
	
    protected ProcessTreeWriterNodeModel() {
    
        // TODO: Specify the amount of input and output ports needed.
        super(new PortType[] {ProcessTreePortObject.TYPE}, new PortType[] {});
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected PortObject[] execute(final PortObject[] inData,
            final ExecutionContext exec) throws Exception {

        // TODO: save the current process tree object into the file
    	logger.info("Begin to write Process Tree into ptml file");
        
    	URL url = FileUtil.toURL(m_fileName.getStringValue());
        Path localPath = FileUtil.resolveToPath(url);
        checkCanceled(exec);
        ProcessTreePortObject m_ptPort = (ProcessTreePortObject) inData[0];
        
        if(m_ptPort.getTree() != null) {
        	m_ptPort.save(localPath.toString());
        }
        
        checkCanceled(exec);
        logger.info("End to write Process Tree into ptml file");
        
        return new PortObject[]{};
    }

    

    /**
     * {@inheritDoc}
     */
    @Override
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs)
            throws InvalidSettingsException {
        // TODO: configure the output file here
    	String warning = CheckUtils.checkDestinationFile(m_fileName.getStringValue(), true);
    	
        if(warning != null) {
        	setWarningMessage(warning);
        }
        if(inSpecs[0].getClass().equals(ProcessTreePortObjectSpec.class))
        	return new PortObjectSpec[] {};
        else
        	throw new InvalidSettingsException("Not a Petri net to export");
    	
        
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
         // TODO: generated method stub
    	m_fileName.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        // TODO: generated method stub
    	m_fileName.loadSettingsFrom(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        // TODO: generated method stub
    	m_fileName.validateSettings(settings);
    }
    
}

