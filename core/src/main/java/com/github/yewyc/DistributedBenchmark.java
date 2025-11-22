package com.github.yewyc;

import org.jboss.logging.Logger;
import org.jgroups.*;
import tech.tablesaw.api.DoubleColumn;
import tech.tablesaw.api.Table;
import tech.tablesaw.plotly.traces.ScatterTrace;
import tech.tablesaw.plotly.traces.Trace;

import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static tech.tablesaw.plotly.traces.ScatterTrace.Fill.TO_ZERO_Y;

public class DistributedBenchmark extends Benchmark {

    private static final Logger log = Logger.getLogger(DistributedBenchmark.class);

    private final JChannel channel;
    private final int expectedNumberOfNodes;
    private final CountDownLatch countDownLatch = new CountDownLatch(1);
    private final CountDownLatch reportCountDownLatch;
    private final List<RemoteTask> remoteTasks = new ArrayList<>();
    private boolean gatheredReportData = false;

    // todo rename order and rename names
    public DistributedBenchmark(long timeSec, int opsPerSec, int virtualThreads, long warmUpTimeSec, int expectedNumberOfNodes) {
        // todo fix super
        super(0, null, 0, 0, null, null, null);
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
    public Benchmark start() throws InterruptedException {

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
    public Benchmark generateReport() {
        try {
            if (this.isCoordinator()) {
                log.info("Collecting generate report messages");
                this.reportCountDownLatch.await();
                this.remoteTasks.add(new RemoteTask(this.channel.getAddress(), this.weightTasks));
                for (RemoteTask remoteTask : this.remoteTasks) {
                    System.out.println("--------------------------------");
                    System.out.println("Node: " + remoteTask.getSource());
                    System.out.println("--------------------------------");
                    for (WeightTask weightTask : this.weightTasks) {
                        // todo
                        //weightTask.getTask().report();
                    }
                    System.out.println();
                    System.out.println();
                }
                this.gatheredReportData = true;
            } else {
                this.channel.send(new ObjectMessage(null, this.weightTasks));
                log.info("Generate report message sent to the coordinator");
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return this;
    }

    @Override
    public Benchmark plot() {
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
                for (WeightTask weightTask : this.weightTasks) {
                    // todo
                    //Task task = weightTask.getTask();
                    Task task = null;
                    // PlotData plotData = task.plot(0);
                    PlotData plotData = null;
                    if (xMap.containsKey(task.getName())) {
                        List<Double> localXMap = xMap.get(task.getName());
                        List<Double> localYMap = yMap.get(task.getName());
                        // todo
                        // localXMap.addAll(Arrays.asList(plotData.xData));
                        // localYMap.addAll(Arrays.asList(plotData.yData));
                    } else {
                        names.add(task.getName());
                        // todo
                        // xMap.put(task.getName(), Arrays.asList(plotData.xData));
                        // yMap.put(task.getName(), Arrays.asList(plotData.yData));
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
            List<WeightTask> weightTasks = msg.getObject();
            this.remoteTasks.add(new RemoteTask(msg.getSrc(), weightTasks));
            this.reportCountDownLatch.countDown();
        }
    }
}
