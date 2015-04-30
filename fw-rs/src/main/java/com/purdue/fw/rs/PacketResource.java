//  (c) Copyright 2013 Hewlett-Packard Development Company, L.P.
//  Autogenerated
package com.purdue.fw.rs;

import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hp.of.lib.OpenflowException;
import com.hp.sdn.rs.misc.ControllerResource;
import com.purdue.fw.api.InvalidInputException;
import com.purdue.fw.api.PacketService;

/**
 * Sample Packet REST API resource.
 */
@Path("capture")
public class PacketResource extends ControllerResource {

	PacketService svc = null;
	
	
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    public Response start(String request) {
        svc = get(PacketService.class);
        ObjectMapper mapper = new ObjectMapper();
        
        JsonNode root = parse(mapper, request, "Packet data");
        String ip_src = root.get("ip1").asText();
        String ip_dst = root.get("ip2").asText();
       // int src_port = root.get("port1").asInt();
       // int dst_port = root.get("port2").asInt();
        
        // These ports are arbitrary currently, as we're not matching on ports.
        int src_port = 1;
        int dst_port = 1;
        try {
			return ok(svc.createAndSendMod(ip_src, ip_dst, src_port, dst_port)).build();
		} catch (InvalidInputException e) {
			return deleted().build();
		} catch (OpenflowException e) {
			return deleted().build();
		}
    }
    
    @POST
    @Produces(MediaType.TEXT_PLAIN)
    @Path("stop")
    public Response stop(String request) {
    	if(svc == null)
    		return ok("ERROR").build();
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
			return ok("Failed").build();
		} catch (OpenflowException e) {
			return ok("Failed2").build();
		}
        return ok("{\"file\":\""+ret+"\"}").build();
    }
    
}
