package org.skywalking.apm.collector.stream.grpc.handler;

import io.grpc.stub.StreamObserver;
import org.skywalking.apm.collector.core.framework.CollectorContextHelper;
import org.skywalking.apm.collector.remote.grpc.proto.Empty;
import org.skywalking.apm.collector.remote.grpc.proto.RemoteCommonServiceGrpc;
import org.skywalking.apm.collector.remote.grpc.proto.RemoteData;
import org.skywalking.apm.collector.remote.grpc.proto.RemoteMessage;
import org.skywalking.apm.collector.server.grpc.GRPCHandler;
import org.skywalking.apm.collector.stream.StreamModuleContext;
import org.skywalking.apm.collector.stream.StreamModuleGroupDefine;
import org.skywalking.apm.collector.stream.worker.Role;
import org.skywalking.apm.collector.stream.worker.WorkerInvokeException;
import org.skywalking.apm.collector.stream.worker.WorkerNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author pengys5
 */
public class RemoteCommonServiceHandler extends RemoteCommonServiceGrpc.RemoteCommonServiceImplBase implements GRPCHandler {

    private final Logger logger = LoggerFactory.getLogger(RemoteCommonServiceHandler.class);

    @Override public void call(RemoteMessage request, StreamObserver<Empty> responseObserver) {
        String roleName = request.getWorkerRole();
        RemoteData remoteData = request.getRemoteData();

        StreamModuleContext context = (StreamModuleContext)CollectorContextHelper.INSTANCE.getContext(StreamModuleGroupDefine.GROUP_NAME);
        Role role = context.getClusterWorkerContext().getRole(roleName);
        Object object = role.dataDefine().deserialize(remoteData);
        try {
            context.getClusterWorkerContext().lookupInSide(roleName).tell(object);
        } catch (WorkerNotFoundException | WorkerInvokeException e) {
            logger.error(e.getMessage(), e);
        }
    }
}
