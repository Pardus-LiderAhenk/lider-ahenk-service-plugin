package tr.org.liderahenk.service.dialogs;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import tr.org.liderahenk.liderconsole.core.dialogs.DefaultTaskDialog;
import tr.org.liderahenk.liderconsole.core.exceptions.ValidationException;
import tr.org.liderahenk.service.constants.ServiceConstants;
import tr.org.liderahenk.service.i18n.Messages;
/**
 * Task execution dialog for service plugin.
 * 
 */
public class ServiceTaskDialog extends DefaultTaskDialog {
	
	
	private Label lblServiceName;
	private Text txtServiceName;
	private Label lblServiceStat;
	private Combo cmbServiceStat;
	private Button chkStartAuto; 
	private static final String[] serviceStatArray = new String[] { "START", "STOP" };
	
	public ServiceTaskDialog(Shell parentShell, Set<String> dnSet) {
		super(parentShell, dnSet);
	}

	@Override
	public String createTitle() {
		return Messages.getString("SERVICE_MANAGEMENT_TITLE");
	}

	@Override
	public Control createTaskDialogArea(Composite parent) {
		
		Composite composite = new Composite(parent, SWT.NONE);
		composite.setLayout(new GridLayout(2, false));

		lblServiceName = new Label(composite, SWT.NONE);
		lblServiceName.setText(Messages.getString("SERVICE_NAME"));
		
		txtServiceName = new Text(composite, SWT.BORDER);
		txtServiceName.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

		lblServiceStat = new Label(composite, SWT.NONE);
		lblServiceStat.setText(Messages.getString("SERVICE_STAT"));

		cmbServiceStat = new Combo(composite, SWT.BORDER | SWT.DROP_DOWN | SWT.READ_ONLY);
		cmbServiceStat.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		for (int i = 0; i < serviceStatArray.length; i++) {
			String i18n = Messages.getString(serviceStatArray[i]);
			if (i18n != null && !i18n.isEmpty()) {
				cmbServiceStat.add(i18n);
				cmbServiceStat.setData(i + "", serviceStatArray[i]);
			}
		}
		
		cmbServiceStat.select(0);

		new Label(composite, SWT.NONE);

		
		chkStartAuto = new Button(composite, SWT.CHECK);
		chkStartAuto.setText(Messages.getString("START_AUTO"));

		return null;
	}
	

	@Override
	public void validateBeforeExecution() throws ValidationException {
		if(txtServiceName.getText() == null || txtServiceName.getText().isEmpty()){
			throw new ValidationException(Messages.getString("PLEASE_ENTER_SERVICE_NAME"));
		}
	}
	
	@Override
	public Map<String, Object> getParameterMap() {
		HashMap<String, Object > parameters = new HashMap<String, Object>();
		parameters.put(ServiceConstants.SERVICE_MANAGEMENT_PARAMETERS.SERVICE_NAME, txtServiceName.getText().toString());
		parameters.put(ServiceConstants.SERVICE_MANAGEMENT_PARAMETERS.SERVICE_STATUS, cmbServiceStat.getText().toString());
		parameters.put(ServiceConstants.SERVICE_MANAGEMENT_PARAMETERS.START_AUTO, chkStartAuto.getSelection());
		return parameters;
	}

	@Override
	public String getCommandId() {
		return "SERVICE_MANAGEMENT";
	}

	@Override
	public String getPluginName() {
		return ServiceConstants.PLUGIN_NAME;
	}

	@Override
	public String getPluginVersion() {
		return ServiceConstants.PLUGIN_VERSION;
	}
	
}
