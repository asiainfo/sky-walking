package org.skywalking.apm.collector.storage.elasticsearch.define;

import java.io.IOException;
import java.util.List;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.index.IndexNotFoundException;
import org.skywalking.apm.collector.client.elasticsearch.ElasticSearchClient;
import org.skywalking.apm.collector.core.client.Client;
import org.skywalking.apm.collector.core.storage.ColumnDefine;
import org.skywalking.apm.collector.core.storage.StorageInstaller;
import org.skywalking.apm.collector.core.storage.TableDefine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author pengys5
 */
public class ElasticSearchStorageInstaller extends StorageInstaller {

    private final Logger logger = LoggerFactory.getLogger(ElasticSearchStorageInstaller.class);

    @Override protected void defineFilter(List<TableDefine> tableDefines) {
        int size = tableDefines.size();
        for (int i = size - 1; i >= 0; i--) {
            if (!(tableDefines.get(i) instanceof ElasticSearchTableDefine)) {
                tableDefines.remove(i);
            }
        }
    }

    @Override protected boolean createTable(Client client, TableDefine tableDefine) {
        ElasticSearchClient esClient = (ElasticSearchClient)client;
        ElasticSearchTableDefine esTableDefine = (ElasticSearchTableDefine)tableDefine;
        // settings
        String settingSource = "";
        // mapping
        XContentBuilder mappingBuilder = null;
        try {
            XContentBuilder settingsBuilder = createSettingBuilder(esTableDefine);
            settingSource = settingsBuilder.string();
            mappingBuilder = createMappingBuilder(esTableDefine);
            logger.info("mapping builder str: {}", mappingBuilder.string());
        } catch (Exception e) {
            logger.error("create {} index mapping builder error", esTableDefine.getName());
        }
        Settings settings = Settings.builder().loadFromSource(settingSource).build();

        boolean isAcknowledged = esClient.createIndex(esTableDefine.getName(), esTableDefine.type(), settings, mappingBuilder);
        logger.info("create {} index with type of {} finished, isAcknowledged: {}", esTableDefine.getName(), esTableDefine.type(), isAcknowledged);
        return isAcknowledged;
    }

    private XContentBuilder createSettingBuilder(ElasticSearchTableDefine tableDefine) throws IOException {
        return XContentFactory.jsonBuilder()
            .startObject()
            .field("index.number_of_shards", tableDefine.numberOfShards())
            .field("index.number_of_replicas", tableDefine.numberOfReplicas())
            .field("index.refresh_interval", String.valueOf(tableDefine.refreshInterval()) + "s")
            .endObject();
    }

    private XContentBuilder createMappingBuilder(ElasticSearchTableDefine tableDefine) throws IOException {
        XContentBuilder mappingBuilder = XContentFactory.jsonBuilder()
            .startObject()
            .startObject("properties");

        for (ColumnDefine columnDefine : tableDefine.getColumnDefines()) {
            ElasticSearchColumnDefine elasticSearchColumnDefine = (ElasticSearchColumnDefine)columnDefine;
            mappingBuilder
                .startObject(elasticSearchColumnDefine.getName())
                .field("type", elasticSearchColumnDefine.getType().toLowerCase())
                .endObject();
        }

        mappingBuilder
            .endObject()
            .endObject();
        logger.debug("create elasticsearch index: {}", mappingBuilder.string());
        return mappingBuilder;
    }

    @Override protected boolean deleteTable(Client client, TableDefine tableDefine) {
        ElasticSearchClient esClient = (ElasticSearchClient)client;
        try {
            return esClient.deleteIndex(tableDefine.getName());
        } catch (IndexNotFoundException e) {
            logger.info("{} index not found", tableDefine.getName());
        }
        return false;
    }

    @Override protected boolean isExists(Client client, TableDefine tableDefine) {
        ElasticSearchClient esClient = (ElasticSearchClient)client;
        return esClient.isExistsIndex(tableDefine.getName());
    }
}
