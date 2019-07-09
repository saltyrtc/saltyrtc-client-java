package org.saltyrtc.client.tests.signaling;

import org.junit.Test;
import org.saltyrtc.client.signaling.CloseCode;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

public class CloseCodeTest {

    @Test
    public void testAllCloseCodesInCLOSE_CODES_ALL() throws IllegalAccessException {
        Field[] fields = CloseCode.class.getFields();

        List<Integer> allCloseCodes = new ArrayList<>();
        List<Integer> closeCodesInCLOSE_CODES_ALL = new ArrayList<>();

        for (Field field : fields) {
            if (field.getType().equals(int.class)) {
                allCloseCodes.add(field.getInt(null));
            }
        }

        for (int closeCode : CloseCode.CLOSE_CODES_ALL) {
            closeCodesInCLOSE_CODES_ALL.add(closeCode);
        }

        assertEquals(allCloseCodes, closeCodesInCLOSE_CODES_ALL);
    }
}
