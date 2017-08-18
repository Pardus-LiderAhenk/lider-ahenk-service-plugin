package tr.org.liderahenk.service.dialogs;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tr.org.liderahenk.liderconsole.core.dialogs.DefaultTaskDialog;
import tr.org.liderahenk.liderconsole.core.exceptions.ValidationException;
import tr.org.liderahenk.liderconsole.core.ldap.enums.DNType;
import tr.org.liderahenk.liderconsole.core.rest.requests.TaskRequest;
import tr.org.liderahenk.liderconsole.core.rest.responses.IResponse;
import tr.org.liderahenk.liderconsole.core.rest.utils.TaskRestUtils;
import tr.org.liderahenk.liderconsole.core.utils.SWTResourceManager;
import tr.org.liderahenk.liderconsole.core.widgets.Notifier;
import tr.org.liderahenk.liderconsole.core.xmpp.notifications.TaskStatusNotification;
import tr.org.liderahenk.service.constants.ServiceConstants;
import tr.org.liderahenk.service.editingsupport.StatusEditingSupport;
import tr.org.liderahenk.service.i18n.Messages;
import tr.org.liderahenk.service.model.DesiredStatus;
import tr.org.liderahenk.service.model.ServiceListItem;

/**
 * Task execution dialog for service plugin.
 * 
 */
public class ServiceTaskDialog extends DefaultTaskDialog {
	
	private static final Logger logger = LoggerFactory.getLogger(ServiceTaskDialog.class);

	private Label lblServiceName;
	private Text txtServiceName;
	private Label lblServiceStat;
	private Combo cmbServiceStat;
	private Button chkStartAuto;
	private static final String[] serviceStatArray = new String[] { "NA", "Start", "Stop" };
	private Composite compositeServiceList;
	//private TableViewer tableViewerServiceMonitor;
	private TableViewer tableViewerServiceManage;

	private final Image activeImage;
	private final Image inactiveImage;
	private Table table;
	private Button btnAddService;

	private List<ServiceListItem> serviceList;
	private List<ServiceListItem> deletedServiceList;
	private TabFolder tabFolder;
	private TabItem tbtmServiceManage;
	private Composite compositeServiceListManage;
	private Button btnDeleteServiceManage;
	private Button btnManageService;
	
	private List<String> dnList;

	public ServiceTaskDialog(Shell parentShell, Set<String> dnSet) {
		super(parentShell, dnSet,false, true, true);
		activeImage = new Image(Display.getDefault(),
				this.getClass().getClassLoader().getResourceAsStream("icons/16/active.png"));
		inactiveImage = new Image(Display.getDefault(),
				this.getClass().getClassLoader().getResourceAsStream("icons/16/inactive.png"));

		serviceList = new ArrayList<>();
		deletedServiceList= new ArrayList<>();
		
		subscribeEventHandler(taskStatusNotificationHandler);
		
		dnList=new ArrayList<String>(dnSet);

	}

	@Override
	public String createTitle() {
		return Messages.getString("SERVICE_MANAGEMENT_TITLE");
	}
	
	@Override
	protected Point getInitialSize() {
		// TODO Auto-generated method stub
		return new Point(800, 910);
	}

	private void getServices() {
		try {
			
			TaskRequest task = new TaskRequest(dnList, DNType.AHENK, getPluginName(), getPluginVersion(), "GET_SERVICES_FROM_DB",
					null, null, null, new Date());
			IResponse response = TaskRestUtils.execute(task);
			Map<String, Object> resultMap = response.getResultMap();
			ObjectMapper mapper = new ObjectMapper();

			List<ServiceListItem> services = mapper.readValue(mapper.writeValueAsString(resultMap.get("serviceList")),
					new TypeReference<List<ServiceListItem>>() {
					});

			if (services != null) {
				serviceList = services;
				tableViewerServiceManage.setInput(serviceList);
				tableViewerServiceManage.refresh();

//				tableViewerServiceMonitor.setInput(serviceList);
//				tableViewerServiceMonitor.refresh();
			}

		} catch (Exception e) {
			Notifier.error(null, Messages.getString("ERROR_ON_LIST"));
			e.printStackTrace();
		}
	}

