//  (c) Copyright 2013 Hewlett-Packard Development Company, L.P.
//  Autogenerated
package com.purdue.fw.rs;

import com.hp.of.lib.OpenflowException;
import com.hp.sdn.rs.misc.ControllerResource;

import java.util.UUID;

import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.DELETE;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import com.purdue.fw.model.Packet;
import com.purdue.fw.api.PacketService;

import com.hp.api.Id;
import org.w3c.dom.Entity;

/**
 * Sample Packet REST API resource.
 */
@Path("capture")
public class PacketResource extends ControllerResource {

    /**
     * Creates a new Packet and registers it.
     * <p>
     * Normal Response Code(s): ok (200)
     * <p>
     * Error Response Codes: badRequest (400), unauthorized (401), forbidden (403),
     * badMethod (405), serviceUnavailable (503)
     *
     * @param request JSON representation of a Packet to be created
     * @return JSON object*/
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    public Response start(String request) {
        PacketService svc = get(PacketService.class);
        ObjectMapper mapper = new ObjectMapper();
        
        JsonNode root = parse(mapper, request, "Packet data");
        String ip_src =  root.get("ip_src") != null ? root.get("ip_src").asText() : null;
        String ip_dst = root.get("ip_dst") != null ? root.get("ip_dst").asText() : null;
        int port_src = root.get("port_src") != null ? root.get("port_src").asInt() : 0;
        int port_dst = root.get("port_dst") != null ? root.get("port_dst").asInt() : 0;
        try {
			svc.createAndSendMod(ip_src, ip_dst, port_src, port_dst);
		} catch (Exception e) {
            return Response.status(Response.Status.BAD_REQUEST).
                    entity(e.getMessage()).type("text/plain").build();
		}

        return ok().build();
    }
    /*
    @POST
    @Produces(MediaType.TEXT_PLAIN)
    @Path("stop")
    public Response stop(String request) {
    	PacketService svc = get(PacketService.class);
        ObjectMapper mapper = new ObjectMapper();
        
        JsonNode root = parse(mapper, request, "Packet data");
        String ip_src = root.get("ip1").asText();
        String ip_dst = root.get("ip2").asText();
        int src_port = root.get("port1").asInt();
        int dst_port = root.get("port2").asInt();
        String ret = null;
        try {
			ret = svc.stopCapture(ip_src, ip_dst, src_port, dst_port);
		} catch (InvalidInputException e) {
			return null;
		} catch (OpenflowException e) {
			return null;
		}
        return ok(ret).build();
    }
    */
}
