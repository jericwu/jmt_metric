package jmt.engine.NetStrategies.RoutingStrategies;

import jmt.engine.NetStrategies.RoutingStrategy;
import jmt.engine.NodeSections.Router;
import jmt.engine.QueueNet.*;
import jmt.engine.random.engine.RandomEngine;

import java.util.Map;
import java.util.Random;

import static jmt.engine.QueueNet.JobClass.CLOSED_CLASS;

public class ClassSwitchStrategy extends RoutingStrategy {
    private int counter = -1;
    private int currentWeight = 0;
    // Map key is node name, not NetNode - see EmpiricalStrategy for details
    private Map<String, Integer> weights;
    private int max;
    private int gcd;
    // Map key is node name, not NetNode - see EmpiricalStrategy for details
    public Map<Object, Map<Object, Double>> outPaths;

    public ClassSwitchStrategy(Map<Object, Map<Object, Double>> outPaths) {
        this.outPaths = outPaths;
    }

    public void setOutPaths(Map<Object, Map<Object, Double>> outPaths) {
        this.outPaths = outPaths;
    }

    @Override
    public NetNode getOutClassSwitchNode(Router router, Job job) {
        NetNode ownerNode = router.getOwnerNode();

        return getOutNode(ownerNode, job);
    }

    public Job classSwitch(NetNode ownerNode, int path, Job job) {
        JobClass inClass = job.getJobClass();
        JobClass outClass = inClass;

        Random rand= new Random();
        JobClassList classList = ownerNode.getJobClasses();

        Object key = outPaths.keySet().toArray()[path];
        Map<Object, Double> prob = outPaths.get(key);

        Double randNumber = rand.nextDouble();
        Double range = 0.0;
        for (Object o: prob.keySet()){
            range += prob.get(o);

            if (randNumber <= range) {
                outClass = classList.get(Integer.parseInt(o.toString()) - 1);
                break;
            }
        }

        job.setJobClass(outClass);
        return job;
    }

    public NetNode getOutNode(NetNode ownerNode, Job job) {
        JobClass jobClass = job.getJobClass();

        RandomEngine randomEngine = ownerNode.getNetSystem().getEngine();
        NodeList nodeList = ownerNode.getOutputNodes();
        if (nodeList.size() == 0) {
            return null;
        }

        // Skips sinks for closed classes
        if (jobClass.getType() == CLOSED_CLASS) {
            NetNode[] valid = new NetNode[nodeList.size()];
            int length = 0; // Here length is used as a counter for valid
            for (int i = 0; i < nodeList.size(); i++) {
                if (!nodeList.get(i).isSink()) {
                    valid[length++] = nodeList.get(i);
                }
            }
            if (length == 0) {
                return null;
            } else if (length == 1) {
                classSwitch(ownerNode, 0, job);
                return valid[0];
            } else {
                int path = (int) Math.floor(randomEngine.raw() * length);
                classSwitch(ownerNode, path, job);
                return valid[path];
            }
        } else {
            int length = nodeList.size();
            if (length == 0) {
                return null;
            } else if (length == 1) {
                classSwitch(ownerNode, 0, job);
                return nodeList.getFirst();
            } else {
                int path = (int) Math.floor(randomEngine.raw() * length);
                classSwitch(ownerNode, path, job);
                return nodeList.get(path);
            }
        }
    }

    @Override
    public NetNode getOutNode(NetNode ownerNode, JobClass jobClass) {
        return null;
    }
}
