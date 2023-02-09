package edu.usc.ict.iago.agent;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

import edu.usc.ict.iago.utils.GameSpec;
import edu.usc.ict.iago.utils.History;
import edu.usc.ict.iago.utils.Offer;
import edu.usc.ict.iago.utils.ServletUtils;

public class HillClimbing {
	
	private State init;
	private static State next;
	private IAGOCoreBehavior behavior;
	private History hist;
	private Random random;
	private GameSpec game;
	private Offer alloc;
	private  ArrayList<Integer> playerPref;
	private ArrayList<Integer> vhPref;
	private AgentUtilsExtension utils;
	private boolean isThreatMe;
	private boolean doesLastOfferRejected = false;
	private double toGive = 0; // var from 0 to 1 which tells me if i have to give more or less
	private static List<State> stateList; 
	
	
	public HillClimbing(GameSpec g, History h, Offer allocated, AgentUtilsExtension utils, boolean isThreatMe, boolean lastOffer) {
		this.game = g;
		this.hist = h;
		this.alloc = new Offer(g.getNumberIssues()); //copy of allocated
		for(int issue = 0; issue < game.getNumberIssues(); issue++)
			this.alloc.setItem(issue, allocated.getItem(issue));
		random = new Random();
		this.utils = utils;
		this.isThreatMe = isThreatMe;
		this.doesLastOfferRejected = lastOffer;
		this.stateList = new ArrayList<>();
	}
	
	public Offer algorithm(Offer o) {
		this.init = new State(this.game, this.utils);
		if(o != null)
			this.init.updateState(o);
		else {
			this.algorithm(this.generateOffer());
		}
		if(HillClimbing.stateList.size() >= 5) {
			double maxReward = Double.MIN_VALUE;
			State maxState = null;
			for(State s: HillClimbing.stateList) {
				if(s.getReward() > maxReward) {
					maxReward = s.getReward();
					maxState = s;
				}
			}
			
			for(State s: HillClimbing.stateList) {
				ServletUtils.log("Reward of Offer: " + HillClimbing.stateList.indexOf(s) + "is " + s.getReward(), ServletUtils.DebugLevels.DEBUG);
			}
			return maxState.getOffer();
		}
		
		HillClimbing.stateList.add(this.init);
		ServletUtils.log("From Hill Climbing: The size of the stateList is: " + HillClimbing.stateList.size(), ServletUtils.DebugLevels.DEBUG);
		ServletUtils.log("First Item " + this.init.getOffer().getItem(0)[0] + " " + this.init.getOffer().getItem(0)[1] + " " + this.init.getOffer().getItem(0)[2], ServletUtils.DebugLevels.DEBUG);
		ServletUtils.log("Second Item " + this.init.getOffer().getItem(1)[0] + " " + this.init.getOffer().getItem(1)[1] + " " + this.init.getOffer().getItem(1)[2], ServletUtils.DebugLevels.DEBUG);
		ServletUtils.log("Third Item " + this.init.getOffer().getItem(2)[0] + " " + this.init.getOffer().getItem(2)[1] + " " + this.init.getOffer().getItem(2)[2], ServletUtils.DebugLevels.DEBUG);
		ServletUtils.log("Fourth Item " + this.init.getOffer().getItem(3)[0] + " " + this.init.getOffer().getItem(3)[1] + " " + this.init.getOffer().getItem(3)[2], ServletUtils.DebugLevels.DEBUG);
		/*if(HillClimbing.stateList.size()==2) {
			double rewardCurrent = HillClimbing.stateList.get(0).getReward();
			double reward = HillClimbing.stateList.get(1).getReward();
			ServletUtils.log("RewardCurrent " + rewardCurrent, ServletUtils.DebugLevels.DEBUG);
			ServletUtils.log("Rewarddd: " + reward, ServletUtils.DebugLevels.DEBUG);
		}*/
		
		return this.algorithm(this.generateOffer());
	}
	
	
	public Offer generateOffer() {
		Offer nextOffer = new Offer(game.getNumberIssues());
		for(int issue = 0; issue < game.getNumberIssues(); issue++)
			nextOffer.setItem(issue, this.alloc.getItem(issue));
        int countForAgent, countForUser;
        int whatUserGet, middle, whatAgentGet;
		int userIndex = 2, agentIndex = 0, midIndex = 1;
		int[] proposeArray = new int[3];
		this.playerPref = utils.getMinimaxOrdering();
		this.vhPref = utils.getMyOrdering();
		// Array representing the middle of the board (undecided items)
		int[] free = this.freeIssues();
		int index1 = -1, index2 = -1;
		
		int [] countArray = new int[5];
		
		int userFave = this.userFave(free, playerPref);
		int userSecondBest = this.utils.findAdversaryIdealSecondBest();
		
		
		int vhFave = this.opponentFave(free, vhPref);
		int vhSecondBest = this.findOpponentIdealSecondBest(vhPref);
		
		
		index1 = this.genereateFreeIndex(free);
		index2 = this.genereateFreeIndex(free);
		
		boolean arrayIsFull = false;
		int count = 0;
		
		if(this.doesLastOfferRejected && this.toGive < 1)
			this.toGive+=0.2;
		
		if(!this.doesLastOfferRejected&& !this.isThreatMe && this.toGive >= 0.1)
			this.toGive-=0.1;
		
		if(toGive > 0.7 && this.isThreatMe == false)
			this.isThreatMe = true;
		
		
			if(this.isThreatMe) {
				if(userFave != -1 && free[userFave] > 0) {
					countArray[userFave]++;
				} else if(userSecondBest != -1 &&free[userSecondBest]>0) {
					countArray[userSecondBest]++;
				} else {
					if(index1!=-1 && index2 != -1) {
						countArray[index1]++;
						countArray[index2]++;
					} else if(index1!=-1 && index2 == -1) {
						countArray[index1]++;
					} else if( index2!=-1 && index1 == -1) {
						countArray[index2]++;
					}
					
				}
			} else {  //haven't been threaten by human 
				if(vhFave != -1 && free[vhFave] > 0) {
					countArray[vhFave]++;
				} else if(vhSecondBest != -1 && free[vhSecondBest] > 0) {
					countArray[vhSecondBest]++;
				}
				if (userFave != -1 && free[userFave] > 0) {
					countArray[userFave]++;
				} else if(userSecondBest != -1 && free[userSecondBest] > 0) {
					countArray[userSecondBest]++;
				} 
				
				for(int i = 0; i < countArray.length; i++) {
					if(countArray[i]==1) {
						arrayIsFull = true;
					}
				}
				if(!arrayIsFull) {
						if(index1 != -1) {
							countArray[index1]++;
						} 
						if(index2 != -1) {
							countArray[index2]++;
						}
					}
				}

		
		int fairSplit = 0;
		int remainingOfSplit = 0;
		
		for(int i=0; i<game.getNumberIssues(); i++) {
			if(countArray[i]==1) {
				count = Math.abs(random.nextInt(free[i]) + 1);
				if(count % 2 != 0) {
					fairSplit = (count/2) + 1;
					remainingOfSplit = count - fairSplit;
				} else {
					fairSplit = count/2;
					remainingOfSplit = fairSplit;
				}
				
				if(this.isThreatMe) {
					whatUserGet = this.alloc.getItem(i)[2] + fairSplit;
					middle = free[i] - count;
					free[i]-= count;
					whatAgentGet = this.alloc.getItem(i)[0] + remainingOfSplit;
					proposeArray = this.allocateNewOfferArray(whatAgentGet, middle, whatUserGet);
					nextOffer.setItem(i, proposeArray);
				} else {
					whatUserGet = this.alloc.getItem(i)[2] + remainingOfSplit;
					middle = free[i] - count;
					free[i]-=count;
					whatAgentGet = this.alloc.getItem(i)[0] + fairSplit;
					proposeArray = this.allocateNewOfferArray(whatAgentGet, middle, whatUserGet);
					nextOffer.setItem(i, proposeArray);
				}
				
				fairSplit = 0;
				remainingOfSplit = 0;
					
			}
		}
		return nextOffer;
		
	}
	
