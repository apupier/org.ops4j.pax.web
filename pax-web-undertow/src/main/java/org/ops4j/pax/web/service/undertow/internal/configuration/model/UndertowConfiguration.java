/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ops4j.pax.web.service.undertow.internal.configuration.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;

import static org.ops4j.pax.web.service.undertow.internal.configuration.model.ObjectFactory.NS_UNDERTOW;

@XmlRootElement(name = "undertow")
@XmlType(name = "UndertowType", propOrder = {
		"subsystem",
		"securityRealms",
		"interfaces",
		"socketBindings"
})
@XmlAccessorType(XmlAccessType.FIELD)
public class UndertowConfiguration {

	@XmlElement(namespace = NS_UNDERTOW)
	private UndertowSubsystem subsystem;

	@XmlElement(name = "security-realm")
	private List<SecurityRealm> securityRealms = new ArrayList<>();

	@XmlElement(name = "interface")
	private List<Interface> interfaces = new ArrayList<>();

	@XmlElement(name = "socket-binding")
	private List<SocketBinding> socketBindings = new ArrayList<>();

	@XmlTransient
	private Map<String, SecurityRealm> securityRealmsMap = new HashMap<>();

	@XmlTransient
	private Map<String, Interface> interfacesMap = new HashMap<>();

	@XmlTransient
	private Map<String, SocketBinding> socketBindingsMap = new HashMap<>();

	@XmlTransient
	private Map<String, UndertowSubsystem.FileHandler> handlersMap = new HashMap<>();

	@XmlTransient
	private Map<String, UndertowSubsystem.AbstractFilter> filtersMap = new HashMap<>();

	/**
	 * Initializes various maps speeding up access to different configuration parts
	 * by identifier (e.g., socket binding)
	 */
	public synchronized void init() {
		for (SocketBinding binding : socketBindings) {
			socketBindingsMap.put(binding.getName(), binding);
		}
		for (SecurityRealm realm : securityRealms) {
			securityRealmsMap.put(realm.getName(), realm);
		}
		for (Interface iface : interfaces) {
			interfacesMap.put(iface.getName(), iface);
		}
		if (subsystem != null) {
			for (UndertowSubsystem.FileHandler handler : subsystem.getFileHandlers()) {
				handlersMap.put(handler.getName(), handler);
			}
			if (subsystem.getFilters() != null) {
				for (UndertowSubsystem.ResponseHeaderFilter filter : subsystem.getFilters().getResponseHeaders()) {
					filtersMap.put(filter.getName(), filter);
				}
				for (UndertowSubsystem.ErrorPageFilter filter : subsystem.getFilters().getErrorPages()) {
					filtersMap.put(filter.getName(), filter);
				}
				for (UndertowSubsystem.CustomFilter filter : subsystem.getFilters().getCustomFilters()) {
					filtersMap.put(filter.getName(), filter);
				}
			}
		}
	}

	/**
	 * Returns {@link org.ops4j.pax.web.service.undertow.internal.configuration.model.UndertowSubsystem.FileHandler}
	 * by name
	 * @param name
	 * @return
	 */
	public UndertowSubsystem.FileHandler handler(String name) {
		return handlersMap.get(name);
	}

	/**
	 * Returns {@link org.ops4j.pax.web.service.undertow.internal.configuration.model.UndertowSubsystem.AbstractFilter}
	 * by name
	 * @param name
	 * @return
	 */
	public UndertowSubsystem.AbstractFilter filter(String name) {
		return filtersMap.get(name);
	}

	/**
	 * Returns {@link SocketBinding} by name
	 * @param name
	 * @return
	 */
	public SocketBinding socketBinding(String name) {
		return socketBindingsMap.get(name);
	}

	/**
	 * Returns {@link Interface} by name
	 * @param name
	 * @return
	 */
	public Interface interfaceRef(String name) {
		return interfacesMap.get(name);
	}

	/**
	 * Return {@link SecurityRealm} by name
	 * @param name
	 * @return
	 */
	public SecurityRealm securityRealm(String name) {
		return securityRealmsMap.get(name);
	}

	/**
	 * Returns valid information about interfaces+port to listen on
	 * @param socketBindingName
	 * @return
	 */
	public BindingInfo bindingInfo(String socketBindingName) {
		SocketBinding sb = socketBinding(socketBindingName);
		if (sb == null) {
			throw new IllegalArgumentException("Can't find socket binding with name \"" + socketBindingName + "\"");
		}
		Interface iface = interfaceRef(sb.getInterfaceRef());
		if (iface == null) {
			throw new IllegalArgumentException("Can't find interface with name \"" + sb.getInterfaceRef() + "\"");
		}
		BindingInfo result = new BindingInfo(sb.getPort());
		for (Interface.InetAddress address : iface.getAddresses()) {
			result.getAddresses().add(address.getIp());
		}

		return result;
	}

	public void setSubsystem(UndertowSubsystem subsystem) {
		this.subsystem = subsystem;
	}

	public UndertowSubsystem getSubsystem() {
		return subsystem;
	}

	public List<SecurityRealm> getSecurityRealms() {
		return securityRealms;
	}

	public List<Interface> getInterfaces() {
		return interfaces;
	}

	public List<SocketBinding> getSocketBindings() {
		return socketBindings;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("{\n\tsubsystem: " + subsystem + "\n");
		sb.append("\tsecurity realms: {");
		for (SecurityRealm sr : securityRealms) {
			sb.append("\n\t\t" + sr);
		}
		sb.append("\n\t}\n\tinterfaces: {");
		for (Interface i : interfaces) {
			sb.append("\n\t\t" + i);
		}
		sb.append("\n\t}\n\tsocket bindings: {");
		for (SocketBinding b : socketBindings) {
			sb.append("\n\t\t" + b);
		}
		sb.append("\n\t}\n}\n");
		return sb.toString();
	}

	/**
	 * Set of IP addresses to bind server socket to + port information (same for each interface)
	 */
	public static class BindingInfo {

		private List<String> addresses = new ArrayList<>();
		private int port;

		public BindingInfo(int port) {
			this.port = port;
		}

		public List<String> getAddresses() {
			return addresses;
		}

		public int getPort() {
			return port;
		}

	}

}