	@Override
	public Control createTaskDialogArea(Composite parent) {

		Composite composite = new Composite(parent, SWT.NONE);
		// gd.widthHint = 900;
		composite.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, true, 1, 1));
		composite.setLayout(new GridLayout(4, false));
		new Label(composite, SWT.NONE);
		new Label(composite, SWT.NONE);
		new Label(composite, SWT.NONE);
		new Label(composite, SWT.NONE);

		lblServiceName = new Label(composite, SWT.NONE);
		lblServiceName.setText(Messages.getString("SERVICE_NAME"));

		txtServiceName = new Text(composite, SWT.BORDER);
		GridData gd_txtServiceName = new GridData(SWT.FILL, SWT.FILL, false, false);
		gd_txtServiceName.widthHint = 139;
		txtServiceName.setLayoutData(gd_txtServiceName);
		txtServiceName.setToolTipText("Örn: ssh");

		btnAddService = new Button(composite, SWT.NONE);
		btnAddService.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				String serviceName = txtServiceName.getText().toString();

				ServiceListItem item = new ServiceListItem();
				item.setServiceName(serviceName);
				item.setDesiredServiceStatus(DesiredStatus.NA);
				boolean isExist = false;
				for (ServiceListItem serviceListItem : serviceList) {
					if (serviceListItem.getServiceName().equals(serviceName))
						isExist = true;
				}

				if (!isExist)
					serviceList.add(item);

				if (serviceList != null) {
					tableViewerServiceManage.setInput(serviceList);
					tableViewerServiceManage.refresh();
				}

			}
		});
		btnAddService.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, false, false, 1, 1));
		btnAddService.setText(Messages.getString("ADD_SERVICE_BTN"));
		new Label(composite, SWT.NONE);

		compositeServiceList = new Composite(composite, SWT.NONE);
		compositeServiceList.setLayout(new GridLayout(1, false));
		compositeServiceList.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 4, 1));

		tabFolder = new TabFolder(compositeServiceList, SWT.NONE);
		tabFolder.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));
//		tbtmServiceMonitor.setControl(compositeServiceListMonitor);

		tbtmServiceManage = new TabItem(tabFolder, SWT.NONE);
		tbtmServiceManage.setText(Messages.getString("DESIRED_SERVICE_STATE")); //$NON-NLS-1$

		compositeServiceListManage = new Composite(tabFolder, SWT.NONE);
		tbtmServiceManage.setControl(compositeServiceListManage);

//		compositeServiceListMonitor.setLayout(new GridLayout(1, false));

//		btnDeleteServiceMonitor = new Button(compositeServiceListMonitor, SWT.NONE);
//		btnDeleteServiceMonitor.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, true, false, 1, 1));
//		btnDeleteServiceMonitor.addSelectionListener(new SelectionAdapter() {
//			@Override
//			public void widgetSelected(SelectionEvent e) {
//
//				TableItem[] selection = tableViewerServiceMonitor.getTable().getSelection();
//				if (selection.length > 0) {
//
//					ServiceListItem item = (ServiceListItem) selection[0].getData();
//					item.setDeleted(true);
//
//					if (serviceList != null) {
//						serviceList.remove(item);
//						tableViewerServiceMonitor.setInput(serviceList);
//						tableViewerServiceMonitor.refresh();
//					}
//				}
//
//			}
//		});
//		btnDeleteServiceMonitor.setText(Messages.getString("DELETE_SERVICE_BTN"));
//
//		createServiceMonitorArea(compositeServiceListMonitor);

		compositeServiceListManage.setLayout(new GridLayout(1, false));
		compositeServiceListManage.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, true));

		btnDeleteServiceManage = new Button(compositeServiceListManage, SWT.NONE);
		btnDeleteServiceManage.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, true, false, 1, 1));
		btnDeleteServiceManage.setText(Messages.getString("DELETE_SERVICE_BTN")); 
		
		btnDeleteServiceManage.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {

				TableItem[] selection = tableViewerServiceManage.getTable().getSelection();
				if (selection.length > 0) {

					ServiceListItem item = (ServiceListItem) selection[0].getData();
					item.setDeleted(true);
					deletedServiceList.add(item);

					if (serviceList != null) {
						serviceList.remove(item);
						tableViewerServiceManage.setInput(serviceList);
						tableViewerServiceManage.refresh();
					}
				}

			}
		});
		createServiceManageArea(compositeServiceListManage);
		
		btnManageService = new Button(compositeServiceListManage, SWT.NONE);
		btnManageService.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, true, false, 1, 1));
		btnManageService.setText(Messages.getString("MANAGE_SERVICES_BTN")); 
		
		btnManageService.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				
				 Map<String, Object> parameters = getServiceParams();
				
				TaskRequest task = new TaskRequest(new ArrayList<String>(getDnSet()), DNType.AHENK, "service",
						"1.0.0", "SERVICE_LIST", parameters, null, null, new Date());
				try {
					TaskRestUtils.execute(task);
				} catch (Exception e1) {
					e1.printStackTrace();
				}
				
			}
		});

		getServices();

		return parent;
	}
	
	
	public Map<String, Object> getServiceParams() {
		java.util.HashMap<String, Object> parameters = new HashMap<String, Object>();
		List<ServiceListItem> list = new ArrayList<>();
		TableItem[] items = tableViewerServiceManage.getTable().getItems();
		for (TableItem tableItem : items) {
			
			ServiceListItem listItem= (ServiceListItem) tableItem.getData();
			
			if(!(listItem.getDesiredServiceStatus()==DesiredStatus.NA) && !listItem.getServiceStatus().equals("NOTFOUND")){
					ServiceListItem item = new ServiceListItem();
					item.setServiceName(listItem.getServiceName());
					item.setServiceStatus(listItem.getDesiredServiceStatus().toString());
					list.add(item);
			}
		}
		parameters.put(ServiceConstants.SERVICE_REQUESTS_PARAMETERS, list);
		return parameters;
	}

	private void createServiceManageArea(Composite parent) {

		tableViewerServiceManage = SWTResourceManager.createTableViewer(parent);
		createServiceManageTableColumns();

		// Hook up listeners
		tableViewerServiceManage.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				// IStructuredSelection selection = (IStructuredSelection)
				// tableViewer.getSelection();
				// Object firstElement = selection.getFirstElement();
				// if (firstElement instanceof ServiceListItem) {
				// setSelectedService((ServiceListItem) firstElement);
				// }
			}
		});
		tableViewerServiceManage.refresh();

	}

