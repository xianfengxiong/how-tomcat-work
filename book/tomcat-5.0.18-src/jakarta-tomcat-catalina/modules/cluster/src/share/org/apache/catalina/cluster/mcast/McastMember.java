/*
 * $Header: /home/cvs/jakarta-tomcat-catalina/modules/cluster/src/share/org/apache/catalina/cluster/mcast/McastMember.java,v 1.2 2003/11/16 22:22:45 fhanik Exp $
 * $Revision: 1.2 $
 * $Date: 2003/11/16 22:22:45 $
 *
 * ====================================================================
 *
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 1999 The Apache Software Foundation.  All rights
 * reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution, if
 *    any, must include the following acknowlegement:
 *       "This product includes software developed by the
 *        Apache Software Foundation (http://www.apache.org/)."
 *    Alternately, this acknowlegement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 *
 * 4. The names "The Jakarta Project", "Tomcat", and "Apache Software
 *    Foundation" must not be used to endorse or promote products derived
 *    from this software without prior written permission. For written
 *    permission, please contact apache@apache.org.
 *
 * 5. Products derived from this software may not be called "Apache"
 *    nor may "Apache" appear in their names without prior written
 *    permission of the Apache Group.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE APACHE SOFTWARE FOUNDATION OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 *
 * [Additional notices, if required by prior licensing conditions]
 *
 */

package org.apache.catalina.cluster.mcast;

import org.apache.catalina.cluster.Member;

/**
 * A <b>membership</b> implementation using simple multicast.
 * This is the representation of a multicast member.
 * Carries the host, and port of the this or other cluster nodes.
 *
 * @author Filip Hanik
 * @version $Revision: 1.2 $, $Date: 2003/11/16 22:22:45 $
 */

import org.apache.catalina.cluster.io.XByteBuffer;
public class McastMember implements Member, java.io.Serializable {

    /**
     * Digits, used for "superfast" de-serialization of an
     * IP address
     */
    final transient static char[] digits = {
        '0', '1', '2', '3', '4', '5',
        '6', '7', '8', '9'};

    /**
     * Public properties specific to this implementation
     */
    public static final transient String TCP_LISTEN_PORT = "tcpListenPort";
    public static final transient String TCP_LISTEN_HOST = "tcpListenHost";
    public static final transient String MEMBER_NAME = "memberName";

    /**
     * The listen host for this member
     */
    protected String host;
    /**
     * The tcp listen port for this member
     */
    protected int port;
    /**
     * The name for this member, has be be unique within the cluster.
     */
    private String name;
    /**
     * Counter for how many messages have been sent from this member
     */
    protected int msgCount = 0;
    /**
     * The number of milliseconds since this members was
     * created, is kept track of using the start time
     */
    protected long memberAliveTime = 0;


    /**
     * Construct a new member object
     * @param name - the name of this member, cluster unique
     * @param host - the tcp listen host
     * @param port - the tcp listen port
     */
    public McastMember(String name,
                       String host,
                       int port,
                       long aliveTime) {
        this.host = host;
        this.port = port;
        this.name = name;
        this.memberAliveTime=aliveTime;
    }

    /**
     *
     * @returns a Hashmap containing the following properties:<BR>
     * 1. tcpListenPort - the port this member listens to for messages - string<BR>
     * 2. tcpListenHost - the host address of this member - string<BR>
     * 3. memberName    - the name of this member - string<BR>
     */
    public java.util.HashMap getMemberProperties() {
        java.util.HashMap map = new java.util.HashMap(2);
        map.put(this.TCP_LISTEN_HOST,this.host);
        map.put(this.TCP_LISTEN_PORT,String.valueOf(this.port));
        map.put(this.MEMBER_NAME,name);
        return map;
    }

    /**
     * Increment the message count.
     */
    protected void inc() {
        msgCount++;
    }

