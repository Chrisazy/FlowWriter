//  (c) Copyright 2013 Hewlett-Packard Development Company, L.P.
//  Autogenerated
package com.purdue.fw.impl;

import static com.hp.of.lib.instr.ActionType.OUTPUT;
import static com.hp.of.lib.match.FieldFactory.createBasicField;
import static com.hp.of.lib.match.OxmBasicFieldType.ETH_TYPE;
import static com.hp.of.lib.match.OxmBasicFieldType.IPV4_DST;
import static com.hp.of.lib.match.OxmBasicFieldType.IPV4_SRC;
import static com.hp.of.lib.match.OxmBasicFieldType.IP_PROTO;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.EnumSet;
import java.util.Set;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.Service;

import com.hp.of.ctl.ControllerService;
import com.hp.of.ctl.DataPathEvent;
import com.hp.of.ctl.DataPathListener;
import com.hp.of.ctl.ErrorEvent;
import com.hp.of.ctl.QueueEvent;
import com.hp.of.ctl.pkt.MessageContext;
import com.hp.of.ctl.pkt.PacketListenerRole;
import com.hp.of.ctl.pkt.SequencedPacketListener;
import com.hp.of.lib.OpenflowException;
import com.hp.of.lib.ProtocolVersion;
import com.hp.of.lib.dt.DataPathInfo;
import com.hp.of.lib.instr.ActOutput;
import com.hp.of.lib.instr.ActionFactory;
import com.hp.of.lib.match.Match;
import com.hp.of.lib.match.MatchFactory;
import com.hp.of.lib.match.MutableMatch;
import com.hp.of.lib.msg.FlowModCommand;
import com.hp.of.lib.msg.MessageFactory;
import com.hp.of.lib.msg.MessageType;
import com.hp.of.lib.msg.OfmFlowMod;
import com.hp.of.lib.msg.OfmMutableFlowMod;
import com.hp.of.lib.msg.Port;
import com.hp.util.ip.EthernetType;
import com.hp.util.ip.IpAddress;
import com.hp.util.ip.IpProtocol;
import com.hp.util.ip.PortNumber;
import com.hp.util.pkt.Ip;
import com.hp.util.pkt.Packet;
import com.hp.util.pkt.ProtocolId;
import com.purdue.fw.api.InvalidInputException;
import com.purdue.fw.api.PacketService;

/**
 * This file contains the business logic for the app, obviously. We use this in
 * kind of an atypical way, because we have one file for our logic, but also
 * expose the API Service with it, instead of using a delegate as in the guide.
 * 
 * This file implements 3 Interfaces, and I will go into detail about each one
 * in this huge comment.
 * 
 * PacketService is an Interface that WE create (though, the generator made it
 * in our case) and is used for the OSGi layer stuff as described in the fw-rs
 * counterpart to this at the top of PacketResource. See the comments on the
 * implemented methods createAndSendFlowMods and stopCapture for more
 * information on the functionality of this Service, but for this section, just
 * know that this class MUST implement PacketService for use with PacketResource
 * (fw-rs). Additionally, this is the same reason we have the @Componenet and @Service
 * annotations on this class. More info below.
 * 
 * SequencedPacketListener is really the heart of this application. When we
 * implement this, we have to implement event(MessageContext msg) (as seen below
 * in this file). This is used to receive PACKET_IN messages, which happen at a
 * few different times. The first situation that causes us to receive a
 * PACKET_IN is during Pure SDN mode, AKA when the controller is NOT in hybrid
 * mode. Pure SDN mode basically just means send ALL packets to the controller.
 * The other time that we get an event is when we've set up flows that redirect
 * packets to the controller. See createMatch and createFlowMod for more info on
 * that, but these comments assume basic knowledge of SDN and Openflow.
 * 
 * DataPathListener is another very important interface that adds the
 * event(DatapathEvent e) and errorEvent(ErrorEvent e) methods. This is pretty
 * straight forward, I think. When a new datapath comes online, we haven't given
 * it our flow rules yet, so when it does, we add them. More details in the
 * comment for that method specifically, but that's all it's for. When a
 * datapath is recognized by the controller, all of the registered
 * DataPathListeners (see next section for information about registering) are
 * sent a DatapathEvent that contains the information about the new datapath as
 * well as the event itself, such as when
 * 
 * For these previous two Interfaces, you'll notice they're both Listeners. If
 * you're a Java programmer, you likely understand the fundamentals of
 * registering listeners. If you're not, see
 * https://docs.oracle.com/javase/tutorial/uiswing/events/intro.html for more
 * information. To register these listeners, we use an instance of the
 * ControllerService class, retrieved via @Reference (see comment in code).
 * 
 * The basic functionality of the program comes in a few stages. These
 * functionalities are only outlined here, descriptions and explanations are
 * provided in the code, this is simply a roadmap.
 * 
 * 1) Initialization - Bind the Controller, register Listeners
 * 2) Receive start API call - Check Input, create and send flow mods
 * 3) Receive PACKET_IN message - Check packet for match,  
 * 4) Receive stop API call - Check Input, create pcap from list of packets
 * 
 * */

