package org.camunda.tngp.broker.transport.clientapi;

import static org.camunda.tngp.dispatcher.impl.log.LogBufferAppender.RESULT_PADDING_AT_END_OF_PARTITION;

import org.agrona.MutableDirectBuffer;
import org.camunda.tngp.dispatcher.ClaimedFragment;
import org.camunda.tngp.dispatcher.Dispatcher;
import org.camunda.tngp.transport.protocol.Protocols;
import org.camunda.tngp.transport.protocol.TransportHeaderDescriptor;
import org.camunda.tngp.transport.requestresponse.RequestResponseProtocolHeaderDescriptor;
import org.camunda.tngp.util.buffer.BufferWriter;

/**
 * Writes a response according to the request response protocol
 */
public class ResponseWriter
{

    protected static final int STATIC_HEADER_LENGTH = TransportHeaderDescriptor.HEADER_LENGTH +
            RequestResponseProtocolHeaderDescriptor.HEADER_LENGTH;

    protected final TransportHeaderDescriptor transportHeaderDescriptor = new TransportHeaderDescriptor();
    protected final RequestResponseProtocolHeaderDescriptor protocolHeaderDescriptor = new RequestResponseProtocolHeaderDescriptor();

    protected Dispatcher sendBuffer;
    protected ClaimedFragment claimedFragment = new ClaimedFragment();

    public ResponseWriter(Dispatcher sendBuffer)
    {
        this.sendBuffer = sendBuffer;
    }

    public boolean tryWrite(int channelId, long connectionId, long requestId, BufferWriter bodyWriter)
    {
        final int responseLength = STATIC_HEADER_LENGTH + bodyWriter.getLength();

        long claimedOffset = -1;

        do
        {
            claimedOffset = sendBuffer.claim(claimedFragment, responseLength, channelId);
        }
        while (claimedOffset == RESULT_PADDING_AT_END_OF_PARTITION);

        boolean isSent = false;

        if (claimedOffset >= 0)
        {
            try
            {
                writeResponseToFragment(connectionId, requestId, bodyWriter);

                claimedFragment.commit();
                isSent = true;
            }
            catch (RuntimeException e)
            {
                claimedFragment.abort();
                throw e;
            }
        }

        return isSent;
    }


    protected void writeResponseToFragment(long connectionId, long requestId, BufferWriter bodyWriter)
    {
        final MutableDirectBuffer buffer = claimedFragment.getBuffer();
        int offset = claimedFragment.getOffset();

        // transport protocol header
        transportHeaderDescriptor.wrap(buffer, offset)
            .protocolId(Protocols.REQUEST_RESPONSE);

        offset += TransportHeaderDescriptor.HEADER_LENGTH;

        // request/response protocol header
        protocolHeaderDescriptor.wrap(buffer, offset)
            .connectionId(connectionId)
            .requestId(requestId);

        offset += RequestResponseProtocolHeaderDescriptor.HEADER_LENGTH;

        bodyWriter.write(buffer, offset);

    }

}
