package tr.org.liderahenk.service.model;

import java.io.Serializable;

public class ServiceListItem implements Serializable {

	private static final long serialVersionUID = -6960172599451368434L;
	private String serviceName;
	private DesiredStatus desiredServiceStatus;
	private DesiredStatus desiredStartAuto;
	private String serviceStatus;
	private String startAuto;

	public String getServiceName() {
		return serviceName;
	}

	public void setServiceName(String serviceName) {
		this.serviceName = serviceName;
	}

	public DesiredStatus getDesiredServiceStatus() {
		return desiredServiceStatus;
	}

	public void setDesiredServiceStatus(DesiredStatus serviceStatus) {
		this.desiredServiceStatus = serviceStatus;
	}

	public DesiredStatus getDesiredStartAuto() {
		return desiredStartAuto;
	}

	public void setDesiredStartAuto(DesiredStatus startAuto) {
		this.desiredStartAuto = startAuto;
	}

	public String getStartAuto() {
		return startAuto;
	}

	public void setStartAuto(String startAuto) {
		this.startAuto = startAuto;
	}

	public String getServiceStatus() {
		return serviceStatus;
	}

	public void setServiceStatus(String serviceStatus) {
		this.serviceStatus = serviceStatus;
	}

	@Override
	public String toString() {
		return "ServiceListItem [serviceName=" + serviceName + ", serviceStatus=" + serviceStatus + ", startAuto="
				+ startAuto + ", desiredServiceStatus=" + desiredServiceStatus + ", desiredStartAuto="
				+ desiredStartAuto + "]";
	}
}
