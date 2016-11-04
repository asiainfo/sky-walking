package com.a.eye.skywalking.storage.data;

import com.a.eye.datacarrier.consumer.IConsumer;
import com.a.eye.skywalking.storage.data.file.DataFileWriter;
import com.a.eye.skywalking.storage.data.index.IndexDBConnector;
import com.a.eye.skywalking.storage.data.index.IndexMetaGroup;
import com.a.eye.skywalking.storage.data.index.IndexOperator;
import com.a.eye.skywalking.storage.data.index.IndexDBConnectorCache;

import java.util.Iterator;
import java.util.List;

public class SpanDataConsumer implements IConsumer<SpanData> {

    private IndexDBConnectorCache cache;
    private DataFileWriter        fileWriter;

    @Override
    public void consume(List<SpanData> data) {

        Iterator<IndexMetaGroup> iterator = fileWriter.write(data).group();

        while (iterator.hasNext()) {
            IndexMetaGroup metaGroup = iterator.next();
            IndexOperator indexOperator = IndexOperator.newOperator(getDBConnector(metaGroup));
            indexOperator.batchUpdate(metaGroup);
        }

    }

    private IndexDBConnector getDBConnector(IndexMetaGroup metaGroup){
        return cache.get(metaGroup.getTimestamp());
    }

    @Override
    public void onError(List<SpanData> list, Throwable throwable) {

    }
}
