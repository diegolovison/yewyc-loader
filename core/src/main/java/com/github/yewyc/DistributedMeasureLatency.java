package com.github.yewyc;

import org.jboss.logging.Logger;
import org.jgroups.*;
import tech.tablesaw.api.DoubleColumn;
import tech.tablesaw.api.Table;
import tech.tablesaw.plotly.traces.ScatterTrace;
import tech.tablesaw.plotly.traces.Trace;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static tech.tablesaw.plotly.traces.ScatterTrace.Fill.TO_ZERO_Y;

public class DistributedMeasureLatency extends MeasureLatency {

    private static final Logger log = Logger.getLogger(DistributedMeasureLatency.class);

    private final JChannel channel;
    private final int expectedNumberOfNodes;
    private final CountDownLatch countDownLatch = new CountDownLatch(1);
    private final CountDownLatch reportCountDownLatch;
    private final List<RemoteTask> remoteTasks = new ArrayList<>();
    private boolean gatheredReportData = false;

    public DistributedMeasureLatency(long timeSec, int opsPerSec, int virtualThreads, long warmUpTimeSec, MeasureLatencyType latencyType, int expectedNumberOfNodes) {
        super(timeSec, opsPerSec, virtualThreads, warmUpTimeSec, latencyType);
        this.expectedNumberOfNodes = expectedNumberOfNodes;
        this.reportCountDownLatch = new CountDownLatch(this.expectedNumberOfNodes - 1);

        try {
            this.channel = new JChannel();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        this.channel.setDiscardOwnMessages(true);
        this.channel.setReceiver(new ReceiverAdapter(this.channel, this.expectedNumberOfNodes, this.countDownLatch, this.remoteTasks, this.reportCountDownLatch));
        try {
            this.channel.connect("yewyc");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public MeasureLatency start() {

        try {
            try {
                this.countDownLatch.await(10, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }

            if (this.countDownLatch.getCount() != 0) {
                throw new RuntimeException("Number of nodes=" + this.countDownLatch.getCount() + " is not matching the expected=" + this.expectedNumberOfNodes);
            }

            log.info("Number of nodes are the expected=" + this.expectedNumberOfNodes + ". Starting the distributed benchmark.");

            super.start();

        } catch (Exception e){
            this.closeChannel();
            throw e;
        }
        return this;
    }

    @Override
    public MeasureLatency generateReport() {
        try {
            if (this.isCoordinator()) {
                log.info("Collecting generate report messages");
                this.reportCountDownLatch.await();
                this.remoteTasks.add(new RemoteTask(this.channel.getAddress(), this.tasks));
                for (RemoteTask remoteTask : this.remoteTasks) {
                    System.out.println("--------------------------------");
                    System.out.println("Node: " + remoteTask.getSource());
                    System.out.println("--------------------------------");
                    for (Task task : remoteTask.getTasks()) {
                        task.report(this.intervalNs);
                    }
                    System.out.println();
                    System.out.println();
                }
                this.gatheredReportData = true;
            } else {
                this.channel.send(new ObjectMessage(null, this.tasks));
                log.info("Generate report message sent to the coordinator");
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return this;
    }

    @Override
    public MeasureLatency plot() {
        if (this.isCoordinator()) {
            // the method will block until all data is collected
            if (!this.gatheredReportData) {
                throw new RuntimeException("Call generateReport method first");
            }
            List<String> names = new ArrayList<>();
            Map<String, List<Double>> xMap = new HashMap<>();
            Map<String, List<Double>> yMap = new HashMap<>();
            // TODO the data must be sorted by X before ploting
            for (RemoteTask remoteTask : this.remoteTasks) {
                for (Task task : remoteTask.getTasks()) {
                    if (xMap.containsKey(task.getName())) {
                        List<Double> localXMap = xMap.get(task.getName());
                        List<Double> localYMap = yMap.get(task.getName());
                        localXMap.addAll(new ArrayList<>(task.getXData()));
                        localYMap.addAll(new ArrayList<>(task.getYData()));
                    } else {
                        names.add(task.getName());
                        xMap.put(task.getName(), new ArrayList<>(task.getXData()));
                        yMap.put(task.getName(), new ArrayList<>(task.getYData()));
                    }
                }
            }

            List<Trace> traces = new ArrayList<>();
            for (int i = 0; i < names.size(); i++) {
                // TODO duplicated code
                int chartIndex = i + 1;
                double[] xData = xMap.get(names.get(i)).stream().mapToDouble(Double::doubleValue).toArray();
                double[] yData = yMap.get(names.get(i)).stream().mapToDouble(Double::doubleValue).toArray();

                Table cuteData =
                        Table.create("Cute Data")
                                .addColumns(
                                        DoubleColumn.create("X", xData),
                                        DoubleColumn.create("Y", yData));

                cuteData = cuteData.sortOn("X");

                ScatterTrace trace = ScatterTrace.builder(
                            cuteData.doubleColumn("X").asDoubleArray(),
                            cuteData.doubleColumn("Y").asDoubleArray()
                        )
                        .mode(ScatterTrace.Mode.LINE)
                        .name(names.get(i))
                        .xAxis("x" + chartIndex).yAxis("y" + chartIndex)
                        .fill(TO_ZERO_Y)
                        .build();
                traces.add(trace);
            }
            this.plot(traces);
        } else {
            log.info("The data will be plotted on the coordinator node. Remember to call generateReport before");
        }
        return this;
    }

    @Override
    public void close() {
        this.closeChannel();
    }

    private boolean isCoordinator() {
        return this.channel.getAddress().equals(this.channel.getView().getCoord());
    }

    private void closeChannel() {
        if (!this.channel.isClosed()) {
            this.channel.close();
        }
    }

    private static final class ReceiverAdapter implements Receiver {

        private final JChannel ch;
        private final int expectedNumberOfNodes;
        private final CountDownLatch countDownLatch;
        private final List<RemoteTask> remoteTasks;
        private final CountDownLatch reportCountDownLatch;

        public ReceiverAdapter(JChannel ch, int expectedNumberOfNodes, CountDownLatch countDownLatch, List<RemoteTask> remoteTasks, CountDownLatch reportCountDownLatch) {
            this.ch = ch;
            this.expectedNumberOfNodes = expectedNumberOfNodes;
            this.countDownLatch = countDownLatch;
            this.remoteTasks = remoteTasks;
            this.reportCountDownLatch = reportCountDownLatch;
        }

        @Override
        public void viewAccepted(View view) {
            log.infof("[%s] view: %s\n", ch.getAddress(), view);
            if (view.getMembers().size() == this.expectedNumberOfNodes) {
                this.countDownLatch.countDown();
            }
        }

        @Override
        public void receive(Message msg) {
            List<Task> tasks = msg.getObject();
            this.remoteTasks.add(new RemoteTask(msg.getSrc(), tasks));
            this.reportCountDownLatch.countDown();
        }
    }
}