	public int genereateFreeIndex(int[] free) {
		int index = -1, i = 0;
		do {
			index = Math.abs(random.nextInt() % this.alloc.getIssueCount());
			i++;
		} while ((free[index] < 1) && i < 30);
		if(i<30)
			return index;
		else 
			return -1;
	}
	
	public int[] allocateNewOfferArray(int whatAgentGet, int middle, int whatUserGet) {
		int[] proposeArr = new int[3];
		int agentIndex = 0, midIndex = 1, userIndex = 2;
		proposeArr[agentIndex] =  whatAgentGet;
		proposeArr[midIndex] =  middle;
		proposeArr[userIndex] = whatUserGet;
		
		return proposeArr;
	}
	
	public int[] freeIssues() {
		// Array representing the middle of the board (undecided items)
			int[] free = new int[game.getNumberIssues()];
				
			for(int issue = 0; issue < game.getNumberIssues(); issue++)
			{
				free[issue] = this.alloc.getItem(issue)[1];
			}
			return free;
	}
	
	public int userFave(int[] free, ArrayList<Integer> playerPref ) {
		// Find most valued issue for player and VH (of the issues that have undeclared items)
		int max = game.getNumberIssues() + 1;
		int userFave = -1;
		for(int i  = 0; i < game.getNumberIssues(); i++)
			if(free[i] > 0 && playerPref.get(i) < max)
				{
				userFave = i;
				max = playerPref.get(i);
				}
		return userFave;
	}
	public int opponentFave(int[] free, ArrayList<Integer> vhPref ) {
		// Find most valued issue for player and VH (of the issues that have undeclared items)
		int max = game.getNumberIssues() + 1;
		int opponentFave = -1;
		for(int i  = 0; i < game.getNumberIssues(); i++)
			if(free[i] > 0 && vhPref.get(i) < max)
				{
				opponentFave = i;
				max = vhPref.get(i);
				}
		return opponentFave;
	}
	
	public int findOpponentIdealSecondBest(ArrayList<Integer> vhPref)
	{
		
		for (int i = 0; i < vhPref.size(); i++)
		{
			if(vhPref.get(i) == 2)
				return i;
		}
		return -1;
	}
		
}