/**
 * Here I explain @Component and @Service
 * 
 * @Component just registers this with the Virgo OSGi layer as a Componenet,
 *            though I actually don't know what that entails. Apologies. I'm
 *            quite sure the immediate = true indicates to the controller that
 *            an instance should be created as soon as possible, instead of
 *            waiting for someone to ask for an instance of this component. This
 *            is mostly unnecesary for our application, but was very helpful in
 *            debugging, while I simulated the rest calls and did all of my
 *            logic in here. See bindControllerService for more info.
 * 
 * @Service registers this as a Service, and in tandem with implementing
 *          PacketService (Though the PacketService interface isn't anything
 *          special on its own), we are now able to grab a reference to this
 *          class without knowing it exists, and use the methods from the
 *          interface accordingly. This is very important in fw-rs and is done
 *          for the OSGi layer so that many components may use the same instance
 *          of a Service, as is often how it should be done
 * */
@Component(immediate = true)
@Service
public class PacketManager implements PacketService, SequencedPacketListener,
		DataPathListener {

	private static final long COOKIE = 0x13374468;
	private static final int FLOW_IDLE_TIMEOUT = 300;
	private static final int FLOW_HARD_TIMEOUT = 600;
	private static final int FLOW_PRIORITY = 30000;

	private String src = null;
	private String filename = null;
	private String dst = null;
	private int srcport = -1;
	private int dstport = -1;
	private int numPackets = 0;

	private static final ProtocolVersion PV = ProtocolVersion.V_1_0;

	@Reference(name = "ControllerService", cardinality = ReferenceCardinality.OPTIONAL_UNARY, policy = ReferencePolicy.DYNAMIC)
	private ControllerService cs;
	final protected static char[] hexArray = "0123456789ABCDEF".toCharArray();
	private static final String IPADDRESS_PATTERN = "^([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\."
			+ "([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\."
			+ "([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\."
			+ "([01]?\\d\\d?|2[0-4]\\d|25[0-5])$";

	private Match createMatch(IpAddress ip_src, IpAddress ip_dst,
			PortNumber src_port, PortNumber dst_port) {
		MutableMatch mm = MatchFactory
				.createMatch(PV)
				.addField(createBasicField(PV, ETH_TYPE, EthernetType.IPv4))
				.addField(createBasicField(PV, IP_PROTO, IpProtocol.TCP))
				// .addField(createBasicField(PV, TCP_SRC, src_port))
				// .addField(createBasicField(PV, TCP_DST, dst_port))
				.addField(createBasicField(PV, IPV4_SRC, ip_src))
				.addField(createBasicField(PV, IPV4_DST, ip_dst));

		return (Match) mm.toImmutable();
	}

	public OfmFlowMod createFlowMod(String ip_src, String ip_dst, int src_port,
			int dst_port) throws InvalidInputException {
		// Check the input
		if (ip_src == null || !ip_src.matches(IPADDRESS_PATTERN))
			throw new InvalidInputException("IP_SRC", ip_src);
		else if (ip_dst == null || !ip_dst.matches(IPADDRESS_PATTERN))
			throw new InvalidInputException("IP_DST", ip_dst);
		if (src_port < 1 || src_port > 65535)
			throw new InvalidInputException("SRC_PORT", src_port + "");
		if (dst_port < 1 || dst_port > 65535)
			throw new InvalidInputException("DST_PORT", dst_port + "");

		// Create a 1.0 FlowMod ADD message...
		OfmMutableFlowMod fm = (OfmMutableFlowMod) MessageFactory.create(PV,
				MessageType.FLOW_MOD, FlowModCommand.ADD);
		fm.cookie(COOKIE)
				.priority(FLOW_PRIORITY)
				.idleTimeout(FLOW_IDLE_TIMEOUT)
				.hardTimeout(FLOW_HARD_TIMEOUT)
				.match(createMatch(IpAddress.valueOf(ip_src),
						IpAddress.valueOf(ip_dst),
						PortNumber.valueOf(src_port),
						PortNumber.valueOf(dst_port)));

		fm.addAction(ActionFactory.createAction(PV, OUTPUT, Port.CONTROLLER,
				ActOutput.CONTROLLER_MAX));
		fm.addAction(ActionFactory.createAction(PV, OUTPUT, Port.NORMAL,
				ActOutput.CONTROLLER_NO_BUFFER));
		return (OfmFlowMod) fm.toImmutable();
	}

	public void sendMod(OfmFlowMod flowMod) throws InvalidInputException,
			OpenflowException {
		Set<DataPathInfo> datapathIds = cs.getAllDataPathInfo();
		for (DataPathInfo dataPathId : datapathIds) {
			cs.sendFlowMod(flowMod, dataPathId.dpid());
		}
	}

	/*
	 * Wrapper for the REST API to consume easily without needing to know or
	 * care what FlowMods are
	 */
	public String createAndSendMod(String ip_src, String ip_dst, int src_port,
			int dst_port) throws InvalidInputException, OpenflowException {

		System.out.println("PFW: Creating and sending mods");
		cs.addPacketListener(this, PacketListenerRole.OBSERVER, 5000,
				EnumSet.of(ProtocolId.TCP, ProtocolId.UDP));
		cs.addDataPathListener(this);

		this.src = ip_src;
		this.dst = ip_dst;
		this.srcport = src_port;
		this.dstport = dst_port;
		sendMod(createFlowMod(ip_src, ip_dst, src_port, dst_port));
		sendMod(createFlowMod(ip_dst, ip_src, dst_port, src_port));

		final byte[] pcap_global = { (byte) 0xD4, (byte) 0xC3, (byte) 0xB2,
				(byte) 0xA1, (byte) 0x02, (byte) 0x00, (byte) 0x04,
				(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
				(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
				(byte) 0x00, (byte) 0xFF, (byte) 0xFF, (byte) 0x00,
				(byte) 0x00, (byte) 0x01, (byte) 0x00, (byte) 0x00, (byte) 0x00 };

		// Write bytes to tmp file.
		filename = "/opt/sdn/PFW" + System.currentTimeMillis() + ".pcap";
		final File tmpPcap = new File(filename);
		FileOutputStream tmpOutputStream = null;
		try {
			tmpOutputStream = new FileOutputStream(tmpPcap, false);
			tmpOutputStream.write(pcap_global);
		} catch (FileNotFoundException e) {
			System.out.println("FileNotFoundException: " + e);
			return "{\"error\":\"FileNotFoundException\"}";
		} catch (IOException e) {
			System.out.println("IOException: " + e);
			return "{\"error\":\"IOException\"}";
		} finally {
			if (tmpOutputStream != null)
				try {
					tmpOutputStream.close();
				} catch (IOException e) {
					System.out.println("IOException: " + e);
					return "{\"error\":\"IOException\"}";
				}
		}
		return "{\"filename\":\""+filename+"\"";
	}

	public String stopCapture(String ip_src, String ip_dst, int src_port,
			int dst_port) throws InvalidInputException, OpenflowException {
		// Check the input
		if (ip_src == null || !ip_src.matches(IPADDRESS_PATTERN))
			throw new InvalidInputException("IP_SRC", ip_src);
		else if (ip_dst == null || !ip_dst.matches(IPADDRESS_PATTERN))
			throw new InvalidInputException("IP_DST", ip_dst);
		if (src_port < 1 || src_port > 65535)
			throw new InvalidInputException("SRC_PORT", src_port + "");
		if (dst_port < 1 || dst_port > 65535)
			throw new InvalidInputException("DST_PORT", dst_port + "");

		cs.removeDataPathListener(this);
		cs.removePacketListener(this);
		return "{\"numPackets\":"+numPackets+"}";
	}

	/* Bind the controller service. */
	protected void bindControllerService(ControllerService cs) {
		System.out.println("PFW: Binding the cs!");
		if (this.cs != null) {
			System.out.println("PFW: We already had one, though..");
			return;
		}
		this.cs = cs;
	}

	/* Unbind the controller service. */
	protected void unbindControllerService(ControllerService cs) {
		if (this.cs == cs) {
			this.cs.removePacketListener(this);
			this.cs = null;
		}
	}

	@Override
	public void errorEvent(ErrorEvent e) {
		System.err.println("PFW: ERROR - " + e.text());
	}

	@Override
	public void event(MessageContext msg) {
		System.out.println("PFW: Received msg");
		Packet p = msg.decodedPacket();
		if (!p.has(ProtocolId.IP)) {
			return;
		}
		Ip ip = p.get(ProtocolId.IP);
		String src_ip = ip.srcAddr().toString();
		String dst_ip = ip.dstAddr().toString();
		if (src == null || dst == null)
			return;
		if ((src_ip.equals(src) && dst_ip.equals(dst))
				|| (src_ip.equals(dst) && dst_ip.equals(src))) {

			long timestamp = System.currentTimeMillis();

			byte[] timestamp_sec = bigToLittleEndian(ByteBuffer.allocate(4)
					.putInt((int) (timestamp / 1000)).array());

			// TODO timestamp_usec is only milliseconds precision, we need to
			// get the time in nano and convert to microseconds
			byte[] timestamp_usec = bigToLittleEndian(ByteBuffer.allocate(4)
					.putInt((int) (timestamp % 1000) * 1000).array());

			byte[] size = bigToLittleEndian(ByteBuffer.allocate(4)
					.putInt(msg.getPacketIn().getData().length).array());

			final File tmpPcap = new File(filename);
			FileOutputStream tmpOutputStream = null;
			try {
				tmpOutputStream = new FileOutputStream(tmpPcap, true);
				tmpOutputStream.write(timestamp_sec);
				tmpOutputStream.write(timestamp_usec);
				tmpOutputStream.write(size);
				tmpOutputStream.write(size);
				tmpOutputStream.write(msg.getPacketIn().getData());
				tmpOutputStream.close();
				System.out.println("PFW: Writing new packet to file");
				numPackets++;
			} catch (FileNotFoundException e) {
				System.out.println("PFW FileNotFoundException: " + e);
				return;
			} catch (IOException e) {
				System.out.println("PFW IOException: " + e);
				return;
			} finally {
				if (tmpOutputStream != null)
					try {
						tmpOutputStream.close();
					} catch (IOException e) {
						System.out.println("PFW IOException: " + e);
					}
			}
		}
	}

	private static byte[] bigToLittleEndian(byte[] array) {
		for (int i = 0; i < array.length / 2; i++) {
			byte aux = array[i];
			array[i] = array[array.length - 1 - i];
			array[array.length - 1 - i] = aux;
		}
		return array;
	}

	@Override
	public void event(DataPathEvent e) {
		System.out.println("PFW: DataPathEvent");
		if (!cs.isHybridMode())
			return;
		try {
			cs.sendFlowMod(createFlowMod(src, dst, srcport, dstport), e.dpid());
			cs.sendFlowMod(createFlowMod(dst, src, dstport, srcport), e.dpid());
		} catch (OpenflowException e1) {
			e1.printStackTrace();
		} catch (InvalidInputException e1) {
			// Probably just means that there's no input yet, but either way,
			// nothing to see here..
			System.err.println("Purdue's Flow Writer: Invalid input");
			System.err.println("\t" + e1.name + ": " + e1.value);
		}
	}

	@Override
	public void queueEvent(QueueEvent arg0) {

	}
}
