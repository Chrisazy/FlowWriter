//  (c) Copyright 2013 Hewlett-Packard Development Company, L.P.
//  Autogenerated
package com.purdue.fw.model;

import java.util.UUID;

import com.hp.api.Id;
import com.hp.api.Transportable;
import com.hp.sdn.BaseModel;
import com.hp.sdn.auditlog.AuditLogEntry;
import com.hp.sdn.Model;

/**
 * Sample Packet domain model.
 */
public class Packet extends Model<Packet> {

    private static final long serialVersionUID = 7571309040451072286L;

    // Just to make the sample a bit more interesting.
    private String name;

    /** 
     * Default constructor required for serialization.
     */
    public Packet() {
        super();
    }

    /** 
     * Creates a new Packet entity. 
     *
     * @param name Packet name
     */
    public Packet(String name) {
        super();
        this.name = name;
    }

    /** 
     * Creates a new Packet entity. 
     *
     * @param id Packet unique id
     * @param name Packet name
     */
    public Packet(Id<Packet, UUID> id, String name) {
        super(id);
        this.name = name;
    }
    
    /**
     * Get the Packet name.
     *
     * @return Packet name
     */
    public String name() {
        return name;
    }

    /**
     * Set the Packet name.
     *
     * @param name new name
     */
    public void setName(String name) {
        this.name = name;
    }
}
