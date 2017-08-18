package tr.org.liderahenk.service.commands;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.codehaus.jackson.map.DeserializationConfig;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;

import tr.org.liderahenk.lider.core.api.persistence.IPluginDbService;
import tr.org.liderahenk.lider.core.api.persistence.entities.ICommandExecutionResult;
import tr.org.liderahenk.lider.core.api.plugin.ICommand;
import tr.org.liderahenk.lider.core.api.plugin.IPluginInfo;
import tr.org.liderahenk.lider.core.api.plugin.ITaskAwareCommand;
import tr.org.liderahenk.lider.core.api.rest.requests.ITaskRequest;
import tr.org.liderahenk.lider.core.api.service.ICommandContext;
import tr.org.liderahenk.lider.core.api.service.ICommandResult;
import tr.org.liderahenk.lider.core.api.service.ICommandResultFactory;
import tr.org.liderahenk.lider.core.api.service.enums.CommandResultStatus;
import tr.org.liderahenk.service.entities.ServiceListItem;

public class ServiceCommand implements ICommand, ITaskAwareCommand {

	private ICommandResultFactory resultFactory;
	private IPluginInfo pluginInfo;

	private IPluginDbService pluginDbService;

	@Override
	public ICommandResult execute(ICommandContext context) {
		
		 ITaskRequest req = context.getRequest();
	
		 Map<String, Object> parameterMap = req.getParameterMap();
		 
		 ObjectMapper mapper = new ObjectMapper();
		 mapper.configure(
				    DeserializationConfig.Feature.FAIL_ON_UNKNOWN_PROPERTIES, false);

		 try {
			List<ServiceListItem> serviceList = mapper.readValue(mapper.writeValueAsString(parameterMap.get("serviceRequestParameters")),
						new TypeReference<List<ServiceListItem>>() {
				});
			
			if(serviceList!=null && serviceList.size()>0){
				for (ServiceListItem serviceListItem : serviceList) {
					
					if(serviceListItem.getId()==null){
						serviceListItem.setCreateDate(new Date());
						serviceListItem.setOwner(req.getOwner());
						serviceListItem.setAgentDn(context.getRequest().getDnList().get(0));
						pluginDbService.save(serviceListItem);
						
					}
					if(serviceListItem.isUpdated() || serviceListItem.isDeleted()){
						serviceListItem.setModifyDate(new Date());
						pluginDbService.update(serviceListItem);
					}
				}
			}
			
		} catch (Exception e) {
			
			e.printStackTrace();
			return resultFactory.create(CommandResultStatus.ERROR, new ArrayList<String>(), this);
		} 
		return resultFactory.create(CommandResultStatus.OK, new ArrayList<String>(), this);
	}

	@Override
	public ICommandResult validate(ICommandContext context) {
		return resultFactory.create(CommandResultStatus.OK, null, this, null);
	}

	@Override
	public String getCommandId() {
		return "SERVICE_MANAGEMENT";
	}

	@Override
	public Boolean executeOnAgent() {
		return true;
	}
	
	@Override
	public String getPluginName() {
		return pluginInfo.getPluginName();
	}

	@Override
	public String getPluginVersion() {
		return pluginInfo.getPluginVersion();
	}

	public void setResultFactory(ICommandResultFactory resultFactory) {
		this.resultFactory = resultFactory;
	}
	
	public void setPluginInfo(IPluginInfo pluginInfo) {
		this.pluginInfo = pluginInfo;
	}

	public IPluginDbService getPluginDbService() {
		return pluginDbService;
	}

	public void setPluginDbService(IPluginDbService pluginDbService) {
		this.pluginDbService = pluginDbService;
	}

	@Override
	public void onTaskUpdate(ICommandExecutionResult result) {

		try {

			byte[] data = result.getResponseData();
			
			final Map<String, Object> responseData = new ObjectMapper().readValue(data, 0, data.length,
					new TypeReference<HashMap<String, Object>>() {
					});

			ObjectMapper mapper = new ObjectMapper();
			
			List<ServiceListItem> services = new ObjectMapper().readValue(mapper.writeValueAsString(responseData.get("services")),
						new TypeReference<List<ServiceListItem>>() {
				});
			
			if(services!=null)
				for (ServiceListItem serviceListItem : services) {
					
					List<ServiceListItem> serviceList= pluginDbService.findByProperty(ServiceListItem.class, "serviceName", serviceListItem.getServiceName(), 1);
					if(serviceList!=null && serviceList.size()>0){
						ServiceListItem service= serviceList.get(0);
						service.setAgentId(result.getAgentId());
						service.setDeleted(serviceListItem.isDeleted());
						service.setDesiredServiceStatus(serviceListItem.getDesiredServiceStatus());
						service.setDesiredStartAuto(serviceListItem.getDesiredStartAuto());
						service.setStartAuto(serviceListItem.getStartAuto());
						service.setServiceStatus(serviceListItem.getServiceStatus());
						
						service.setModifyDate(new Date());
						service.setServiceMonitoring(true);
						pluginDbService.update(service);
					}
				}

		} catch (Exception e) {
			e.printStackTrace();
		}

	
		
	}
	
}
