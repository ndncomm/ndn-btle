/**
 * Copyright (C) 2015 Regents of the University of California.
 * @author: Jeff Thompson <jefft0@remap.ucla.edu>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * A copy of the GNU Lesser General Public License is in the file COPYING.
 */

package com.lannbox.rfduinotest;

import com.lannbox.rfduinotest.MainActivity.BTLeForwarder;
import java.io.IOException;
import java.nio.ByteBuffer;
import net.named_data.jndn.encoding.ElementListener;
import net.named_data.jndn.encoding.EncodingException;
import net.named_data.jndn.transport.Transport;

/**
 * A BTLeForwarderTransport extends Transport to send to the local BTLeForwarder.
 */
public class BTLeForwarderTransport extends Transport {
  /**
   * A BTLeForwarderTransport.ConnectionInfo extends Transport.ConnectionInfo to
   * hold a pointer to the BTLeForwarder.
   */
  public static class ConnectionInfo extends Transport.ConnectionInfo {
    /**
     * Create a ConnectionInfo with the given BTLeForwarder.
     * @param host The host for the connection.
     * @param port The port number for the connection.
     */
    public
    ConnectionInfo(BTLeForwarder forwarder)
    {
      forwarder_ = forwarder;
    }

    /**
     * Get the forwarder to the constructor.
     * @return The forwarder.
     */
    public final BTLeForwarder
    getForwader() { return forwarder_; }

    private final BTLeForwarder forwarder_;
  }

  /**
   * Determine whether this transport connecting according to connectionInfo is
   * to a node on the current machine.
   * @param connectionInfo This is ignored.
   * @return False because BTLeForwarder transports are always local.
   */
  public boolean isLocal(Transport.ConnectionInfo connectionInfo) {
    return true;
  }

  /**
   * Override to return false since connect does not need to use the onConnected
   * callback.
   * @return False.
   */
  public boolean
  isAsync() { return false; }

  /**
   * Connect to forwarder given to the info in ConnectionInfo, and use
   * elementListener.
   * @param connectionInfo A BTLeForwarderTransport.ConnectionInfo.
   * @param elementListener The ElementListener must remain valid during the
   * life of this object.
   * @param onConnected If not null, this calls onConnected.run() when the
   * connection is established. This is needed if isAsync() is true.
   * @throws IOException For I/O error.
   */
  public void
  connect
    (Transport.ConnectionInfo connectionInfo, ElementListener elementListener,
     Runnable onConnected)
    throws IOException
  {
    forwarder_ = ((ConnectionInfo)connectionInfo).getForwader();
    elementListener_ = elementListener;
  }

  /**
   * Sent buffer to the BTLeForwarder which sends to the Arduino.
   * @param buffer The buffer of data to send.  This reads from position() to
   * limit(), but does not change the position.
   * @throws IOException For I/O error.
   */
  public void
  send(ByteBuffer buffer) throws IOException
  {
    forwarder_.sendAndReceive(buffer, elementListener_);
  }

  /**
   * Do nothing since BTLeForwader reports incoming data when it gets it.
   */
  public void
  processEvents() throws IOException, EncodingException
  {
  }

  BTLeForwarder forwarder_;
  ElementListener elementListener_;
}
