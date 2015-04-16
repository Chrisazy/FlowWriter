//  (c) Copyright 2013 Hewlett-Packard Development Company, L.P.
//  Autogenerated
package com.purdue.fw.api;

import java.io.File;

import com.hp.of.lib.OpenflowException;


public interface PacketService {
	public void createAndSendMod(String ip_src, String ip_dst, int src_port,
			int dst_port) throws InvalidInputException, OpenflowException;

	public String stopCapture(String ip_src, String ip_dst, int src_port,
			int dst_port) throws InvalidInputException, OpenflowException;
}
