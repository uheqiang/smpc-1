package smpc;

import java.util.*;

import smpc.Abstracts.*;

public class Simulator {

	public ArrayList<Node> nodes;
	public Topology topology;
	public Config config;	
	
	public Simulator(Config config) {
		this.config = config;
	}
	

	public void initialize() {
		nodes = new ArrayList<Node>();
		this.topology = new Topology(this.config);

		for (int id = 0; id < config.numberOfnodes; id++) {
			int nodeCluster = topology.getClusterNumber(id);
			int myLayer = getMyLayerNumber(nodeCluster, this.config);
			nodes.add(new Node(id, config, nodes, this.topology, true, false, myLayer));

		}

		// set the subset to be corrupted
		int numberOfCorrputedNodes = config.percentOfCorruptedNodes
				* config.numberOfnodes / 100;
		List<Integer> corruptedNodes = getMRandomNumbersOutOfN(
				numberOfCorrputedNodes, config.numberOfnodes);
		for (Integer corruptedID : corruptedNodes) {
			nodes.get(corruptedID).honest = false;
		}

	}

	public int getMyLayerNumber(int nodeCluster, Config config) {
		for(int i = 1; i < config.numberOfLayersTopology; i++) {
			if( i >= Math.pow(config.nArry, i-1) ) {
				return i;
			}
		}
		return 0;
	}


	public void simulate() {

		int currentTime = 0;
		int endOfCycle = currentTime + this.config.lengthOfRound;

		HashMap<Integer, FailedNode> failedNodes = new HashMap<Integer, FailedNode>();
		
		int roundNumber = 1;
		while (true) {
			//Handle failres and recoveries
			failAndRecover(failedNodes);
			
			//for all the nodes schedule their incoming packets
			for (Node node : nodes) {
				if(!node.failed)
					node.schedlueIncomingPackets(currentTime, endOfCycle);
			}

			// First, pick all the nodes and run the protocol function
			for (Node node : nodes) {
				if(!node.failed)
					node.protocol(currentTime, roundNumber);
			}
			
			//here check for the termination Condition
			if (terminationConditionMet()) {
				//print statistics
				break;
			}
			
			currentTime = endOfCycle;
			endOfCycle = endOfCycle + this.config.lengthOfRound;
			roundNumber++;
			// TODO: insert the condition for the ending and also set the
			// counters
			
			
		}
	}

	
	private void failAndRecover(HashMap<Integer, FailedNode> failedNodes) {
		// find nodes that have failed during this cycle and set them to fail
		ArrayList<FailedNode>newFailedNodes = getFailedNodes(config.numberOfnodes,
				config.failureRate, config.lengthOfRound);
		//Update the failed nodes list
		for(FailedNode failedNode:newFailedNodes) {
			failedNodes.put(failedNode.ID, failedNode);
			nodes.get(failedNode.ID).failed = true;
		}
		
		//recover all the failed nodes that have passed the recovery time
		Iterator<Map.Entry<Integer, FailedNode>> failedNodesIterator= failedNodes.entrySet().iterator();
		while(failedNodesIterator.hasNext()) {
			Map.Entry<Integer, FailedNode> pair = failedNodesIterator.next();
			Integer failedNodeID = pair.getKey();
			FailedNode failedNode = failedNodes.get(failedNodeID);
			if (failedNode.roundsLeft -1 == 0) {
				//recover the Node
				failedNodesIterator.remove();
				this.nodes.get(failedNodeID).failed = false;
			}
			else {
				//reduce the remained time
				failedNode.roundsLeft = failedNode.roundsLeft - 1 ; 
				failedNodes.put(failedNodeID, failedNode);
			}
		}
		
	}
	
	private boolean terminationConditionMet() {
		/*
		 * condition1: At all the times there must be at least one honest party in each cluster.
		 */
		for(int i = 0 ; i< this.config.numberOfClusters(); i++) {
			ArrayList<Integer> clusterMembers = this.topology.getClusterMembers(i);
			boolean hasHonestLiveNode = false;
			System.out.println("cluster Id: " + i);
			System.out.println("members: "+ clusterMembers.toString());
			for(Integer memberID:clusterMembers) {
				Node member = this.nodes.get(memberID);

				if(member.failed == false && member.honest == true) {
					//if there is at least one honest member that has not failed
					hasHonestLiveNode = true;
					System.out.println("good node ID:" + memberID);
					break;
				}
			}
			if (!hasHonestLiveNode) {		
				System.out.println("Termination Condition 1");
				return true;
			}
		}
		
		/*
		 * condition 2
		 * if the nodes in the last level clusters have gathered their inputs
		 */
		
		
		return false;
	}

	public ArrayList<FailedNode> getFailedNodes(int totalNumberOfNodes,
		int failureRate, int timeLengthMinute) {
		ArrayList<FailedNode> results = new ArrayList<>();
		//this is the average of the numbers that should fail in this round
		double failedNumberAvg= (double)totalNumberOfNodes/(double)failureRate * ((double)timeLengthMinute/(double)60) ;		
		
		//here we calculate the exact number of nodes that must fail with a gausian distribution
		//following the previous mean and deviation of 1 
		Random rndGen = new Random();
		double nodeIDToFailDouble = rndGen.nextGaussian()*10 + failedNumberAvg;
		
		
		for(int i = 0 ; i < nodeIDToFailDouble ; i++) {
			int nextRndID = rndGen.nextInt(totalNumberOfNodes);
			results.add(new FailedNode(this.config.recoveryTime, nextRndID));
		}
		return results;
	}

	public List<Integer> getMRandomNumbersOutOfN(int m, int n) {
		ArrayList<Integer> list = new ArrayList<Integer>();
		for (int i = 0; i < n; i++) {
			list.add(new Integer(i));
		}
		Collections.shuffle(list);
		List<Integer> results = list.subList(0, m);
		return results;
	}


	public static void main(String[] args) {
		System.out.println("Simulation Starting ...");
		Config config = new Config();
		Simulator sim = new Simulator(config);
		System.out.println("Simulation Initializing ...");
		sim.initialize();
		System.out.println("Simulating ...");
		sim.simulate();

	}
	
	public class FailedNode{
		int roundsLeft;
		int ID;
		public FailedNode(int roundsLeft, int ID) {
			this.ID = ID;
			this.roundsLeft = roundsLeft;
		}
	}

}

