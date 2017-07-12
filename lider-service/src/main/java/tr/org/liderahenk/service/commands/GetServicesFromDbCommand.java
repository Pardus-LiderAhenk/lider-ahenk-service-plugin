package tr.org.liderahenk.service.commands;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import tr.org.liderahenk.lider.core.api.persistence.IPluginDbService;
import tr.org.liderahenk.lider.core.api.plugin.ICommand;
import tr.org.liderahenk.lider.core.api.plugin.IPluginInfo;
import tr.org.liderahenk.lider.core.api.rest.requests.ITaskRequest;
import tr.org.liderahenk.lider.core.api.service.ICommandContext;
import tr.org.liderahenk.lider.core.api.service.ICommandResult;
import tr.org.liderahenk.lider.core.api.service.ICommandResultFactory;
import tr.org.liderahenk.lider.core.api.service.enums.CommandResultStatus;
import tr.org.liderahenk.service.entities.ServiceListItem;

public class GetServicesFromDbCommand implements ICommand {

	private ICommandResultFactory resultFactory;
	private IPluginInfo pluginInfo;

	private IPluginDbService pluginDbService;

	@Override
	public ICommandResult execute(ICommandContext context) {
		
		List<String> resultMessages;
		Map<String, Object> resultMap;
		try {
			ITaskRequest req = context.getRequest();
			
			
			Map<String, Object> properties= new HashMap<String, Object>();
			properties.put("owner", req.getOwner());
			properties.put("deleted", false);
			
			List<ServiceListItem> services=pluginDbService.findByProperties(ServiceListItem.class, properties, null, null);
			
			resultMessages = new ArrayList<String>();
			resultMessages.add("İşlem Başarılı");
			
			resultMap = new HashMap<String, Object>();
			
			resultMap.put("serviceList", services);
		} catch (Exception e) {
			e.printStackTrace();
			return resultFactory.create(CommandResultStatus.ERROR, new ArrayList<String>(), this);
			
		}
		
		return resultFactory.create(CommandResultStatus.OK, resultMessages, this, resultMap);
	}

	@Override
	public ICommandResult validate(ICommandContext context) {
		return resultFactory.create(CommandResultStatus.OK, null, this, null);
	}

	@Override
	public String getCommandId() {
		return "GET_SERVICES_FROM_DB";
	}

	@Override
	public Boolean executeOnAgent() {
		return false;
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
	
}
