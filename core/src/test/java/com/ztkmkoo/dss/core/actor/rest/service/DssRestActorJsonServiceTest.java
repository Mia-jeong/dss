package com.ztkmkoo.dss.core.actor.rest.service;

import akka.actor.testkit.typed.javadsl.TestProbe;
import com.ztkmkoo.dss.core.actor.AbstractDssActorTest;
import com.ztkmkoo.dss.core.actor.rest.entity.DssRestServiceRequest;
import com.ztkmkoo.dss.core.actor.rest.entity.DssRestServiceResponse;
import com.ztkmkoo.dss.core.message.rest.DssRestChannelHandlerCommand;
import com.ztkmkoo.dss.core.message.rest.DssRestMasterActorCommandRequest;
import com.ztkmkoo.dss.core.message.rest.DssRestServiceActorCommandRequest;
import com.ztkmkoo.dss.core.network.rest.enumeration.DssRestContentType;
import com.ztkmkoo.dss.core.network.rest.enumeration.DssRestMethodType;
import io.netty.util.CharsetUtil;
import lombok.Builder;
import org.junit.Test;

import java.io.Serializable;
import java.util.HashMap;

import static org.junit.Assert.*;

/**
 * Project: dss
 * Created by: @ztkmkoo(ztkmkoo@gmail.com)
 * Date: 20. 3. 30. 오전 2:41
 */
public class DssRestActorJsonServiceTest extends AbstractDssActorTest {

    private static final TestProbe<DssRestChannelHandlerCommand> probe = testKit.createTestProbe();
    private static final DssRestServiceActorCommandRequest sampleCommandRequest = new DssRestServiceActorCommandRequest(DssRestMasterActorCommandRequest
            .builder()
            .channelId("testChannelId")
            .path("/test")
            .methodType(DssRestMethodType.GET)
            .sender(probe.getRef())
            .content("{\"age\":15, \"point\": {\"type1\": 100, \"type2\": 500}}")
            .build());

    private final TestService testService = TestService
            .builder()
            .name("test")
            .path("/test")
            .methodType(DssRestMethodType.GET)
            .build();

    @Test
    public void handling() {
        final DssRestServiceResponse response = testService.handling(sampleCommandRequest);
        assertNull(response);
    }

    @Test
    public void getName() {
        assertEquals("test", testService.getName());
    }

    @Test
    public void getPath() {
        assertEquals("/test", testService.getPath());
    }

    @Test
    public void getMethodType() {
        assertEquals(DssRestMethodType.GET, testService.getMethodType());
    }

    @Test
    public void getConsume() {
        assertNotNull(testService.getConsume());
        assertEquals(DssRestContentType.APPLICATION_JSON, testService.getConsume().getContentType());
        assertEquals(CharsetUtil.UTF_8, testService.getConsume().getCharset());
    }

    @Test
    public void getProduce() {
        assertNotNull(testService.getProduce());
        assertEquals(DssRestContentType.APPLICATION_JSON, testService.getProduce().getContentType());
        assertEquals(CharsetUtil.UTF_8, testService.getProduce().getCharset());
    }


    private static class TestService extends DssRestActorJsonService<HashMap<String, Serializable>> {

        @Builder
        protected TestService(String name, String path, DssRestMethodType methodType) {
            super(name, path, methodType);
        }

        @Override
        protected DssRestServiceResponse handlingRequest(DssRestServiceRequest<HashMap<String, Serializable>> request) {
            return null;
        }
    }
}