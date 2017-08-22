package org.skywalking.apm.commons.datacarrier.consumer;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import org.junit.Assert;
import org.junit.Test;
import org.powermock.api.support.membermodification.MemberModifier;
import org.skywalking.apm.commons.datacarrier.DataCarrier;
import org.skywalking.apm.commons.datacarrier.SampleData;

/**
 * Created by wusheng on 2016/10/26.
 */
public class ConsumerTest {
    public static LinkedBlockingQueue<SampleData> buffer = new LinkedBlockingQueue<SampleData>();

    public static boolean isOccurError = false;

    @Test
    public void testConsumerLessThanChannel() throws IllegalAccessException {
        final DataCarrier<SampleData> carrier = new DataCarrier<SampleData>(2, 100);

        for (int i = 0; i < 100; i++) {
            Assert.assertTrue(carrier.produce(new SampleData().setName("data" + i)));
        }
        SampleConsumer consumer = new SampleConsumer();

        consumer.i = 100;
        carrier.consume(SampleConsumer.class, 1);
        Assert.assertEquals(1, ((SampleConsumer)getConsumer(carrier)).i);

        SampleConsumer2 consumer2 = new SampleConsumer2();
        consumer2.i = 100;
        carrier.consume(consumer2, 1);
        Assert.assertEquals(100, ((SampleConsumer2)getConsumer(carrier)).i);

        carrier.shutdownConsumers();
    }

    @Test
    public void testConsumerMoreThanChannel() throws IllegalAccessException, InterruptedException {
        final DataCarrier<SampleData> carrier = new DataCarrier<SampleData>(2, 100);

        for (int i = 0; i < 200; i++) {
            Assert.assertTrue(carrier.produce(new SampleData().setName("data" + i)));
        }
        SampleConsumer consumer = new SampleConsumer();

        carrier.consume(SampleConsumer.class, 5);

        Thread.sleep(2000);

        List<SampleData> result = new ArrayList<SampleData>();
        buffer.drainTo(result);

        Assert.assertEquals(200, result.size());

        HashSet<Integer> consumerCounter = new HashSet<Integer>();
        for (SampleData data : result) {
            consumerCounter.add(data.getIntValue());
        }
        Assert.assertEquals(5, consumerCounter.size());
    }

    @Test
    public void testConsumerOnError() {
        final DataCarrier<SampleData> carrier = new DataCarrier<SampleData>(2, 100);

        for (int i = 0; i < 200; i++) {
            Assert.assertTrue(carrier.produce(new SampleData().setName("data" + i)));
        }
        SampleConsumer2 consumer = new SampleConsumer2();

        consumer.onError = true;
        carrier.consume(consumer, 5);

        Assert.assertTrue(isOccurError);
    }

    class SampleConsumer2 implements IConsumer<SampleData> {
        public int i = 1;

        public boolean onError = false;

        @Override
        public void init() {

        }

        @Override
        public void consume(List<SampleData> data) {
            if (onError) {
                throw new RuntimeException("consume exception");
            }
        }

        @Override
        public void onError(List<SampleData> data, Throwable t) {
            isOccurError = true;
        }

        @Override
        public void onExit() {

        }
    }

    private IConsumer getConsumer(DataCarrier<SampleData> carrier) throws IllegalAccessException {
        ConsumerPool pool = ((ConsumerPool)MemberModifier.field(DataCarrier.class, "consumerPool").get(carrier));
        ConsumerThread[] threads = (ConsumerThread[])MemberModifier.field(ConsumerPool.class, "consumerThreads").get(pool);

        return (IConsumer)MemberModifier.field(ConsumerThread.class, "consumer").get(threads[0]);
    }
}
