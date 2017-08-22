package org.skywalking.apm.collector.ui.dao;

import com.google.gson.JsonArray;
import java.util.List;
import org.skywalking.apm.collector.storage.h2.dao.H2DAO;

/**
 * @author pengys5
 */
public class InstPerformanceH2DAO extends H2DAO implements IInstPerformanceDAO {

    @Override public List<InstPerformance> getMultiple(long timestamp, int applicationId) {
        return null;
    }

    @Override public int getMetric(int instanceId, long timeBucket) {
        return 0;
    }

    @Override public JsonArray getMetric(int instanceId, long startTimeBucket, long endTimeBucket) {
        return null;
    }
}