    /**
     * Create a data package to send over the wire representing this member.
     * This is faster than serialization.
     * @return - the bytes for this member deserialized
     * @throws Exception
     */
    protected byte[] getData(long startTime) throws Exception {
        //package looks like
        //alive - 8 bytes
        //port - 4 bytes
        //host - 4 bytes
        //name - remaining bytes
        byte[] named = getName().getBytes();
        byte[] addr = java.net.InetAddress.getByName(host).getAddress();
        byte[] data = new byte[8+4+addr.length+named.length];
        long alive=System.currentTimeMillis()-startTime;
        System.arraycopy(XByteBuffer.toBytes((long)alive),0,data,0,8);
        System.arraycopy(XByteBuffer.toBytes(port),0,data,8,4);
        System.arraycopy(addr,0,data,12,addr.length);
        System.arraycopy(named,0,data,8+4+addr.length,named.length);
        return data;
    }
    /**
     * Deserializes a member from data sent over the wire
     * @param data - the bytes received
     * @return a member object.
     */
    protected static McastMember getMember(byte[] data) {
       //package looks like
       //alive - 8 bytes
       //port - 4 bytes
       //host - 4 bytes
       //name - remaining bytes
       byte[] alived = new byte[8];
       System.arraycopy(data, 0, alived, 0, 8);
       byte[] portd = new byte[4];
       System.arraycopy(data, 8, portd, 0, 4);
       byte[] addr = new byte[4];
       System.arraycopy(data, 12, addr, 0, 4);
       byte[] named = new byte[data.length - 16];
       System.arraycopy(data, 16, named, 0, named.length);
       return new McastMember(new String(named), addressToString(addr),
                              XByteBuffer.toInt(portd, 0),
                              XByteBuffer.toLong(alived, 0));
    }

    /**
     * Return the name of this object
     * @return a unique name to the cluster
     */
    public String getName() {
        return name;
    }

    /**
     * Return the listen port of this member
     * @return - tcp listen port
     */
    public int getPort()  {
        return this.port;
    }

    /**
     * Return the TCP listen host for this member
     * @return IP address or host name
     */
    public String getHost()  {
        return this.host;
    }

    /**
     * Contains information on how long this member has been online.
     * The result is the number of milli seconds this member has been
     * broadcasting its membership to the cluster.
     * @return nr of milliseconds since this member started.
     */
    public long getMemberAliveTime() {
       return memberAliveTime;
    }

    public void setMemberAliveTime(long time) {
       memberAliveTime=time;
    }



    /**
     * String representation of this object
     * @return
     */
    public String toString()  {
        return "org.apache.catalina.cluster.mcast.McastMember["+name+","+host+","+port+", alive="+memberAliveTime+"]";
    }

    /**
     * @see java.lang.Object.hashCode()
     * @return
     */
    public int hashCode() {
        return this.name.hashCode();
    }

    /**
     * Returns true if the param o is a McastMember with the same name
     * @param o
     * @return
     */
    public boolean equals(Object o) {
        if ( o instanceof McastMember )    {
            return this.name.equals(((McastMember)o).getName());
        }
        else
            return false;
    }

    /**
     * Converts for bytes (ip address) to a string representation of it<BR>
     * Highly optimized method.
     * @param address (4 bytes ip address)
     * @return string representation of that ip address
     */
    private static final String addressToString(byte[] address) {
        int q, r = 0;
        int charPos = 15;
        char[] buf = new char[15];
        char dot = '.';

        int i = address[3] & 0xFF;
        for (; ; )
        {
            q = (i * 52429) >>> (19);
            r = i - ( (q << 3) + (q << 1));
            buf[--charPos] = digits[r];
            i = q;
            if (i == 0)
                break;
        }
        buf[--charPos] = dot;
        i = address[2] & 0xFF;
        for (; ; )
        {
            q = (i * 52429) >>> (19);
            r = i - ( (q << 3) + (q << 1));
            buf[--charPos] = digits[r];
            i = q;
            if (i == 0)
                break;
        }
        buf[--charPos] = dot;

        i = address[1] & 0xFF;
        for (; ; )
        {
            q = (i * 52429) >>> (19);
            r = i - ( (q << 3) + (q << 1));
            buf[--charPos] = digits[r];
            i = q;
            if (i == 0)
                break;
        }

        buf[--charPos] = dot;
        i = address[0] & 0xFF;

        for (; ; )
        {
            q = (i * 52429) >>> (19);
            r = i - ( (q << 3) + (q << 1));
            buf[--charPos] = digits[r];
            i = q;
            if (i == 0)
                break;
        }
        return new String(buf, charPos, 15 - charPos);
    }
}
