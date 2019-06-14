package org.saltyrtc.client.messages.c2c;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.msgpack.core.MessagePacker;
import org.msgpack.jackson.dataformat.MessagePackFactory;
import org.saltyrtc.client.messages.Message;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * A task message is an opaque container for the actual message.
 *
 * All message data except the type is stored as an untyped map and can be retrieved through
 * `getData()`.
 */
public class TaskMessage extends Message {

    private final String type;
    private final Map<String, Object> data;

    public TaskMessage(String type, Map<String, Object> otherData) {
        this.type = type;
        this.data = otherData;
    }

    @Override
    public String getType() {
        return this.type;
    }

    public Map<String, Object> getData() {
        return this.data;
    }

    @Override
    public void write(MessagePacker packer) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper(new MessagePackFactory());
        final Map<String, Object> fullMsg = new HashMap<>(this.data);
        fullMsg.put("type", this.type);
        final byte[] msgBytes = objectMapper.writeValueAsBytes(fullMsg);
        packer.writePayload(msgBytes);
    }
}