//	private void createServiceMonitorArea(Composite parent) {
//		// createTableFilterArea(parent);
//
//		tableViewerServiceMonitor = SWTResourceManager.createTableViewer(parent);
//		createServiceMonitorTableColumns();
//
//		// Hook up listeners
//		tableViewerServiceMonitor.addSelectionChangedListener(new ISelectionChangedListener() {
//			@Override
//			public void selectionChanged(SelectionChangedEvent event) {
//				// IStructuredSelection selection = (IStructuredSelection)
//				// tableViewer.getSelection();
//				// Object firstElement = selection.getFirstElement();
//				// if (firstElement instanceof ServiceListItem) {
//				// setSelectedService((ServiceListItem) firstElement);
//				// }
//			}
//		});
//		tableViewerServiceMonitor.refresh();
//
//	}

//	private void createServiceMonitorTableColumns() {
//		// Package name
//		TableViewerColumn serviceNameColumn = SWTResourceManager.createTableViewerColumn(tableViewerServiceMonitor,
//				Messages.getString("SERVICE_NAME"), 200);
//		serviceNameColumn.setLabelProvider(new ColumnLabelProvider() {
//			@Override
//			public String getText(Object element) {
//				if (element instanceof ServiceListItem) {
//					return ((ServiceListItem) element).getServiceName();
//				}
//				return Messages.getString("UNTITLED");
//			}
//		});
//
//		// // Desired status
//		// TableViewerColumn desiredStatusColumn =
//		// SWTResourceManager.createTableViewerColumn(tableViewerServiceMonitor,
//		// Messages.getString("SERVICE_STAT"), 250);
//		// desiredStatusColumn.setLabelProvider(new ColumnLabelProvider() {
//		// @Override
//		// public String getText(Object element) {
//		// if (element instanceof ServiceListItem) {
//		// return ((ServiceListItem)
//		// element).getDesiredServiceStatus().getMessage();
//		// }
//		// return Messages.getString("UNTITLED");
//		// }
//		// });
//		// desiredStatusColumn.setEditingSupport(new
//		// StatusEditingSupport(tableViewerServiceMonitor));
//
//		// // Desired status
//		// TableViewerColumn desiredStartAutoColumn =
//		// SWTResourceManager.createTableViewerColumn(tableViewer,
//		// Messages.getString("START_AT_BEGINNING"), 250);
//		// desiredStartAutoColumn.setLabelProvider(new ColumnLabelProvider() {
//		// @Override
//		// public String getText(Object element) {
//		// if (element instanceof ServiceListItem) {
//		// return ((ServiceListItem)
//		// element).getDesiredStartAuto().getMessage();
//		// }
//		// return Messages.getString("UNTITLED");
//		// }
//		// });
//		// desiredStartAutoColumn.setEditingSupport(new
//		// StartAutoEditingSupport(tableViewer));
//
//	}

	private void createServiceManageTableColumns() {
		// Service name
		TableViewerColumn serviceNameColumn = SWTResourceManager.createTableViewerColumn(tableViewerServiceManage,
				Messages.getString("SERVICE_NAME"), 200);
		serviceNameColumn.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				if (element instanceof ServiceListItem) {
					return ((ServiceListItem) element).getServiceName();
				}
				return Messages.getString("UNTITLED");
			}
		});
		TableViewerColumn serviceMonitorStatusColumn = SWTResourceManager.createTableViewerColumn(tableViewerServiceManage,
				Messages.getString("SERVICE_MONITOR"), 180);
		serviceMonitorStatusColumn.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				if (element instanceof ServiceListItem) {
					
					
					return ((ServiceListItem) element).isServiceMonitoring() ? Messages.getString("IS_MONTORING") : Messages.getString("IS_NOT_MONITORING");
				}
				return Messages.getString("UNTITLED");
			}
		});
		
		TableViewerColumn serviceStatusColumn = SWTResourceManager.createTableViewerColumn(tableViewerServiceManage,
				Messages.getString("SERVICE_STAT"), 180);

		serviceStatusColumn.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				if (element instanceof ServiceListItem) {
					return ((ServiceListItem) element).getServiceStatus();
				}
				return Messages.getString("UNTITLED");
			}
		});

		// Desired status
		TableViewerColumn desiredStatusColumn = SWTResourceManager.createTableViewerColumn(tableViewerServiceManage,
				Messages.getString("SERVICE_MANAGE"), 180);
		desiredStatusColumn.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				if (element instanceof ServiceListItem) {

					if (((ServiceListItem) element).getDesiredServiceStatus() == null)
						((ServiceListItem) element).setDesiredServiceStatus(DesiredStatus.NA);

					return ((ServiceListItem) element).getDesiredServiceStatus().getMessage();
				}
				return Messages.getString("UNTITLED");
			}
		});
		desiredStatusColumn.setEditingSupport(new StatusEditingSupport(tableViewerServiceManage));


	}

	@Override
	public void validateBeforeExecution() throws ValidationException {
		if (tableViewerServiceManage.getTable().getItems().length ==0) {
			throw new ValidationException(Messages.getString("PLEASE_ENTER_SERVICE_NAME"));
		}
	}

	@Override
	public Map<String, Object> getParameterMap() {

		java.util.HashMap<String, Object> parameters = new HashMap<String, Object>();
		List<ServiceListItem> list = new ArrayList<>();
		TableItem[] items = tableViewerServiceManage.getTable().getItems();
		for (TableItem tableItem : items) {
			ServiceListItem item = (ServiceListItem) tableItem.getData();
			list.add(item);
		}
		
		if(deletedServiceList!=null)
			list.addAll(deletedServiceList);
		parameters.put(ServiceConstants.SERVICE_REQUESTS_PARAMETERS, list);
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
	
	@Override
	public String getMailSubject() {
		
		return "Servis Alarm";
	}
	
	private EventHandler taskStatusNotificationHandler = new EventHandler() {
		@Override
		public void handleEvent(final Event event) {
			Job job = new Job("TASK") {
				@Override
				protected IStatus run(IProgressMonitor monitor) {
					monitor.beginTask("SERVICE_MANAGEMENT", 100);
					try {
						TaskStatusNotification taskStatus = (TaskStatusNotification) event
								.getProperty("org.eclipse.e4.data");
						byte[] data = taskStatus.getResult().getResponseData();
						final Map<String, Object> responseData = new ObjectMapper().readValue(data, 0, data.length,
								new TypeReference<HashMap<String, Object>>() {
								});  
						Display.getDefault().asyncExec(new Runnable() {

							@Override
							public void run() {
								
								getServices();
								
							}
						});
					} catch (Exception e) {
						logger.error(e.getMessage(), e);
						Notifier.error("", Messages.getString("UNEXPECTED_ERROR_ACCESSING_RESOURCE_USAGE"));
					}
					monitor.worked(100);
					monitor.done();

					return Status.OK_STATUS;
				}
			};

			job.setUser(true);
			job.schedule();
		}
	};

	@Override
	public String getMailContent() {
		
		return "cn={ahenk} tanımlamış olduğunuz aşağıdaki servisler durmuştur. \n {stopped_services} ";
	}
}
