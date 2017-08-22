package org.skywalking.apm.collector.storage.define.jvm;

import org.skywalking.apm.collector.storage.define.CommonTable;

/**
 * @author pengys5
 */
public class CpuMetricTable extends CommonTable {
    public static final String TABLE = "cpu_metric";
    public static final String COLUMN_APPLICATION_INSTANCE_ID = "application_instance_id";
    public static final String COLUMN_USAGE_PERCENT = "usage_percent";
}
