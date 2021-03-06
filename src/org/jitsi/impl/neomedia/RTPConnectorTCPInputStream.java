/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package org.jitsi.impl.neomedia;

import java.io.*;
import java.net.*;

import org.ice4j.socket.*;
import org.jitsi.impl.neomedia.transform.*;
import org.jitsi.service.libjitsi.*;
import org.jitsi.service.packetlogging.*;
import org.jitsi.util.*;

/**
 * RTPConnectorInputStream implementation for TCP protocol.
 *
 * @author Sebastien Vincent
 */
public class RTPConnectorTCPInputStream
    extends TransformInputStream
{
    /**
     * The <tt>Logger</tt> used by instances for logging output.
     */
    private static final Logger logger
        = Logger.getLogger(RTPConnectorTCPInputStream.class);

    /**
     * TCP socket used to receive data.
     */
    private final Socket socket;

    /**
     * Initializes a new <tt>RTPConnectorInputStream</tt> which is to receive
     * packet data from a specific TCP socket.
     *
     * @param socket the TCP socket the new instance is to receive data from
     */
    public RTPConnectorTCPInputStream(Socket socket)
    {
        this.socket = socket;

        if(socket != null)
        {
            try
            {
                socket.setReceiveBufferSize(65535);
            }
            catch (Throwable t)
            {
            }

            closed = false;
            receiverThread = new Thread(this);
            receiverThread.start();
        }
    }

    /**
     * Close this stream, stops the worker thread.
     */
    @Override
    public synchronized void close()
    {
        closed = true;
        if(socket != null)
        {
            try
            {
                socket.close();
            }
            catch(IOException e)
            {
            }
        }
    }

    /**
     * Log the packet.
     *
     * @param p packet to log
     */
    @Override
    protected void doLogPacket(DatagramPacket p)
    {
        if(socket.getLocalAddress() == null)
            return;

        // Do not log the packet if this one has been processed (and already
        // logged) by the ice4j stack.
        if(socket instanceof MultiplexingSocket)
            return;

        PacketLoggingService packetLogging = LibJitsi.getPacketLoggingService();

        if (packetLogging != null)
            packetLogging.logPacket(
                    PacketLoggingService.ProtocolName.RTP,
                    (p.getAddress() != null)
                            ? p.getAddress().getAddress()
                            : new byte[] { 0,0,0,0 },
                    p.getPort(),
                    socket.getLocalAddress().getAddress(),
                    socket.getLocalPort(),
                    PacketLoggingService.TransportName.TCP,
                    false,
                    p.getData(),
                    p.getOffset(),
                    p.getLength());
    }

    /**
     * Receive packet.
     *
     * @param p packet for receiving
     * @throws IOException if something goes wrong during receiving
     */
    @Override
    protected void receivePacket(DatagramPacket p)
        throws IOException
    {
        int len = -1;
        byte data[] = null;

        try
        {
            data = p.getData();
            len = socket.getInputStream().read(data);
        }
        catch(Exception e)
        {
            logger.info("problem read: " + e);
        }

        if(len > 0)
        {
            p.setData(data);
            p.setLength(len);
            p.setAddress(socket.getInetAddress());
            p.setPort(socket.getPort());
        }
        else
        {
            throw new IOException("Failed to read on TCP socket");
        }
    }
}
