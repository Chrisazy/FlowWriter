//  (c) Copyright 2013 Hewlett-Packard Development Company, L.P.
//  Autogenerated
package com.purdue.fw.impl;

import static com.hp.of.lib.instr.ActionType.OUTPUT;
import static com.hp.of.lib.match.FieldFactory.createBasicField;
import static com.hp.of.lib.match.OxmBasicFieldType.ETH_TYPE;
import static com.hp.of.lib.match.OxmBasicFieldType.IPV4_DST;
import static com.hp.of.lib.match.OxmBasicFieldType.IPV4_SRC;
import static com.hp.of.lib.match.OxmBasicFieldType.IP_PROTO;

import java.util.EnumSet;
import java.util.Set;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.ReferencePolicy;

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
import com.hp.util.pkt.Ip;
import com.hp.util.pkt.Packet;
import com.hp.util.pkt.ProtocolId;

/**
 * Sample Packet service implementation.
 */
@Component(immediate = true)
public class PacketManager implements SequencedPacketListener, DataPathListener {

	private static final long COOKIE = 0x13374468;
	private static final int FLOW_IDLE_TIMEOUT = 300;
	private static final int FLOW_HARD_TIMEOUT = 600;
	private static final int FLOW_PRIORITY = 30000;

	private static String src = "10.0.0.1";
	private static String dst = "10.0.0.2";

	private static final ProtocolVersion PV = ProtocolVersion.V_1_0;

	@Reference(name = "ControllerService", cardinality = ReferenceCardinality.OPTIONAL_UNARY, policy = ReferencePolicy.DYNAMIC)
	private ControllerService cs;
	final protected static char[] hexArray = "0123456789ABCDEF".toCharArray();

	public static String bytesToHex(byte[] bytes) {
		char[] hexChars = new char[bytes.length * 2];
		for (int j = 0; j < bytes.length; j++) {
			int v = bytes[j] & 0xFF;
			hexChars[j * 2] = hexArray[v >>> 4];
			hexChars[j * 2 + 1] = hexArray[v & 0x0F];
		}
		return new String(hexChars);
	}

	private Match createMatch(IpAddress ip_src, IpAddress ip_dst) {
		MutableMatch mm = MatchFactory.createMatch(PV)
				.addField(createBasicField(PV, ETH_TYPE, EthernetType.IPv4))
				.addField(createBasicField(PV, IP_PROTO, IpProtocol.TCP))
				.addField(createBasicField(PV, IP_PROTO, IpProtocol.UDP))
				.addField(createBasicField(PV, IP_PROTO, IpProtocol.ICMP))
				.addField(createBasicField(PV, IPV4_SRC, ip_src))
				.addField(createBasicField(PV, IPV4_DST, ip_dst));
		return (Match) mm.toImmutable();
	}

	public OfmFlowMod createFlowMod(String ip_src, String ip_dst) {
		// Create a 1.0 FlowMod ADD message...
		OfmMutableFlowMod fm = (OfmMutableFlowMod) MessageFactory.create(PV,
				MessageType.FLOW_MOD, FlowModCommand.ADD);
		fm.cookie(COOKIE)
				.priority(FLOW_PRIORITY)
				.idleTimeout(FLOW_IDLE_TIMEOUT)
				.hardTimeout(FLOW_HARD_TIMEOUT)
				.match(createMatch(IpAddress.valueOf(ip_src),
						IpAddress.valueOf(ip_dst)));

		fm.addAction(ActionFactory.createAction(PV, OUTPUT, Port.CONTROLLER,ActOutput.CONTROLLER_MAX));
		fm.addAction(ActionFactory.createAction(PV, OUTPUT, Port.NORMAL,ActOutput.CONTROLLER_NO_BUFFER));
		return (OfmFlowMod) fm.toImmutable();
	}

	public void sendMod(OfmFlowMod flowMod) {
		Set<DataPathInfo> datapathIds = cs.getAllDataPathInfo();
		for (DataPathInfo dataPathId : datapathIds) {
			try {
				cs.sendFlowMod(flowMod, dataPathId.dpid());
			} catch (OpenflowException e) {
				e.printStackTrace();
			}
		}
	}

	/*
	 * Wrapper for the REST API to consume easily without needing to know or
	 * care what FlowMods are
	 */
	public void createAndSendMod(String ip_src, String ip_dst) {
		sendMod(createFlowMod(ip_src, ip_dst));
	}

	/* Bind the controller service. */
	protected void bindControllerService(ControllerService cs) {
		System.out.println("PFW: Binding the cs!");
		if (this.cs != null) {
			System.out.println("PFW: We already had one, though..");
			return;
		}
		this.cs = cs;
		cs.addPacketListener(this, PacketListenerRole.OBSERVER, 5000,
				EnumSet.of(ProtocolId.TCP, ProtocolId.UDP));
		cs.addDataPathListener(this);
		
		
		// TODO: This will NOT be directly called here, it will be called from
		// elsewhere
		

		if(cs.isHybridMode()) {
			System.out.println("PFW: Adding flowmods");
			sendMod(createFlowMod(src, dst));
			sendMod(createFlowMod(dst, src));
		}
			
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
		System.err.println("MYAPPERR: ERROR - " + e.text());
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
		if ((src_ip.equals(src) && dst_ip.equals(dst))
				|| (src_ip.equals(src) && dst_ip.equals(dst))) {

			System.out.println("PFWMAP4: "
					+ bytesToHex(msg.getPacketIn().getData()));

			/*
			 * // It was part of our conversation!
			 * System.out.println("MAP: Part of our convo");
			 * if(p.has(ProtocolId.TCP)) { Tcp tcp = p.get(ProtocolId.TCP);
			 * System.out.println(tcp.); } if(p.has(ProtocolId.ICMP)) {
			 * System.out.println("MAP Here we go, getting ICMP!"); Icmp icmp =
			 * p.get(ProtocolId.ICMP); System.out.println("MAP2: START DATA");
			 * System.out.println("MAP2: "+ bytesToHex(icmp.bytes()));
			 * System.out.println("MAP2: END DATA"); System.out.flush(); } else
			 * if(p.has(ProtocolId.UNKNOWN)) { UnknownProtocol data =
			 * ip.get(ProtocolId.UNKNOWN);
			 * System.out.println("MAP: Was Unknown");
			 * System.out.println("MAP: "+bytesToHex(data.bytes())); } else {
			 * System.out.println("MAP: Had neither:: "+msg.toDebugString()); }
			 */
		}
	}

	@Override
	public void event(DataPathEvent e) {
		if(!cs.isHybridMode())
			return;
		try {
			cs.sendFlowMod(createFlowMod(src, dst), e.dpid());
		} catch (OpenflowException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
	}

	@Override
	public void queueEvent(QueueEvent arg0) {
		// TODO Auto-generated method stub

	}
}
