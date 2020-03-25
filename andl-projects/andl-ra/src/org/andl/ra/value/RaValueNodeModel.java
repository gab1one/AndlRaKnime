package org.andl.ra.value;

import java.io.File;
import java.io.IOException;

import org.andl.ra.RaEvaluator;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.container.CellFactory;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.data.container.SingleCellFactory;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.streamable.simple.SimpleStreamableFunctionNodeModel;

/**
 * <code>NodeModel</code> for the "RaValue" node.
 *
 * @author andl
 */
public class RaValueNodeModel extends SimpleStreamableFunctionNodeModel {
    
	private static final NodeLogger LOGGER = NodeLogger.getLogger(RaValueNodeModel.class);
	private static final String KEY_COLUMN_NAME = "column-name";
	private static final String KEY_TYPE_NAME = "column-type-name";
	private static final String KEY_EXPRESSION = "column-value-expression";
	private static final String DEFAULT_COLUMN_NAME = "new column";
	private static final String DEFAULT_TYPE_NAME = "STRING";
	private static final String DEFAULT_EXPRESSION = "";

	private final SettingsModelString _columnNameSettings = createSettingsColumnName();
	private final SettingsModelString _columnTypeNameSettings = createSettingsColumnTypeName();
	private final SettingsModelString _expressionSettings = createSettingsExpression();
	
	static SettingsModelString createSettingsColumnName() {
		return new SettingsModelString(KEY_COLUMN_NAME, DEFAULT_COLUMN_NAME);
	}
	
	static SettingsModelString createSettingsColumnTypeName() {
		return new SettingsModelString(KEY_TYPE_NAME, DEFAULT_TYPE_NAME);
	}
	
	static SettingsModelString createSettingsExpression() {
		return new SettingsModelString(KEY_EXPRESSION, DEFAULT_EXPRESSION);
	}
	
    //--------------------------------------------------------------------------
    // ctor and dummy overrides
    //
    // 
	//
    /**
     * Default constructor is all that is needed<br>
     * 
     * New Value uses a streamable node model and adds column(s) created by evaluating an expression
     */
    protected RaValueNodeModel() {
        super();
        LOGGER.info("Extension node created");
    }

    /** {@inheritDoc} */
    @Override
    protected void reset() { }
    
    /** {@inheritDoc} */
    @Override
    protected void loadInternals(final File internDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException { }
    
    /** {@inheritDoc} */
    @Override
    protected void saveInternals(final File internDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException { }

    //--------------------------------------------------------------------------
    // execute, configure, settings
    //
    /** {@inheritDoc} */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {

        DataTableSpec spec = inData[0].getDataTableSpec();
        ColumnRearranger rearranger = createColumnRearranger(spec);
        return new BufferedDataTable[] { 
        	exec.createColumnRearrangeTable(inData[0], rearranger, exec)
        };
    }

    /** {@inheritDoc} */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
    throws InvalidSettingsException {
    	
        ColumnRearranger rearranger = createColumnRearranger(inSpecs[0]);
        return new DataTableSpec[] { 
        	rearranger.createSpec() 
        };
    }
    
    /** {@inheritDoc} */
    @Override
    protected ColumnRearranger createColumnRearranger(final DataTableSpec inspec) throws InvalidSettingsException {
    	return createRearranger(inspec,
    			_columnNameSettings.getStringValue(),
    			_columnTypeNameSettings.getStringValue(), 
    			_expressionSettings.getStringValue());
    }

    /** {@inheritDoc} */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
		_columnNameSettings.saveSettingsTo(settings);
		_columnTypeNameSettings.saveSettingsTo(settings);
		_expressionSettings.saveSettingsTo(settings);
    }

    /** {@inheritDoc} */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
		_columnNameSettings.loadSettingsFrom(settings);
		_columnTypeNameSettings.loadSettingsFrom(settings);
		_expressionSettings.loadSettingsFrom(settings);
    }

    /** {@inheritDoc} */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
		_columnNameSettings.validateSettings(settings);
		_columnTypeNameSettings.validateSettings(settings);
		_expressionSettings.validateSettings(settings);
    }
    
    //--------------------------------------------------------------------------
    
    // create the rearranger that actually does all the work
    // TODO: multiple columns

    private ColumnRearranger createRearranger(final DataTableSpec inspec, String colname, String typename, String expression) 
    throws InvalidSettingsException {

    	TypeCellFactory tcf = TypeCellFactory.valueOf(typename);
   		try {
	        DataColumnSpec outcolspec = new DataColumnSpecCreator(colname, tcf.getDataType()).createSpec();
	        RaEvaluator jexl = new RaEvaluator(inspec, outcolspec.getType(), expression); 
	
	        ColumnRearranger rearranger = new ColumnRearranger(inspec);
	        CellFactory fac = new SingleCellFactory(outcolspec) {
	            @Override
	            public DataCell getCell(final DataRow row) {
	            	return jexl.evaluateDataCell(row);
	            }
	        };
	
	        rearranger.append(fac);
	        return rearranger;
		} catch (Exception e) {
			throw new InvalidSettingsException(
				"Not a valid expression for the type: " + e.getMessage(), e);
		}
	}
    
    
}

