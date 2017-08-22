package org.skywalking.apm.collector.ui.jetty.handler.time;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.Calendar;
import javax.servlet.http.HttpServletRequest;
import org.skywalking.apm.collector.server.jetty.ArgumentsParseException;
import org.skywalking.apm.collector.server.jetty.JettyHandler;
import org.skywalking.apm.collector.ui.service.TimeSynchronousService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author pengys5
 */
public class AllInstanceLastTimeGetHandler extends JettyHandler {

    private final Logger logger = LoggerFactory.getLogger(AllInstanceLastTimeGetHandler.class);

    @Override public String pathSpec() {
        return "/time/allInstance";
    }

    private TimeSynchronousService service = new TimeSynchronousService();

    @Override protected JsonElement doGet(HttpServletRequest req) throws ArgumentsParseException {
        Long time = service.allInstanceLastTime();
        logger.debug("all instance last time: {}", time);
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(time);

        int second = calendar.get(Calendar.SECOND);
        second = (second % 5) * 5;
        calendar.set(Calendar.SECOND, second);

        JsonObject timeJson = new JsonObject();
        timeJson.addProperty("time", calendar.getTimeInMillis());
        return timeJson;
    }

    @Override protected JsonElement doPost(HttpServletRequest req) throws ArgumentsParseException {
        throw new UnsupportedOperationException();
    }
}
