package edu.usc.ict.iago.agent;

import java.lang.annotation.Target;
import java.util.*;

//import org.omg.CosNaming.IstringHelper;

import edu.usc.ict.iago.utils.BehaviorPolicy;
import edu.usc.ict.iago.utils.Event;
import edu.usc.ict.iago.utils.GameSpec;
import edu.usc.ict.iago.utils.History;
import edu.usc.ict.iago.utils.Offer;
import edu.usc.ict.iago.utils.Preference;
import edu.usc.ict.iago.utils.Preference.Relation;
import edu.usc.ict.iago.utils.ServletUtils;	
import edu.usc.ict.iago.agent.State;
import edu.usc.ict.iago.agent.SimulatedAnnealingParams;

public class RepeatedFavorBehavior extends IAGOCoreBehavior implements BehaviorPolicy {
		
	private AgentUtilsExtension utils;
	private GameSpec game;	
	private Offer allocated;
	private Offer concession;
	public static LedgerBehavior lb = LedgerBehavior.NONE;
	private int adverseEvents = 0;
	private float playerLieThreshHold = 0;
	private boolean playerFairness = true;
	private boolean playerThreatMe = false;
	private boolean firstOffer = false;
	private RepeatedFavorMessage message;
	private boolean cashedFavor = false;
	private int currGame;
	public boolean calledToOfferByPref = false;	
    public SimulatedAnnealingParams SAparams;
    private Random random = new Random();
    private ArrayList<Integer> playerPref;
	private ArrayList<Integer> vhPref;
	private HillClimbing hill;
	
	@Override
	public boolean isCalledToOfferByPref() {
		return calledToOfferByPref;
	}
	@Override
	public void setCalledToOfferByPref(boolean calledToOfferByPref) {
		this.calledToOfferByPref = calledToOfferByPref;
	}
	public enum LedgerBehavior {
		FAIR,
		LIMITED,
		BETRAYING,
		NONE;	
	}
	
	

	//Raz and Jonatan 
	
	public void setCashFavor(boolean cashed) {
		this.cashedFavor = cashed;
	}
	public boolean getCashFavor() {
		return this.cashedFavor;
	}
	
	@Override
	public float getPlayerLieThreshHold() {
		return playerLieThreshHold;
	}
	
	@Override
	public void setPlayerLieThreshHold(float playerLieThreshHold) {
		this.playerLieThreshHold = playerLieThreshHold;
	}



	public boolean isPlayerFairness() {
		return playerFairness;
	}
	
	@Override
	public void setIsThreatMe(boolean didHe) {
		this.playerThreatMe = didHe;
	}

	@Override
	public LedgerBehavior getLb() {
		return lb;
	}
	public void setLb(LedgerBehavior lb) {
		this.lb = lb;
	}
	public boolean isPlayerThreatMe() {
		return playerThreatMe;
	}
	public void setPlayerThreatMe(boolean playerThreatMe) {
		this.playerThreatMe = playerThreatMe;
	}
	public boolean isFirstOffer() {
		return firstOffer;
	}
	public void setFirstOffer(boolean firstOffer) {
		this.firstOffer = firstOffer;
	}
	public RepeatedFavorMessage getMessage() {
		return message;
	}
	@Override
	public void setMessage(IAGOCoreMessage message) {
		this.message = (RepeatedFavorMessage) message;
	}
	public boolean isCashedFavor() {
		return cashedFavor;
	}
	public void setCashedFavor(boolean cashedFavor) {
		this.cashedFavor = cashedFavor;
	}
	public int getCurrGame() {
		return currGame;
	}
	public void setPlayerFairness(boolean playerFairness) {
		this.playerFairness = playerFairness;
	}



	public LedgerBehavior getLedgerStatus() {
		if(this.lb == LedgerBehavior.FAIR) {
			return LedgerBehavior.FAIR;
		}
		else if (this.lb == LedgerBehavior.LIMITED) {
			return LedgerBehavior.LIMITED;
		}
		else if (this.lb == LedgerBehavior.BETRAYING) {
			return LedgerBehavior.BETRAYING;
		}
		else
			return LedgerBehavior.NONE;
	}
	
	
	public void setLedgerBehavior(String behave) {
		switch(behave)
		{
		case "fair":
			this.lb = LedgerBehavior.FAIR;
		case "limited":
			this.lb = LedgerBehavior.LIMITED;
		case "beraying":
			this.lb = LedgerBehavior.BETRAYING;
		case "none":
			this.lb = LedgerBehavior.NONE;
		default:
			this.lb = LedgerBehavior.NONE;
		}		
	}
	
	public RepeatedFavorBehavior(LedgerBehavior lb)
	{
		super();
		this.lb = lb;		
        this.SAparams = new SimulatedAnnealingParams(1.0,0.001,0.999,2);
        
	}
		
	@Override
	protected void setUtils(AgentUtilsExtension utils)
	{
		this.utils = utils;
		
		this.game = this.utils.getSpec();
		allocated = new Offer(game.getNumberIssues());
		for(int i = 0; i < game.getNumberIssues(); i++)
		{
			int[] init = {0, game.getIssueQuantities().get(i), 0};
			allocated.setItem(i, init);
		}
		concession = new Offer(game.getNumberIssues());
		for(int i = 0; i < game.getNumberIssues(); i++)
		{
			int[] init = {0, game.getIssueQuantities().get(i), 0};
			concession.setItem(i, init);
		}
		this.setLedgerBehavior(this.currGame);
	}
	
	@Override
	protected void updateAllocated (Offer update)
	{
		allocated = update;
	}
	
	@Override
	protected void updateAdverseEvents (int change)
	{
		adverseEvents = Math.max(0, adverseEvents + change);
	}
	
	@Override
	protected Offer getAllocated ()
	{
		return allocated;
	}
	
	@Override
	protected Offer getConceded ()
	{
		return concession;
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
	
	public int[] freeIssues() {
		// Array representing the middle of the board (undecided items)
		int[] free = new int[game.getNumberIssues()];
		for(int issue = 0; issue < game.getNumberIssues(); issue++)
		{
			free[issue] = allocated.getItem(issue)[1];
		}
		return free;
	}
	
	public int genereateFreeIndex(int[] free) {
		int index = -1, i = 0;
		do {
			index = Math.abs(random.nextInt() % this.allocated.getIssueCount());
			i++;
		} while ((free[index] < 1) && i < 30);
		if(i<30)
			return index;
		else 
			return -1;
	}
	
	public Offer generateOffer(Offer baseOffer) {
		Offer nextOffer = new Offer(game.getNumberIssues());
        int countForAgent, countForUser;
        int whatUserGet, middle, whatAgentGet;
		int userIndex = 2, agentIndex = 0, midIndex = 1;
		int[] proposeArray = new int[3];
		this.playerPref = utils.getMinimaxOrdering();
		this.vhPref = utils.getMyOrdering();
		for(int issue = 0; issue < game.getNumberIssues(); issue++)
			nextOffer.setItem(issue, baseOffer.getItem(issue));
		// Array representing the middle of the board (undecided items)
		int[] free = new int [4];
		for(int i = 0; i < 4; i++) { 
			free[i] = baseOffer.getItem(i)[1];
		}
		
		
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
		
		
		
			if(this.playerThreatMe) {
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
				if(vhFave != -1 &&free[vhFave] > 0) {
					countArray[vhFave]++;
				} else if(vhSecondBest != -1 &&free[vhSecondBest] > 0) {
					countArray[vhSecondBest]++;
				}
				if (userFave != -1 && free[userFave] > 0) {
					countArray[userFave]++;
				} else if(userSecondBest != -1 &&free[userSecondBest] > 0) {
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
				
				if(this.playerThreatMe) {
					whatUserGet = baseOffer.getItem(i)[2] + fairSplit;
					middle = free[i] - count;
					free[i]-=count;
					whatAgentGet = baseOffer.getItem(i)[0] + remainingOfSplit;
					proposeArray = this.allocateNewOfferArray(whatAgentGet, middle, whatUserGet);
					nextOffer.setItem(i, proposeArray);
				} else {
					whatUserGet = baseOffer.getItem(i)[2] + remainingOfSplit;
					middle = free[i] - count;
					free[i]-=count;
					whatAgentGet = baseOffer.getItem(i)[0] + fairSplit;
					proposeArray = this.allocateNewOfferArray(whatAgentGet, middle, whatUserGet);
					nextOffer.setItem(i, proposeArray);
				}
				
				fairSplit = 0;
				remainingOfSplit = 0;
					
			}
		}
		return nextOffer;
		
	}

	protected ArrayList<Offer> SimulatedAnnealing(double threshold, double time, Offer baseOffer) {
		ServletUtils.log("I'm in Simulated Annealing", ServletUtils.DebugLevels.DEBUG);
		ArrayList<Offer> targetOffers = new ArrayList<>(); //optimal utility
        State current = new State(baseOffer, utils);
        double currentOfferUtil = current.getReward();
		ServletUtils.log("Current Util " + currentOfferUtil, ServletUtils.DebugLevels.DEBUG);
        double newEnergy, curEnergy, p;
        double curTemp = SAparams.getStartTemprature();
        double targetOfferUtil = 0.0;
        double nextOfferUtil = 0.0;
        Offer nextOffer = null;
        Offer maxOffer = null;
        double maxOfferUtil = 0.0;
        List<String> issuesNames = game.getIssueSingularText();
        List<Integer> issuesQuantities = game.getIssueQuantities();
        int counter = 0;
        int counterRegular = 0;
		
        while (curTemp > SAparams.getEndTemprature()) {
        	
            for (int i = 0; i < SAparams.getNumberOfSteps(); i++) {
            	//Choose Random Offer
            	nextOffer = this.generateOffer(baseOffer);
        		State nextState = new State(nextOffer, utils);
        		nextOfferUtil = nextState.getReward();
                if (maxOffer == null || nextOfferUtil >= maxOfferUtil) {
            		//ServletUtils.log("I'm in Simulated Annealing--->Changing max offer", ServletUtils.DebugLevels.DEBUG);
                    maxOffer = nextOffer;
                    maxOfferUtil = nextOfferUtil;
                }
            }
            
            newEnergy = Math.abs(threshold - maxOfferUtil);
    		//ServletUtils.log("I'm in Simulated Annealing--->New Energy" + newEnergy, ServletUtils.DebugLevels.DEBUG);
            curEnergy = Math.abs(threshold - currentOfferUtil);
    		//ServletUtils.log("I'm in Simulated Annealing--->Current Energy" + curEnergy, ServletUtils.DebugLevels.DEBUG);
            p = Math.exp(-Math.abs(newEnergy - curEnergy) / curTemp);

            if (newEnergy < curEnergy || p > random.nextDouble()) {
                //baseOffer = maxOffer;
            	baseOffer = new Offer(game.getNumberIssues());
        		for(int issue = 0; issue < game.getNumberIssues(); issue++)
        			baseOffer.setItem(issue, maxOffer.getItem(issue));
                currentOfferUtil = maxOfferUtil;
        		//ServletUtils.log("I'm in Simulated Annealing--->Changing Current offer", ServletUtils.DebugLevels.DEBUG);
            }
            //double minOfferUtilInList = findMinOffer(targetOffers);
            if(targetOffers.size() < 5) {
            	if(!offerAlreadyInList(baseOffer, targetOffers)) {
                	targetOffers.add(baseOffer);
            	}
            } 
            else {
            	for(Offer o : targetOffers) {
                	ServletUtils.log("Reward of target Offers: " + new State(o, this.utils).getReward(), ServletUtils.DebugLevels.DEBUG);
                	return targetOffers;
            	}
            }
            curTemp = curTemp * SAparams.getCool();
            //Debug Prints
            /*if(maxOffer.getItem(0)[0] > 5 || maxOffer.getItem(1)[0] > 5 ||maxOffer.getItem(2)[0] > 5 ||maxOffer.getItem(3)[0] > 5) {
            	//ServletUtils.log("The maxOffer is invalid------> iteration " + counter, ServletUtils.DebugLevels.DEBUG);
                ServletUtils.log("First Item " + maxOffer.getItem(0)[0] + " " + maxOffer.getItem(0)[1] + " " + maxOffer.getItem(0)[2], ServletUtils.DebugLevels.DEBUG);
        		ServletUtils.log("Second Item " + maxOffer.getItem(1)[0] + " " + maxOffer.getItem(1)[1] + " " + maxOffer.getItem(1)[2], ServletUtils.DebugLevels.DEBUG);
        		ServletUtils.log("Third Item " + maxOffer.getItem(2)[0] + " " + maxOffer.getItem(2)[1] + " " + maxOffer.getItem(2)[2], ServletUtils.DebugLevels.DEBUG);
        		ServletUtils.log("Fourth Item " + maxOffer.getItem(3)[0] + " " + maxOffer.getItem(3)[1] + " " + maxOffer.getItem(3)[2], ServletUtils.DebugLevels.DEBUG);
                counter++;
            }*/
            counterRegular++;
        }
    	ServletUtils.log("Counter : " + counter, ServletUtils.DebugLevels.DEBUG);
    	ServletUtils.log("Counter Regular: " + counterRegular, ServletUtils.DebugLevels.DEBUG);
    	
    	ServletUtils.log("The length of the targetList is " + targetOffers.size(), ServletUtils.DebugLevels.DEBUG);
    	ServletUtils.log("The maxOffer was invalid for ------> iteration " + counter, ServletUtils.DebugLevels.DEBUG);
    	ServletUtils.log("The while was running for ------> iteration " + counterRegular, ServletUtils.DebugLevels.DEBUG);
    	for(Offer o : targetOffers) {
        	ServletUtils.log("Reward of target Offers: " + new State(o, this.utils).getReward(), ServletUtils.DebugLevels.DEBUG);
    	}
    	
        return targetOffers;
	}
	
	private double findMinOffer(ArrayList<Offer> targetOffers) {
		double min = Double.MAX_VALUE;
    	Offer minOffer = null;
    	for(Offer o :targetOffers) {
    		double reward = new State(o, utils).getReward();
    		if(min > reward) {
    			min = reward;
    			minOffer = o;
    		}
    	}
		return min;
	}
	
	private boolean offerAlreadyInList(Offer o, ArrayList<Offer> targetOffers) {
		for(Offer offer: targetOffers) {
			if(compareOffers(offer,o)) {
				return true;
			}
		}
		return false;
	}
	private boolean compareOffers(Offer o1, Offer o2) {
		for(int i = 0; i < this.game.getNumberIssues(); i++) {
			int[] arrOffer1 = o1.getItem(i);
			int[] arrOffer2 = o2.getItem(i);
			if(!Arrays.equals(arrOffer1, arrOffer2)) {
				return false;
			}
		}
		return true;
	}
	
	@Override
	protected Offer getFavorOffer() {
		Offer propose = new Offer(this.allocated.getIssueCount());
		for(int issue = 0; issue < game.getNumberIssues(); issue++)
			propose.setItem(issue, allocated.getItem(issue));
		
		int whatUserGet, middle, whatAgentGet;
		int index = -1;
		int[] proposeArray = new int[3];
		int[]free = this.freeIssues();
		
		ArrayList<Integer> vhPref = utils.getMyOrdering();
		ArrayList<Integer> playerPref = utils.getMinimaxOrdering();
		int opponentFave = this.opponentFave(free, vhPref);
		int opponentSecondBest = this.findOpponentIdealSecondBest(vhPref);
		
		int userFave = this.utils.findAdversaryIdealBest();
		int userSecondBest = this.utils.findAdversaryIdealSecondBest();
		
		if(this.utils.getVerbalLedger() < 0) {
			utils.modifyOfferLedger(-1);
			if(opponentFave != -1 && free[opponentFave] >= 2) {
				propose.setItem(opponentFave, new int[] {this.allocated.getItem(opponentFave)[0] + 2, free[opponentFave] - 2, this.allocated.getItem(opponentFave)[2]});
				 ServletUtils.log("I'm in getFAVOR ----> FAVOR_RUTERN 1 ", ServletUtils.DebugLevels.DEBUG);
			} else if(opponentSecondBest != -1 && free[opponentSecondBest] >= 2) {
				propose.setItem(opponentSecondBest, new int[] {this.allocated.getItem(opponentSecondBest)[0] + 2, free[opponentSecondBest] - 2, this.allocated.getItem(opponentSecondBest)[2]});
				 ServletUtils.log("I'm in getFAVOR ----> FAVOR_RUTERN 2 ", ServletUtils.DebugLevels.DEBUG);
			} else {
				LinkedList<Integer> freeIndex = new LinkedList<>();
				 ServletUtils.log("I'm in getFAVOR ----> FAVOR_RUTERN 3 ", ServletUtils.DebugLevels.DEBUG);
				for (int i = 0; i < this.allocated.getIssueCount(); i++) {
					if (free[i] > 1)
						freeIndex.add(i);
				}
				if(freeIndex.size() > 0)
				{
					int tmp = freeIndex.get(0);
					propose.setItem(tmp, new int[]{this.allocated.getItem(tmp)[0] + 2, free[tmp] - 1, this.allocated.getItem(tmp)[2]});
				}
				
			}
			return propose;
		}
		
		if(userFave != -1) {
			if(free[userFave] >= 2) {
				whatUserGet = this.allocated.getItem(userFave)[2] + 2;
				middle = free[userFave] - 2;
				free[userFave]-=2;
				whatAgentGet = this.allocated.getItem(userFave)[0];
				proposeArray = this.allocateNewOfferArray(whatAgentGet, middle, whatUserGet);
				index = userFave;
				propose.setItem(index, proposeArray);
				
				//
				
				if(free[opponentFave] > 0 && opponentFave != -1 && opponentFave != userFave) {
					whatUserGet = this.allocated.getItem(opponentFave)[2];
					middle = free[opponentFave] - 1;
					free[opponentFave]-=1;
					whatAgentGet = this.allocated.getItem(opponentFave)[0] + 1;
					proposeArray = this.allocateNewOfferArray(whatAgentGet, middle, whatUserGet);
					index = opponentFave;
					String next = "How's that as a favor?";
					this.message.setNextMessage(next);
					propose.setItem(index, proposeArray);
				} else if(free[opponentSecondBest] > 0 && opponentSecondBest != -1 && opponentSecondBest != userFave) {
					whatUserGet = this.allocated.getItem(opponentSecondBest)[2];
					middle = free[opponentSecondBest] - 1;
					free[opponentSecondBest]-=1;
					whatAgentGet = this.allocated.getItem(opponentSecondBest)[0] + 1;
					proposeArray = this.allocateNewOfferArray(whatAgentGet, middle, whatUserGet);
					index = opponentSecondBest;
					String next = "How's that as a favor?";
					this.message.setNextMessage(next);
					propose.setItem(index, proposeArray);
				} else {
					LinkedList<Integer> freeIndex = new LinkedList<>();
					for (int i = 0; i < this.allocated.getIssueCount(); i++) {
						if (free[i] > 0)
							freeIndex.add(i);
					}
					if(freeIndex.size() > 0)
					{
						int tmp = freeIndex.get(0);
						propose.setItem(tmp, new int[]{this.allocated.getItem(tmp)[0] + 1, free[tmp] - 1, this.allocated.getItem(tmp)[2]});
					}
					return propose;
				}
			} else if(free[userFave] > 0) {
				whatUserGet = this.allocated.getItem(userFave)[2] + 1;
				middle = free[userFave] - 1;
				free[userFave]-=1;
				whatAgentGet = this.allocated.getItem(userFave)[0];
				proposeArray = this.allocateNewOfferArray(whatAgentGet, middle, whatUserGet);
				index = userFave;
				String next = "How's that as a favor?";
				this.message.setNextMessage(next);
				propose.setItem(index, proposeArray);
			} else if(free[userSecondBest] > 0 && userSecondBest!= -1) {
				whatUserGet = this.allocated.getItem(userSecondBest)[2] + 1;
				middle = free[userSecondBest] - 1;
				free[userSecondBest]-=1;
				whatAgentGet = this.allocated.getItem(userSecondBest)[0];
				proposeArray = this.allocateNewOfferArray(whatAgentGet, middle, whatUserGet);
				index = userSecondBest;
				String next = "How's that as a favor?";
				this.message.setNextMessage(next);
				propose.setItem(index, proposeArray);
			}
			
		} else {
			String next = "I'm sorry, I can't find enything for you ";
			this.message.setNextMessage(next);
			return null;
		}
		
		this.concession = propose;
		return propose;
	}
	
	@Override
	protected Offer getFinalOffer(History history)
	{
		Offer propose = new Offer(this.allocated.getIssueCount());
		for(int issue = 0; issue < game.getNumberIssues(); issue++)
			propose.setItem(issue, allocated.getItem(issue));
		
		boolean isTotalEven = false;
		boolean shouldReturnOneToUser = false;
		
		ServletUtils.log("FINAL OFFER I'M IN!!!" , ServletUtils.DebugLevels.DEBUG);
		
		int[] free = this.freeIssues();
		ArrayList<Integer> vhPref = utils.getMyOrdering();
		ArrayList<Integer> playerPref = utils.getMinimaxOrdering(); 
		
		int userBest = this.utils.findAdversaryIdealBest();
		int userSecondBest = this.utils.findAdversaryIdealSecondBest();
		
		ServletUtils.log("I have user best and user second best " + userBest + " " + userSecondBest , ServletUtils.DebugLevels.DEBUG);
		
		int userFave = this.userFave(free, playerPref);
		
		ServletUtils.log("USER FAVE " + userFave , ServletUtils.DebugLevels.DEBUG);
		
		int opponentFave = this.opponentFave(free, vhPref);
		int opponentSecondBest = this.findOpponentIdealSecondBest(vhPref);
		
		ServletUtils.log("I have OPPENENT FAVES " + opponentFave + " " + opponentSecondBest , ServletUtils.DebugLevels.DEBUG);
		
		//int margin = this.getRealMargin();
		int whatUserGet, middle, whatAgentGet;
		int userIndex = 2, agentIndex = 0, midIndex = 1;
		int[] proposeArray = new int[3];
		
		int numberOfItems = 0;
		for(int i = 0; i < free.length; i++) {
			
				numberOfItems+=free[i];
		}
		
		if(numberOfItems % 2 == 0)
			isTotalEven = true;
		// else isTotalEven = false
		
		ServletUtils.log("numberOfItems " + numberOfItems , ServletUtils.DebugLevels.DEBUG);
		
		for(int i = 0; i < this.allocated.getIssueCount(); i++) {
			
			if(free[i] % 2 == 0) {
				if(!shouldReturnOneToUser) {
					whatUserGet = free[i] / 2 + this.allocated.getItem(i)[2];
					whatAgentGet = free[i] / 2 + this.allocated.getItem(i)[0];
					middle = 0;
					free[i]-=free[i];
					proposeArray = this.allocateNewOfferArray(whatAgentGet, middle, whatUserGet);
					propose.setItem(i, proposeArray);
				} else {
					whatUserGet = free[i] / 2 + 1 + this.allocated.getItem(i)[2];
					whatAgentGet = free[i] / 2 - 1 + this.allocated.getItem(i)[0];
					middle = 0;
					free[i]-=free[i];
					proposeArray = this.allocateNewOfferArray(whatAgentGet, middle, whatUserGet);
					propose.setItem(i, proposeArray);
					shouldReturnOneToUser = false;
				}
				
				
				
			} else {
				if(opponentFave != -1 && opponentFave == i) {
					whatUserGet = (free[i] / 2) + this.allocated.getItem(i)[2];
					whatAgentGet = (free[i] / 2 + 1) + this.allocated.getItem(i)[0];
					middle = 0;
					free[i]-=free[i];
					proposeArray = this.allocateNewOfferArray(whatAgentGet, middle, whatUserGet);
					propose.setItem(i, proposeArray);
					shouldReturnOneToUser = true;
				} else if(opponentSecondBest != -1 && opponentSecondBest == i && !shouldReturnOneToUser) {
					whatUserGet = (free[i] / 2) + this.allocated.getItem(i)[2];
					whatAgentGet = (free[i] / 2 + 1) + this.allocated.getItem(i)[0];
					middle = 0;
					free[i]-=free[i];
					proposeArray = this.allocateNewOfferArray(whatAgentGet, middle, whatUserGet);
					propose.setItem(i, proposeArray);
					shouldReturnOneToUser = true;
				} else {
					if(!shouldReturnOneToUser) {
						whatUserGet = (free[i] / 2) + this.allocated.getItem(i)[2];
						whatAgentGet = (free[i] / 2 + 1) + this.allocated.getItem(i)[0];
						middle = 0;
						free[i]-=free[i];
						proposeArray = this.allocateNewOfferArray(whatAgentGet, middle, whatUserGet);
						propose.setItem(i, proposeArray);
						shouldReturnOneToUser = true;
					} else {
						whatUserGet = (free[i] / 2) + 1 + this.allocated.getItem(i)[2];
						whatAgentGet = free[i] / 2 + this.allocated.getItem(i)[0];
						middle = 0;
						free[i]-=free[i];
						proposeArray = this.allocateNewOfferArray(whatAgentGet, middle, whatUserGet);
						propose.setItem(i, proposeArray);
						shouldReturnOneToUser = false;
					}
					
				}
				
			}
	     }
		
		
		this.concession = propose;
		this.message.setNextMessage("The game is almost done, and I think this offer would benefit both of us!");
		ServletUtils.log("FINAL OFFER " + propose , ServletUtils.DebugLevels.DEBUG);
		return propose;
	}
	
	public int[] allocateNewOfferArray(int whatAgentGet, int middle, int whatUserGet) {
		int[] proposeArr = new int[3];
		int agentIndex = 0, midIndex = 1, userIndex = 2;
		proposeArr[agentIndex] =  whatAgentGet;
		proposeArr[midIndex] =  middle;
		proposeArr[userIndex] = whatUserGet;
		
		return proposeArr;
	}

	@Override
	public Offer getNextOffer(History history) 
	{	
		//start from where we currently have accepted
		Offer propose = new Offer(game.getNumberIssues());
		for(int issue = 0; issue < game.getNumberIssues(); issue++)
			propose.setItem(issue, allocated.getItem(issue));
		
		
		// Assign ordering to the player based on perceived preferences. Ideally, they would be opposite the agent's (integrative)
		ArrayList<Integer> playerPref = utils.getMinimaxOrdering(); 
		ArrayList<Integer> vhPref = utils.getMyOrdering();
		
		// Array representing the middle of the board (undecided items)
		int[] free = new int[game.getNumberIssues()];
		
		for(int issue = 0; issue < game.getNumberIssues(); issue++)
		{
			free[issue] = allocated.getItem(issue)[1];
		}
	
		int userFave = -1;
		int opponentFave = -1;
		
		// Find most valued issue for player and VH (of the issues that have undeclared items)
		int max = game.getNumberIssues() + 1;
		for(int i  = 0; i < game.getNumberIssues(); i++) {
			if(free[i] > 0 && playerPref.get(i) < max)
			{
				userFave = i;
				max = playerPref.get(i);
			}
		}
		max = game.getNumberIssues() + 1;
		for(int i  = 0; i < game.getNumberIssues(); i++) {
			if(free[i] > 0 && vhPref.get(i) < max)
			{
				opponentFave = i;
				max = vhPref.get(i);
			}
		}
		
		
		//is there ledger to work with?
		if(lb == LedgerBehavior.NONE) //this agent doesn't care
		{
			//nothing
		}
		else if (utils.getVerbalLedger() < 0) //we have favors to cash!
		{
			//we will naively cash them immediately regardless of game importance
			//take entire category
			utils.modifyOfferLedger(-1);
			propose.setItem(opponentFave, new int[] {allocated.getItem(opponentFave)[0] + free[opponentFave], 0, allocated.getItem(opponentFave)[2]});
			ServletUtils.log("get favor", ServletUtils.DebugLevels.DEBUG);
			return propose;	
		}
		else if (utils.getVerbalLedger() > 0) //we have favors to return!
		{
			if (lb == LedgerBehavior.BETRAYING)//this agent doesn't care
			{
				//nothing, so continue
			}
			else if(lb == LedgerBehavior.FAIR)//this agent returns an entire column!
			{
				//return entire category
				utils.modifyOfferLedger(1);
				propose.setItem(userFave, new int[] {allocated.getItem(userFave)[0], 0, allocated.getItem(userFave)[2] + free[userFave]});
				ServletUtils.log("return favor", ServletUtils.DebugLevels.DEBUG);
				return propose;
			}
			else //if (lb == LedgerBehavior.LIMITED)//this agent returns a single item.  woo hoo
			{
				//return single item
				utils.modifyOfferLedger(1);
				propose.setItem(userFave, new int[] {allocated.getItem(userFave)[0], free[userFave] - 1, allocated.getItem(userFave)[2] + 1});
				ServletUtils.log("return single item", ServletUtils.DebugLevels.DEBUG);
				ServletUtils.log("return single item", ServletUtils.DebugLevels.DEBUG);
				return propose;
			}
		}
		else //we have nothing special
		{
			//nothing, so continue
		}

		

		if (userFave == -1 && opponentFave == -1) // We already have a full offer (no undecided items), try something different
		{
			//just repeat and keep allocated
		}			
		else if(userFave == opponentFave)// Both agent and player want the same issue most
		{
			if(free[userFave] >= 2) // If there are more than two of that issue, propose an offer where the VH and player each get one more of that issue
				propose.setItem(userFave, new int[] {allocated.getItem(userFave)[0] + 1, free[userFave] - 2, allocated.getItem(userFave)[2] + 1});
			else // Otherwise just give the one item left to us, the agent
			{
				if (utils.adversaryRow == 0) {
					propose.setItem(userFave, new int[] {allocated.getItem(userFave)[0], free[userFave] - 1, allocated.getItem(userFave)[2] + 1});
				} else if (utils.adversaryRow == 2) {
					propose.setItem(userFave, new int[] {allocated.getItem(userFave)[0] + 1, free[userFave] - 1, allocated.getItem(userFave)[2]});
				}
			}
		}
		else // If the agent and player have different top picks
		{
			// Give both the VH and the player one more of the item they want most
			propose.setItem(userFave, new int[] {allocated.getItem(userFave)[0], free[userFave] - 1, allocated.getItem(userFave)[2] + 1});
			propose.setItem(opponentFave, new int[] {allocated.getItem(opponentFave)[0] + 1, free[opponentFave] - 1, allocated.getItem(opponentFave)[2]});
		}
		ServletUtils.log("return two propose", ServletUtils.DebugLevels.DEBUG);
		return propose;
	}
	
	//Yonatan and Raz new getNextOffer
	
	@Override
	public Offer myNextOffer(History history) {

		Offer propose = new Offer(game.getNumberIssues());
		for(int issue = 0; issue < game.getNumberIssues(); issue++)
			propose.setItem(issue, allocated.getItem(issue));
		ServletUtils.log("I'm in myNextOffer", ServletUtils.DebugLevels.DEBUG);
		
		// Assign ordering to the player based on perceived preferences. Ideally, they would be opposite the agent's (integrative)
		ArrayList<Integer> playerPref = utils.getMinimaxOrdering(); 
		ArrayList<Integer> vhPref = utils.getMyOrdering();
		
		this.setLedgerBehavior(this.currGame);
		int whatUserGet, middle, whatAgentGet;
		int userIndex = 2, agentIndex = 0, midIndex = 1;
		Random r = new Random();
		int[] proposeArray = new int[3];
		int[] free = this.freeIssues();
		int totalFree = 0;
		for(int i =0; i < this.allocated.getIssueCount(); i++) {
			totalFree+= free[i];
		}
		
		//Checking Simulated Annealing
		ServletUtils.log("Executing Simulated Annealing", ServletUtils.DebugLevels.DEBUG);
		ArrayList<Offer> list =  this.SimulatedAnnealing(12, 0, this.allocated);
		Offer o = list.get(list.size()-1);
		ServletUtils.log("Result from Simulated Annealing was in size: " + list.size(), ServletUtils.DebugLevels.DEBUG);
		
		//Checking Hill Climbing
//		HillClimbing hill = new HillClimbing(game, history, propose, utils, playerThreatMe, firstOffer);
//		Offer o = hill.algorithm(propose);
//		ServletUtils.log("Result from hill climbing", ServletUtils.DebugLevels.DEBUG);
//		ServletUtils.log("First Item " + o.getItem(0)[0] + " " + o.getItem(0)[1] + " " + o.getItem(0)[2], ServletUtils.DebugLevels.DEBUG);
//		ServletUtils.log("Second Item " + o.getItem(1)[0] + " " + o.getItem(1)[1] + " " + o.getItem(1)[2], ServletUtils.DebugLevels.DEBUG);
//		ServletUtils.log("Third Item " + o.getItem(2)[0] + " " + o.getItem(2)[1] + " " + o.getItem(2)[2], ServletUtils.DebugLevels.DEBUG);
//		ServletUtils.log("Fourth Item " + o.getItem(3)[0] + " " + o.getItem(3)[1] + " " + o.getItem(3)[2], ServletUtils.DebugLevels.DEBUG);
//		State s = new State(o, utils);
//		double rewardS = s.getReward();
//		ServletUtils.log("This is the reward for this offer" + s.getReward(), ServletUtils.DebugLevels.DEBUG);
		return o;
		
		
//		int userBest = this.utils.findAdversaryIdealBest();
//		int userSecondBest = this.utils.findAdversaryIdealSecondBest();
//		
//		int userFave = this.userFave(free, playerPref);
//		
//		int opponentFave = this.opponentFave(free, vhPref);
//		int opponentSecondBest = this.findOpponentIdealSecondBest(vhPref);
//		if(this.utils.getVerbalLedger() < 0 && (this.getRealMargin() >= 4 || this.currGame == 3)){
//			utils.modifyOfferLedger(-1);
//			if(opponentFave != -1 && free[opponentFave] >= 2) {
//				propose.setItem(opponentFave, new int[] {this.allocated.getItem(opponentFave)[0] + 2, free[opponentFave] - 2, this.allocated.getItem(opponentFave)[2]});
//				
//			} else if(opponentSecondBest != -1 && free[opponentSecondBest] >= 2) {
//				propose.setItem(opponentSecondBest, new int[] {this.allocated.getItem(opponentSecondBest)[0] + 2, free[opponentSecondBest] - 2, this.allocated.getItem(opponentSecondBest)[2]});
//			} else {
//				LinkedList<Integer> freeIndex = new LinkedList<>();
//				for (int i = 0; i < this.allocated.getIssueCount(); i++) {
//					if (free[i] > 1)
//						freeIndex.add(i);
//				}
//				if(freeIndex.size() > 0)
//				{
//					int tmp = freeIndex.get(0);
//					propose.setItem(tmp, new int[]{this.allocated.getItem(tmp)[0] + 2, free[tmp] - 1, this.allocated.getItem(tmp)[2]});
//				}
//				return propose;
//			}
//		} 
//		
//		 {
//				/**
//				 * check if player lied
//				 * check if player threaten me 
//				 * check if isFixedPie
//				 * check if player is fair
//				 */
//				switch (this.lb) {
//				case FAIR: {
//					if(totalFree>10) {
//						if(this.playerThreatMe && currGame < 3) {
//							int index = -1;
//							this.playerThreatMe = false;
//							if(userFave != -1) {
//								if(free[userFave] >= 2) {
//									whatUserGet = this.allocated.getItem(userFave)[2] + 2;
//									middle = free[userFave] - 2;
//									free[userFave]-=2;
//									whatAgentGet = this.allocated.getItem(userFave)[0];
//									proposeArray = this.allocateNewOfferArray(whatAgentGet, middle, whatUserGet);
//									index = userFave;
//									ServletUtils.log("I'm in myNextOffer --> 1", ServletUtils.DebugLevels.DEBUG);
//									propose.setItem(index, proposeArray);
//									
//									//
//									
//									if(free[opponentFave] > 0 && opponentFave != userFave && opponentFave != -1) {
//										whatUserGet = this.allocated.getItem(opponentFave)[2] + 1;
//										middle = free[opponentFave] - 1;
//										free[opponentFave]-=1;
//										whatAgentGet = this.allocated.getItem(opponentFave)[0];
//										proposeArray = this.allocateNewOfferArray(whatAgentGet, middle, whatUserGet);
//										index = opponentFave;
//										ServletUtils.log("I'm in myNextOffer --> 2", ServletUtils.DebugLevels.DEBUG);
//										propose.setItem(index, proposeArray);
//									} else if(free[opponentSecondBest] > 0 && opponentSecondBest!= -1 ) {
//										whatUserGet = this.allocated.getItem(opponentSecondBest)[2] + 1;
//										middle = free[opponentSecondBest] - 1;
//										free[opponentSecondBest]-=1;
//										whatAgentGet = this.allocated.getItem(opponentSecondBest)[0];
//										proposeArray = this.allocateNewOfferArray(whatAgentGet, middle, whatUserGet);
//										index = opponentSecondBest;
//										ServletUtils.log("I'm in myNextOffer --> 3", ServletUtils.DebugLevels.DEBUG);
//										propose.setItem(index, proposeArray);
//									} else {
//										int rand = -1, i = 0;;
//										do {
//											rand = Math.abs(r.nextInt() % this.allocated.getIssueCount());
//											i++;
//										} while ((free[rand] < 1 || rand == userFave) && i < 20);
//										whatUserGet = this.allocated.getItem(rand)[2] + 1;
//										middle = free[rand] - 1;
//										free[rand]-=1;
//										whatAgentGet = this.allocated.getItem(rand)[0];
//										proposeArray = this.allocateNewOfferArray(whatAgentGet, middle, whatUserGet);
//										index = rand;
//										ServletUtils.log("I'm in myNextOffer --> 3.5", ServletUtils.DebugLevels.DEBUG);
//										propose.setItem(index, proposeArray);
//									}
//								
//								} else if(free[userFave] > 0) {
//									whatUserGet = this.allocated.getItem(userFave)[2] + 1;
//									middle = free[userFave] - 1;
//									free[userFave]-=1;
//									whatAgentGet = this.allocated.getItem(userFave)[0];
//									proposeArray = this.allocateNewOfferArray(whatAgentGet, middle, whatUserGet);
//									index = userFave;
//									ServletUtils.log("I'm in myNextOffer --> 4", ServletUtils.DebugLevels.DEBUG);
//									propose.setItem(index, proposeArray);
//									
//								} else if(free[userSecondBest] > 0 && userSecondBest != -1) {
//									whatUserGet = this.allocated.getItem(userSecondBest)[2] + 1;
//									middle = free[userSecondBest] - 1;
//									free[userSecondBest]-=1;
//									whatAgentGet = this.allocated.getItem(userSecondBest)[0];
//									proposeArray = this.allocateNewOfferArray(whatAgentGet, middle, whatUserGet);
//									index = userSecondBest;
//									ServletUtils.log("I'm in myNextOffer --> 5", ServletUtils.DebugLevels.DEBUG);
//									propose.setItem(index, proposeArray);
//									
//								} else {
//									int rand = -1, i = 0;;
//									do {
//										rand = Math.abs(r.nextInt() % this.allocated.getIssueCount() );
//										i++;
//									} while ((free[rand] < 1 || rand == userFave || rand == userSecondBest) && i < 20);
//									whatUserGet = this.allocated.getItem(rand)[2] + 1;
//									middle = free[rand] - 1;
//									free[rand]-=1;
//									whatAgentGet = this.allocated.getItem(rand)[0];
//									proposeArray = this.allocateNewOfferArray(whatAgentGet, middle, whatUserGet);
//									index = rand;
//									ServletUtils.log("I'm in myNextOffer --> 3.5", ServletUtils.DebugLevels.DEBUG);
//									propose.setItem(index, proposeArray);
//									
//								}
//								
//							} else if(userSecondBest != -1) {
//								if (free[userSecondBest] >= 2) {
//									whatUserGet = this.allocated.getItem(userSecondBest)[2] + 2;
//									middle = free[userSecondBest] - 2;
//									free[userSecondBest]-=2;
//									whatAgentGet = this.allocated.getItem(userSecondBest)[0];
//									proposeArray = this.allocateNewOfferArray(whatAgentGet, middle, whatUserGet);
//									index = userSecondBest;
//									ServletUtils.log("I'm in myNextOffer --> 6", ServletUtils.DebugLevels.DEBUG);
//									propose.setItem(index, proposeArray);
//								} else {
//									int rand = -1, i = 0;;
//									do {
//										rand = Math.abs(r.nextInt() % this.allocated.getIssueCount() );
//										i++;
//									} while ((free[rand] < 1 || rand == userFave || rand == userSecondBest) && i < 20);
//									whatUserGet = this.allocated.getItem(rand)[2] + 1;
//									middle = free[rand] - 1;
//									free[rand]-=1;
//									whatAgentGet = this.allocated.getItem(rand)[0];
//									proposeArray = this.allocateNewOfferArray(whatAgentGet, middle, whatUserGet);
//									index = rand;
//									ServletUtils.log("I'm in myNextOffer --> 7", ServletUtils.DebugLevels.DEBUG);
//									propose.setItem(index, proposeArray);
//								}
//								
//							} else {
//								int rand = -1, i = 0;;
//								do {
//									rand = Math.abs(r.nextInt() % this.allocated.getIssueCount() );
//									i++;
//								} while ((free[rand] < 1 || rand == userFave || rand == userSecondBest) && i < 20);
//								if(free[rand] >= 2) {
//									whatUserGet = this.allocated.getItem(rand)[2] + 2;
//									middle = free[rand] - 2;
//									free[rand]-=2;
//									whatAgentGet = this.allocated.getItem(rand)[0];
//									proposeArray = this.allocateNewOfferArray(whatAgentGet, middle, whatUserGet);
//									index = rand;
//									ServletUtils.log("I'm in myNextOffer --> 8", ServletUtils.DebugLevels.DEBUG);
//									propose.setItem(index, proposeArray);
//								} else {
//									if(free[rand] > 0) {
//										whatUserGet = this.allocated.getItem(rand)[2] + 1;
//										middle = free[rand] - 1;
//										free[rand]-=1;
//										whatAgentGet = this.allocated.getItem(rand)[0];
//										proposeArray = this.allocateNewOfferArray(whatAgentGet, middle, whatUserGet);
//										index = rand;
//										ServletUtils.log("I'm in myNextOffer --> 9", ServletUtils.DebugLevels.DEBUG);
//										propose.setItem(index, proposeArray);
//									}
//									
//								}
//								
//							}
//							String next = "I tried my best to make an offer that you will be pleased with.";
//							this.message.setNextMessage(next);
//							propose.setItem(index, proposeArray);
//							
//						} else if(this.playerThreatMe && currGame == 3) {
//							int index = -1;
//							int rand = Math.abs(r.nextInt() % this.allocated.getIssueCount());
//							if(free[rand] >= 2) {
//								whatUserGet = this.allocated.getItem(rand)[2] + 1;
//								middle = free[rand] - 1;
//								free[rand]-=1;
//								whatAgentGet = this.allocated.getItem(rand)[0];
//								proposeArray = this.allocateNewOfferArray(whatAgentGet, middle, whatUserGet);
//								index = rand;
//								ServletUtils.log("I'm in myNextOffer --> 10", ServletUtils.DebugLevels.DEBUG);
//								propose.setItem(index, proposeArray);
//							} else {
//								do {
//									rand = Math.abs(r.nextInt() % this.allocated.getIssueCount());
//								} while (free[rand] < 1);
//								whatUserGet = this.allocated.getItem(rand)[2] + 1;
//								middle = free[rand] - 1;
//								free[rand]-=1;
//								whatAgentGet = this.allocated.getItem(rand)[0];
//								proposeArray = this.allocateNewOfferArray(whatAgentGet, middle, whatUserGet);
//								index = rand;
//								ServletUtils.log("I'm in myNextOffer --> 11", ServletUtils.DebugLevels.DEBUG);
//								propose.setItem(index, proposeArray);
//							}
//							this.message.setNextMessage("I don't think you should walk away this game. Here is my offer: ");
//							
//						} else if(!this.playerThreatMe && currGame < 3) {
//							{
//								
//								int index = -1;
//								int i = 0;
//								int rand = 0;
//								do {
//									rand = Math.abs(r.nextInt() % this.allocated.getIssueCount());
//									i++;
//								} while (free[rand] < 2 && i < 20);
//								
//								if(free[rand] >= 2) {
//									ServletUtils.log("free[rand] "   + free[rand] + " rand:" + rand, ServletUtils.DebugLevels.DEBUG);
//									whatUserGet = this.allocated.getItem(rand)[2] + 2;
//									middle = free[rand] - 2;
//									free[rand]-=2;
//									whatAgentGet = this.allocated.getItem(rand)[0];
//									proposeArray = this.allocateNewOfferArray(whatAgentGet, middle, whatUserGet);
//									index = rand;
//									ServletUtils.log("I'm in myNextOffer --> 12 "  + proposeArray[0] + ", " + proposeArray[1] + ", "+proposeArray[2], ServletUtils.DebugLevels.DEBUG);
//									ServletUtils.log("opponentFave = " + opponentFave, ServletUtils.DebugLevels.DEBUG);
//									propose.setItem(index, proposeArray);
//									if(free[opponentFave] >= 2 && opponentFave != rand && opponentFave!=-1) {
//										whatUserGet = this.allocated.getItem(opponentFave)[2];
//										middle = free[opponentFave] - 2;
//										free[opponentFave]-=2;
//										whatAgentGet = this.allocated.getItem(opponentFave)[0] + 2;
//										proposeArray = this.allocateNewOfferArray(whatAgentGet, middle, whatUserGet);
//										index = opponentFave;
//										ServletUtils.log("I'm in myNextOffer --> 13 " + + proposeArray[0] + ", " + proposeArray[1] + ", "+proposeArray[2], ServletUtils.DebugLevels.DEBUG);
//										propose.setItem(index, proposeArray);
//									} else if(free[opponentSecondBest] >= 2 && opponentSecondBest!=-1 && opponentSecondBest != rand) {
//										whatUserGet = this.allocated.getItem(opponentSecondBest)[2];
//										middle = free[opponentSecondBest] - 2;
//										free[opponentSecondBest]-=2;
//										whatAgentGet = this.allocated.getItem(opponentSecondBest)[0] + 2;
//										proposeArray = this.allocateNewOfferArray(whatAgentGet, middle, whatUserGet);
//										index = opponentSecondBest;
//										ServletUtils.log("I'm in myNextOffer --> 14", ServletUtils.DebugLevels.DEBUG);
//										propose.setItem(index, proposeArray);
//									} else if(free[opponentFave] > 0 && free[opponentSecondBest] > 0 && 
//											opponentFave != -1 && opponentSecondBest!=-1 ) {
//										whatUserGet = this.allocated.getItem(opponentFave)[2];
//										middle = free[opponentFave] - 1;
//										free[opponentFave]-=1;
//										whatAgentGet = this.allocated.getItem(opponentFave)[0] + 1;
//										proposeArray = this.allocateNewOfferArray(whatAgentGet, middle, whatUserGet);
//										index = opponentFave;
//										ServletUtils.log("I'm in myNextOffer --> 15", ServletUtils.DebugLevels.DEBUG);
//										propose.setItem(index, proposeArray);
//										//
//										whatUserGet = this.allocated.getItem(opponentSecondBest)[2];
//										middle = free[opponentSecondBest] - 1;
//										free[opponentSecondBest]-=1;
//										whatAgentGet = this.allocated.getItem(opponentSecondBest)[0] + 1;
//										proposeArray = this.allocateNewOfferArray(whatAgentGet, middle, whatUserGet);
//										index = opponentSecondBest;
//										ServletUtils.log("I'm in myNextOffer --> 13", ServletUtils.DebugLevels.DEBUG);
//										propose.setItem(index, proposeArray);
//									} else {
//										int rand1=-1;
//										do {
//											rand1 = Math.abs(r.nextInt() % this.allocated.getIssueCount());
//										} while (free[rand1] < 2 || rand1 == rand);
//										whatUserGet = this.allocated.getItem(rand1)[2] + 1;
//										middle = free[rand1] - 1;
//										free[rand1]-=1;
//										whatAgentGet = this.allocated.getItem(rand1)[0];
//										proposeArray = this.allocateNewOfferArray(whatAgentGet, middle, whatUserGet);
//										index = rand1;
//										ServletUtils.log("I'm in myNextOffer --> 14", ServletUtils.DebugLevels.DEBUG);
//										propose.setItem(index, proposeArray);
//										//
//										 i = 0;
//										do {
//											rand = Math.abs(r.nextInt() % this.allocated.getIssueCount());
//											i++;
//										} while ((free[rand] < 1 || rand == rand1) && i < 20);
//										whatUserGet = this.allocated.getItem(rand)[2];
//										middle = free[userFave] - 1;
//										free[rand]-=1;
//										whatAgentGet = this.allocated.getItem(rand)[0] + 1;
//										proposeArray = this.allocateNewOfferArray(whatAgentGet, middle, whatUserGet);
//										index = rand;
//										ServletUtils.log("I'm in myNextOffer --> 15", ServletUtils.DebugLevels.DEBUG);
//										propose.setItem(index, proposeArray);
//									}
//								}
//								this.message.setNextMessage("Take a look at this offer: ");
//							}
//						} else {
//							int index = -1;
//							int i = 0;
//							int rand = 0, rand1 = 0;
//							do {
//								rand1 = Math.abs(r.nextInt() % this.allocated.getIssueCount());
//							} while (free[rand1] < 1);
//							whatUserGet = this.allocated.getItem(rand)[2] + 1;
//							middle = free[rand1] - 1;
//							free[rand1]-=1;
//							whatAgentGet = this.allocated.getItem(rand1)[0];
//							proposeArray = this.allocateNewOfferArray(whatAgentGet, middle, whatUserGet);
//							index = rand1;
//							ServletUtils.log("I'm in myNextOffer --> 16", ServletUtils.DebugLevels.DEBUG);
//							propose.setItem(index, proposeArray);
//							//
//							do {
//								rand = Math.abs(r.nextInt() % this.allocated.getIssueCount());
//							} while (free[rand] < 1 || rand == rand1);
//							whatUserGet = this.allocated.getItem(rand)[2];
//							middle = free[userFave] - 1;
//							free[rand]-=1;
//							whatAgentGet = this.allocated.getItem(rand)[0] + 1;
//							proposeArray = this.allocateNewOfferArray(whatAgentGet, middle, whatUserGet);
//							index = rand;
//							propose.setItem(index, proposeArray);
//							ServletUtils.log("I'm in myNextOffer --> 17", ServletUtils.DebugLevels.DEBUG);
//							this.message.setNextMessage("Will you consider this one..? ");
//						}
//					} 
//					 else { //totalFree < 10
//						 int rand = Math.abs(r.nextInt() % this.allocated.getIssueCount());
//							int i = 0, index = -1;
//							do {
//								rand = Math.abs(r.nextInt() % this.allocated.getIssueCount());
//								i++;
//							} while(free[rand] < 1 && i < 50);
//							if(free[rand] > 0) {
//								if(this.playerThreatMe && currGame < 3) {
//									this.playerThreatMe = false;
//									whatUserGet = this.allocated.getItem(rand)[2] + 1;
//									middle = free[rand] - 1;
//									free[rand]-=1;
//									whatAgentGet = this.allocated.getItem(rand)[0];
//									proposeArray = this.allocateNewOfferArray(whatAgentGet, middle, whatUserGet);
//									index = rand;
//									ServletUtils.log("I'm in myNextOffer --> 18", ServletUtils.DebugLevels.DEBUG);
//									propose.setItem(index, proposeArray);
//									this.message.setNextMessage("I don't think you should walk away this game. Here is my offer: ");
//									
//									
//								} else {
//									whatUserGet = this.allocated.getItem(rand)[2] + 1;
//									middle = free[rand] - 1;
//									free[rand]-=1;
//									whatAgentGet = this.allocated.getItem(rand)[0];
//									proposeArray = this.allocateNewOfferArray(whatAgentGet, middle, whatUserGet);
//									index = opponentSecondBest;
//									propose.setItem(index, proposeArray);
//									//
//									if(free[opponentFave] > 0 && opponentFave!=-1 && opponentFave!= rand) {
//										whatUserGet = this.allocated.getItem(opponentFave)[2];
//										middle = free[opponentFave] - 1;
//										free[opponentFave]-=1;
//										whatAgentGet = this.allocated.getItem(opponentFave)[0] + 1;
//										proposeArray = this.allocateNewOfferArray(whatAgentGet, middle, whatUserGet);
//										index = rand;
//										ServletUtils.log("I'm in myNextOffer --> 19", ServletUtils.DebugLevels.DEBUG);
//										propose.setItem(index, proposeArray);
//									} else if(free[opponentSecondBest] > 0 && opponentSecondBest!=-1 && opponentSecondBest != rand ) {
//										whatUserGet = this.allocated.getItem(opponentSecondBest)[2];
//										middle = free[opponentSecondBest] - 1;
//										free[opponentSecondBest]-=1;
//										whatAgentGet = this.allocated.getItem(opponentSecondBest)[0] + 1;
//										proposeArray = this.allocateNewOfferArray(whatAgentGet, middle, whatUserGet);
//										index = opponentSecondBest;
//										ServletUtils.log("I'm in myNextOffer --> 20", ServletUtils.DebugLevels.DEBUG);
//										propose.setItem(index, proposeArray);
//									} else {
//										i=0;
//										do {
//											rand = Math.abs(r.nextInt() % this.allocated.getIssueCount());
//										} while (free[rand] < 1 || rand == opponentSecondBest || rand == opponentFave && i < 50);
//										whatUserGet = this.allocated.getItem(rand)[2];
//										middle = free[rand] - 1;
//										free[rand]-=1;
//										whatAgentGet = this.allocated.getItem(rand)[0] + 1;
//										proposeArray = this.allocateNewOfferArray(whatAgentGet, middle, whatUserGet);
//										index = rand;
//										ServletUtils.log("I'm in myNextOffer --> 21", ServletUtils.DebugLevels.DEBUG);
//										propose.setItem(index, proposeArray);
//									}
//									this.message.setNextMessage("Will you consider this one..? ");
//									
//								}
//							}
//							
//						
//					}
//					break;
//				}
//				
//				case LIMITED: {
//					{
//						int index = -1, i = 0, rand = -1, rand1 = -1;
//						rand1 = Math.abs(r.nextInt() % this.allocated.getIssueCount());
//						do {
//							rand1 = Math.abs(r.nextInt() % this.allocated.getIssueCount());
//							i++;
//						} while (free[rand1] < 1 && i < 20);
//						if(free[rand1] > 0) {
//							whatUserGet = this.allocated.getItem(rand1)[2] + 1;
//							middle = free[rand1] - 1;
//							free[rand1]-=1;
//							whatAgentGet = this.allocated.getItem(rand1)[0];
//							proposeArray = this.allocateNewOfferArray(whatAgentGet, middle, whatUserGet);
//							index = rand1;
//							ServletUtils.log("I'm in myNextOffer --> 22", ServletUtils.DebugLevels.DEBUG);
//							propose.setItem(index, proposeArray);
//							//
//							if(free[opponentFave] > 0 && opponentFave != -1 && opponentFave != rand) {
//								whatUserGet = this.allocated.getItem(opponentFave)[2];
//								middle = free[opponentFave] - 1;
//								free[opponentFave]-=1;
//								whatAgentGet = this.allocated.getItem(opponentFave)[0] + 1;
//								proposeArray = this.allocateNewOfferArray(whatAgentGet, middle, whatUserGet);
//								index = opponentFave;
//								ServletUtils.log("I'm in myNextOffer --> 23", ServletUtils.DebugLevels.DEBUG);
//								propose.setItem(index, proposeArray);
//							} else if(free[opponentSecondBest] > 0 && opponentSecondBest != -1 && opponentSecondBest != rand) {
//								whatUserGet = this.allocated.getItem(opponentSecondBest)[2];
//								middle = free[opponentSecondBest] - 1;
//								free[opponentSecondBest]-=1;
//								whatAgentGet = this.allocated.getItem(opponentSecondBest)[0] + 1;
//								proposeArray = this.allocateNewOfferArray(whatAgentGet, middle, whatUserGet);
//								index = opponentSecondBest;
//								ServletUtils.log("I'm in myNextOffer --> 24", ServletUtils.DebugLevels.DEBUG);
//								propose.setItem(index, proposeArray);
//							} else {
//								i=0;
//								do {
//									rand = Math.abs(r.nextInt() % this.allocated.getIssueCount());
//								} while ((free[rand] < 1 || rand==rand1 )&& i < 50);
//								whatUserGet = this.allocated.getItem(rand)[2];
//								middle = free[rand] - 1;
//								free[rand]-=1;
//								whatAgentGet = this.allocated.getItem(rand)[0] + 1;
//								proposeArray = this.allocateNewOfferArray(whatAgentGet, middle, whatUserGet);
//								index = rand;
//								ServletUtils.log("I'm in myNextOffer --> 25", ServletUtils.DebugLevels.DEBUG);
//								propose.setItem(index, proposeArray);
//							}
//						}
//						
//						
//					}
//					this.message.setNextMessage("This is my offer:  ");
//					break;
//				}
//				case BETRAYING: { 
//					if (opponentSecondBest >= 0) {
//						if (free[opponentSecondBest] >= 3) {
//							propose.setItem(opponentSecondBest, new int[] {allocated.getItem(opponentSecondBest)[0] + free[opponentSecondBest], 0, allocated.getItem(opponentSecondBest)[2]});
//							//this.allocated = propose;
//							this.concession = propose;
//							message.setNextMessage("Please accept this Offer, I'll remember that in the future..!");
//							ServletUtils.log("I'm in myNextOffer --> 26", ServletUtils.DebugLevels.DEBUG);
//							return propose;
//						}
//						else if(allocated.getItem(opponentSecondBest)[0] >= 1 && free[opponentSecondBest] >= 1 ) {
//							propose.setItem(opponentSecondBest, new int[] {allocated.getItem(opponentSecondBest)[0] + 2, free[opponentSecondBest] - 1, allocated.getItem(opponentSecondBest)[2] - 1});
//							//this.allocated = propose;
//							this.concession = propose;
//							message.setNextMessage("Please accept this Offer, I'll remember that in the future..!");
//							ServletUtils.log("I'm in myNextOffer --> 27", ServletUtils.DebugLevels.DEBUG);
//							return propose;
//						}
//						else if(allocated.getItem(opponentSecondBest)[0] >= 1) {
//							propose.setItem(opponentSecondBest, new int[] {allocated.getItem(opponentSecondBest)[0] + 1, free[opponentSecondBest], allocated.getItem(opponentSecondBest)[2] - 1});
//							//this.allocated = propose;
//							this.concession = propose;
//							ServletUtils.log("I'm in myNextOffer --> 28", ServletUtils.DebugLevels.DEBUG);
//							message.setNextMessage("Please accept this Offer, I'll remember that in the future..!");
//							return propose;
//						}
//					}
//					else {
//						if(opponentFave >= 0) 
//							if (free[opponentFave] >= 3) {
//								propose.setItem(opponentFave, new int[] {allocated.getItem(opponentFave)[0] + free[opponentFave], 0, allocated.getItem(opponentFave)[2]});
//								//this.allocated = propose;
//								this.concession = propose;
//								ServletUtils.log("I'm in myNextOffer --> 29", ServletUtils.DebugLevels.DEBUG);
//								message.setNextMessage("Please accept this Offer, I'll remember that in the future..!");
//								return propose;
//						}
//						else if(allocated.getItem(opponentFave)[0] >= 1 && free[opponentFave] >= 1 ) {
//							propose.setItem(opponentFave, new int[] {allocated.getItem(opponentFave)[0] + 2, free[opponentFave] - 1, allocated.getItem(opponentFave)[2] - 1});
//							//this.allocated = propose;
//							this.concession = propose;
//							ServletUtils.log("I'm in myNextOffer --> 30", ServletUtils.DebugLevels.DEBUG);
//							message.setNextMessage("Please accept this Offer, I'll remember that in the future..!");
//							return propose;
//						}
//						else if(allocated.getItem(opponentFave)[0] >= 1) {
//							propose.setItem(opponentFave, new int[] {allocated.getItem(opponentFave)[0] + 1, free[opponentFave], allocated.getItem(opponentFave)[2] - 1});
//							//this.allocated = propose;
//							this.concession = propose;
//							message.setNextMessage("Please accept this Offer, I'll remember that in the future..!");
//							ServletUtils.log("I'm in myNextOffer --> 31", ServletUtils.DebugLevels.DEBUG);
//							return propose;
//						}
//					}
//					break;
//				}
//				case NONE: 
//					//ignore
//				default:
//					return null; 
//				}
//		   }
//			//this.concession = propose;
//			ServletUtils.log("I'm in myNextOffer --> END OF MY NEXT OFFER" + propose, ServletUtils.DebugLevels.DEBUG);
//			if(propose != null)
//				return propose;
//			else 
//				return null;
	}
		
	
	//TODO:
	/**
		* assign behavior by logic above the return favor and no favor case
		* add message to each offer
		* finish the no favor cases
	**/
	@Override
	protected Offer getTimingOffer(History history) {
		return this.myNextOffer(history);
	}

	@Override
	protected Offer getAcceptOfferFollowup(History history) {
		
		Offer lastOffer;
		Offer propose = new Offer(game.getNumberIssues());
		for(int issue = 0; issue < game.getNumberIssues(); issue++)
			propose.setItem(issue, allocated.getItem(issue));
		
		ArrayList<Integer> playerPref = utils.getMinimaxOrdering(); 
		ArrayList<Integer> vhPref = utils.getMyOrdering();
		
		if(history.getHistory().getLast().getOffer() != null) {
			lastOffer = history.getHistory().getLast().getOffer();
			
		int[] free = this.freeIssues();
		int userFave = this.userFave(free, playerPref);
		int opponentFave = this.opponentFave(free, vhPref);
		
		int [][] last = new int[this.game.getNumberIssues()][3];
		int i = 0;
		for(i=0; i < this.game.getNumberIssues(); i++) {
				if(lastOffer.getItem(i) != null) {
					for(int j = 0; j < 3; j++) {
						last[i][j] = lastOffer.getItem(i)[j];
				  }
				} else {
					i--;
					break;
				}
					
			}
		Random rand = new Random();
		
		switch (this.lb) { 
		
		case FAIR: {
			int index = Math.abs(rand.nextInt() % 6);
			while(index > i) {
				index = Math.abs(rand.nextInt() % 6);
			}
			if(free[index] >= 1 && free[userFave] >= 1) {
				int[] arr =  {lastOffer.getItem(userFave)[0], free[userFave] - 1 ,lastOffer.getItem(userFave)[2] + 1};
				propose.setItem(index, arr);
				propose.setItem(opponentFave, new int[] {this.allocated.getItem(opponentFave)[0] + 1, free[opponentFave] - 1, this.allocated.getItem(opponentFave)[2]});
				this.concession = propose;
				return propose;
			}
			else return null;
				
		}
		
		case LIMITED: {
			int index = Math.abs(rand.nextInt() % 6);
			while(index > i) {
				index = Math.abs(rand.nextInt() % 6);
			}
			if(free[index] >= 1 && free[opponentFave] >= 1) {
				int[] arr =  {lastOffer.getItem(index)[0], free[index] - 1 ,lastOffer.getItem(index)[2] + 1};
				propose.setItem(index, arr);
				propose.setItem(opponentFave, new int[] {this.allocated.getItem(opponentFave)[0] + 1, free[opponentFave] - 1, this.allocated.getItem(opponentFave)[2]});
				this.concession = propose;
				return propose;
			}
			else return null;
		}
		
		case BETRAYING: {
			//ignore
		}
		
		case NONE: {
			//ignore
		}
	
		default:
			break;
	   }
		}
			
		return null;
	}
	
	@Override
	protected Offer getFirstOffer(History history) {
		
		if(firstOffer)
			return null;
		
		Offer propose = new Offer(game.getNumberIssues());
		for(int issue = 0; issue < game.getNumberIssues(); issue++)
			propose.setItem(issue, allocated.getItem(issue));
		// Assign ordering to the player based on perceived preferences. Ideally, they would be opposite the agent's (integrative)
		ArrayList<Integer> playerPref = utils.getMinimaxOrdering(); 
		ArrayList<Integer> vhPref = utils.getMyOrdering();
					
		int[] free = this.freeIssues();
		int userFave = this.userFave(free, playerPref);
		int opponentFave = this.opponentFave(free, vhPref);
		
		
		if(userFave == -1 || opponentFave == -1) {
			int val1 = -1;
			int val2 = -1;
			int index1 = -1;
			int index2 = -1;
			int i = 0;
			while (val1 < 1 && i < this.game.getNumberIssues())
			{
				val1 = free[i];
				index1 = i;
				i++;
			}
			while (val2 < 1 && i < this.game.getNumberIssues())
			{
				val2 = free[i];
				index2 = i;
				i++;
			}
			
			if(val1 > 0 && val2 > 0) {
				
			}
			propose.setItem(index1, new int[] {allocated.getItem(index1)[0], free[index1] - 1, allocated.getItem(index1)[2] + 1});
			propose.setItem(index2, new int[] {allocated.getItem(index2)[0] + 1, free[index2] - 1, allocated.getItem(index2)[2]});
			this.firstOffer = true;
			//this.allocated = propose;
			//this.concession = propose;
			State s = new State(propose, utils);
			ServletUtils.log("Reward of first offer " + s.getReward() , ServletUtils.DebugLevels.DEBUG);
			return propose;
		}
		else {
			
				propose.setItem(userFave, new int[] {allocated.getItem(userFave)[0], this.allocated.getItem(userFave)[1] - 1, allocated.getItem(userFave)[2] + 1});
				propose.setItem(opponentFave, new int[] {allocated.getItem(opponentFave)[0] + 1, this.allocated.getItem(opponentFave)[1] - 1, allocated.getItem(opponentFave)[2]});
				ServletUtils.log("INDEXESSSS ---------------> " + propose.getItem(opponentFave)[0] , ServletUtils.DebugLevels.DEBUG);
				this.firstOffer = true;
				//this.allocated = propose;
				//this.concession = propose;
				State s = new State(propose, utils);
				ServletUtils.log("Reward of first offer " + s.getReward() , ServletUtils.DebugLevels.DEBUG);
				return propose;
		}
	}

	
	public int getRealMargin() { //this is the real margin between user and agent
		int margin = 0;
		for (int i = 0; i < game.getNumberIssues(); i++) {
			margin += this.allocated.getItem(i)[0] - this.allocated.getItem(i)[2];
		}
		
		/**
		 if margin is positive, the user leads (margin is the lead value), 
		 if margin is negative the VH leads,
		 if margin equals 0 then it's a tie. 
		 **/
		return margin;
	}
	
	@Override
		protected int getAcceptMargin() {
		return Math.max(0,Math.min(game.getNumberIssues(), adverseEvents));
	}
	
	
	
	@Override
	protected Offer getRejectOfferFollowup(History history) {
		ServletUtils.log("i'm in rejected FOLLOW UP" , ServletUtils.DebugLevels.DEBUG);
		
		HillClimbing hill = new HillClimbing(game, history, this.allocated, utils, playerThreatMe, true);
		Offer o = hill.algorithm(this.allocated);
		ServletUtils.log("Result from hill climbing", ServletUtils.DebugLevels.DEBUG);
		ServletUtils.log("First Item " + o.getItem(0)[0] + " " + o.getItem(0)[1] + " " + o.getItem(0)[2], ServletUtils.DebugLevels.DEBUG);
		ServletUtils.log("Second Item " + o.getItem(1)[0] + " " + o.getItem(1)[1] + " " + o.getItem(1)[2], ServletUtils.DebugLevels.DEBUG);
		ServletUtils.log("Third Item " + o.getItem(2)[0] + " " + o.getItem(2)[1] + " " + o.getItem(2)[2], ServletUtils.DebugLevels.DEBUG);
		ServletUtils.log("Fourth Item " + o.getItem(3)[0] + " " + o.getItem(3)[1] + " " + o.getItem(3)[2], ServletUtils.DebugLevels.DEBUG);
		State s = new State(o, utils);
		double rewardS = s.getReward();
		ServletUtils.log("This is the reward for this offer" + s.getReward(), ServletUtils.DebugLevels.DEBUG);
		return o;
		
		
		
//		Offer lastOffer = this.concession;
//		ServletUtils.log("lastOffer " + lastOffer , ServletUtils.DebugLevels.DEBUG);
//		ArrayList<Integer> playerPref = utils.getMinimaxOrdering(); 
//		ArrayList<Integer> vhPref = utils.getMyOrdering();
//					
//		int[] free = this.freeIssues();
//		int userFave = this.userFave(free, playerPref);
//		int userSecondBest = this.utils.findAdversaryIdealSecondBest();
//		int opponentFave = this.opponentFave(free, vhPref);
//		int opponentSecondBest = this.findOpponentIdealSecondBest(vhPref);
//		int userWorst = this.utils.findAdversaryIdealWorst(game);
//		Offer next = new Offer(this.allocated.getIssueCount());
//			for(int i = 0; i < this.allocated.getIssueCount(); i++) {
//				next.setItem(i, this.allocated.getItem(i));
//			}
//		
//		
//		if(lastOffer == null)
//			return null;
//		else
//		{	
//			next = lastOffer;
//			int[]  whatUserGetOffer = new int[this.allocated.getIssueCount()];
//			int[]  whatVhGetOffer   = new int[this.allocated.getIssueCount()];
//			int[]  middle = new int[this.allocated.getIssueCount()];
//			int[] nextOfferArray = new int[3];
//			for(int i = 0; i < lastOffer.getIssueCount(); i++) {
//				whatUserGetOffer[i] = lastOffer.getItem(i)[2];
//				middle[i]    = lastOffer.getItem(i)[1];
//				whatVhGetOffer[i]   = lastOffer.getItem(i)[0];
//			}
//			switch(this.lb) {
//				case FAIR:{
//					ServletUtils.log("case FAIR" , ServletUtils.DebugLevels.DEBUG);
//					ServletUtils.log("allocated get item(userFave)[2] == " + this.allocated.getItem(userFave)[2] , ServletUtils.DebugLevels.DEBUG);
//					
//						if(this.allocated.getItem(userFave)[0] > 0 && whatVhGetOffer[userFave] > 0
//								&& whatUserGetOffer[userFave] < 5 && whatUserGetOffer[userFave] < 5 && userFave != -1) {
//							nextOfferArray[0] = whatVhGetOffer[userFave] - 1;
//							nextOfferArray[1] = middle[userFave];
//							nextOfferArray[2] = whatUserGetOffer[userFave] + 1;
//							next.setItem(userFave, nextOfferArray);
//							if (opponentFave != userFave && opponentFave != -1) {
//								if(this.allocated.getItem(opponentFave)[1] > 0) {
//									nextOfferArray[0] = whatVhGetOffer[opponentFave] + 1;
//									nextOfferArray[1] = middle[opponentFave] - 1;
//									nextOfferArray[2] = whatUserGetOffer[opponentFave];
//									next.setItem(opponentFave, nextOfferArray);
//								}
//							}
//							else if(opponentSecondBest != userFave && opponentSecondBest != -1) {
//								if(this.allocated.getItem(opponentSecondBest)[1] > 0 && middle[opponentSecondBest] > 0 && 
//										whatVhGetOffer[opponentSecondBest] < 5 && this.allocated.getItem(opponentSecondBest)[0] < 5) {
//									nextOfferArray[0] = whatVhGetOffer[opponentSecondBest] + 1;
//									nextOfferArray[1] = middle[opponentSecondBest] - 1;
//									nextOfferArray[2] = whatUserGetOffer[opponentSecondBest];
//									next.setItem(opponentSecondBest, nextOfferArray);
//								}
//							}
//							this.concession = next;
//							ServletUtils.log("return next " + next , ServletUtils.DebugLevels.DEBUG);
//							return next;
//						} else if(this.allocated.getItem(userSecondBest)[0] > 0 && whatVhGetOffer[userSecondBest] > 0 &&
//								whatUserGetOffer[userSecondBest] < 5 && this.allocated.getItem(userSecondBest)[2] < 5
//								&& userSecondBest != -1) {
//							nextOfferArray[0] = whatVhGetOffer[userSecondBest] - 1;
//							nextOfferArray[1] = middle[userSecondBest];
//							nextOfferArray[2] = whatUserGetOffer[userSecondBest] + 1;
//							next.setItem(userSecondBest, nextOfferArray);
//							if (opponentFave != userFave && opponentFave != -1) {
//								if(this.allocated.getItem(opponentFave)[1] > 0 && middle[opponentFave] > 0 && 
//										this.allocated.getItem(opponentFave)[0] < 5 && whatVhGetOffer[opponentFave] < 5) {
//									nextOfferArray[0] = whatVhGetOffer[opponentFave] + 1;
//									nextOfferArray[1] = middle[opponentFave] - 1;
//									nextOfferArray[2] = whatUserGetOffer[opponentFave];
//									next.setItem(opponentFave, nextOfferArray);
//								}
//							}
//							else if(opponentSecondBest != userFave && opponentSecondBest != -1) {
//								if(this.allocated.getItem(opponentSecondBest)[1] > 0 && middle[opponentSecondBest] > 0 && 
//										this.allocated.getItem(opponentSecondBest)[0] < 5 && whatVhGetOffer[opponentSecondBest] < 5) {
//									nextOfferArray[0] = whatVhGetOffer[opponentSecondBest] + 1;
//									nextOfferArray[1] = middle[opponentSecondBest] - 1;
//									nextOfferArray[2] = whatUserGetOffer[opponentSecondBest];
//									next.setItem(opponentSecondBest, nextOfferArray);
//								}
//							}
//							//this.allocated = next;
//							this.concession = next;
//							ServletUtils.log("return next " + next , ServletUtils.DebugLevels.DEBUG);
//							return next;
//						} else if(this.allocated.getItem(userWorst)[0] > 1 && whatVhGetOffer[userWorst] > 1
//								&& this.allocated.getItem(opponentFave)[2] > 0 && whatUserGetOffer[userWorst] < 4 && 
//								this.allocated.getItem(userWorst)[2] < 4 && whatVhGetOffer[opponentFave] < 5 
//								&& whatUserGetOffer[opponentFave] > 0 && this.allocated.getItem(opponentFave)[0] < 5) {
//							nextOfferArray[0] = whatVhGetOffer[userWorst] - 2;
//							nextOfferArray[1] = middle[userWorst];
//							nextOfferArray[2] = whatUserGetOffer[userWorst] + 2;
//							next.setItem(userWorst, nextOfferArray);
//							
//							nextOfferArray[0] = whatVhGetOffer[opponentFave] + 1;
//							nextOfferArray[1] = middle[opponentFave];
//							nextOfferArray[2] = whatUserGetOffer[opponentFave] - 1;
//							next.setItem(opponentFave, nextOfferArray);
//							this.concession = next;
//							ServletUtils.log("return next " + next , ServletUtils.DebugLevels.DEBUG);
//							return next;
//						}
//					} 
//					
//				case LIMITED:{
//					if(this.allocated.getItem(userWorst)[0] > 0 && whatVhGetOffer[userWorst] > 0 && 
//							this.allocated.getItem(userWorst)[2] < 5 && whatUserGetOffer[userWorst] < 5 &&
//							this.allocated.getItem(opponentFave)[0] < 5 && whatVhGetOffer[opponentFave] < 5 &&
//							this.allocated.getItem(opponentFave)[2] > 0 &&  whatUserGetOffer[opponentFave] > 0) {
//						nextOfferArray[0] = whatVhGetOffer[userWorst] - 1;
//						nextOfferArray[1] = middle[userWorst];
//						nextOfferArray[2] = whatUserGetOffer[userWorst] + 1;
//						next.setItem(userWorst, nextOfferArray);
//						
//						nextOfferArray[0] = whatVhGetOffer[opponentFave] + 1;
//						nextOfferArray[1] = middle[opponentFave];
//						nextOfferArray[2] = whatUserGetOffer[opponentFave] - 1;
//						next.setItem(opponentFave, nextOfferArray);
//						
//						this.concession = next;
//						return next;	
//					}
//					
//					//ServletUtils.log("return NULLLLLLLLLLLLLL", ServletUtils.DebugLevels.DEBUG);
//					return null;
//					
//				}
//				case BETRAYING:{
//					ServletUtils.log("I'm BETRAYING", ServletUtils.DebugLevels.DEBUG);
//					return null;
//				}
//				case NONE: {
//					ServletUtils.log("I'm BETRAYING", ServletUtils.DebugLevels.DEBUG);
//					return null;
//					//this agent doesn't care
//				}
//				default:
//					return null;
//				
//			}
//				
//				
//		}
		
	}
	
	@Override
	protected Offer getOfferAccordingToPref(int issue1, int issue2) {
		this.setCalledToOfferByPref(true);
		if(issue1 == -1 && issue2 == -1)
			return null;
		
		Offer propose = new Offer(this.allocated.getIssueCount());
		for(int issue = 0; issue < game.getNumberIssues(); issue++)
			propose.setItem(issue, allocated.getItem(issue));
		
		int []free = this.freeIssues();
		ArrayList<Integer> vhPref = utils.getMyOrdering();
		int opponentFave = this.opponentFave(free, vhPref);
		
//		if (this.message.getUserToldMeHisPref()) {
//			if (this.message.lastPref.getIssue1() != issue1 && this.message.lastPref.getIssue2() != issue2)
//				return null;
//		}
			
		int opponentSeconedBest = this.findOpponentIdealSecondBest(vhPref);
		
		switch (this.lb) {
		case FAIR: {
			int i = 0;
			while (i < this.allocated.getIssueCount()) {
				if(i == issue1) {
					if(free[i] > 0) { 
						ServletUtils.log("i =  " + i +" " + game.getIssuePluralText().get(i) , ServletUtils.DebugLevels.DEBUG);
						if(free[i]>1)
							propose.setItem(issue1, new int []{this.allocated.getItem(issue1)[0], free[issue1] - 2,this.allocated.getItem(issue1)[2] + 2 });
						if(free[i] == 1){
							propose.setItem(issue1, new int []{this.allocated.getItem(issue1)[0], free[issue1] - 1,this.allocated.getItem(issue1)[2] + 1 });
						}
						if(opponentFave != issue1 && free[opponentFave] > 0 && opponentFave != -1 ) {
								propose.setItem(opponentFave,new int[] {this.allocated.getItem(opponentFave)[0] + 1, free[opponentFave] - 1, this.allocated.getItem(opponentFave)[2]});
								//ServletUtils.log("OPPONENTFAVE =  =  "+ opponentFave +" " + game.getIssuePluralText().get(opponentFave) , ServletUtils.DebugLevels.DEBUG);
						}
						else if(free[opponentSeconedBest] > 0 && opponentSeconedBest != issue1 && opponentSeconedBest != -1){
							 opponentSeconedBest = this.findOpponentIdealSecondBest(vhPref);
							 propose.setItem(opponentSeconedBest,new int[] {this.allocated.getItem(opponentSeconedBest)[0] + 1, free[opponentSeconedBest] - 1, this.allocated.getItem(opponentSeconedBest)[2]});
							 //ServletUtils.log("opponentSecondFave = " + opponentFave + " "+ game.getIssuePluralText().get(opponentSeconedBest) , ServletUtils.DebugLevels.DEBUG);
							
						} else {
							int rand = -1;
							int k = 0;
							Random r = new Random();
							do {
								rand = Math.abs(r.nextInt() % this.allocated.getIssueCount());
								k++;
							} while ((free[rand] < 1 || rand == opponentSeconedBest || rand == opponentFave) && k < 50);
								if (free[rand] > 0) {
									 propose.setItem(rand,new int[] {this.allocated.getItem(rand)[0] + 1, free[rand] - 1, this.allocated.getItem(rand)[2]});
								}
						}
					}	
				}
				i++;	
			}
				
				if(propose != null) {
					this.concession = propose;
					//ServletUtils.log("propose = " + propose , ServletUtils.DebugLevels.DEBUG);
					return propose;
				}
				else
					return null;
				
				
				
		}
				
		case LIMITED:
		{
				int i = 0;
				while (i < this.allocated.getIssueCount()) {
					if(i == issue1) {
						if(free[i] > 0) { 
							propose.setItem(issue1, new int []{this.allocated.getItem(issue1)[0], free[issue1] - 1,this.allocated.getItem(issue1)[2] + 1 });
							if(opponentFave != issue1 && free[opponentFave] > 1) {
									propose.setItem(opponentFave,new int[] {this.allocated.getItem(opponentFave)[0] + 2, free[opponentFave] - 2, this.allocated.getItem(opponentFave)[2]});
							}
							else {
								 opponentSeconedBest = this.findOpponentIdealSecondBest(vhPref);
								if (free[opponentSeconedBest] > 1 && opponentSeconedBest != issue1) {
										propose.setItem(opponentSeconedBest,new int[] {this.allocated.getItem(opponentSeconedBest)[0] + 2, free[opponentSeconedBest] - 2, this.allocated.getItem(opponentSeconedBest)[2]});
								}
							}
						}
							
					}
					i++;	
				}
					
					if(propose != null) {
						this.concession = propose;
						return propose;
					}
					else
						return null;
				
		}
		case BETRAYING:{
				return null;
			}
		default:
			return null;
		
		}
		
		
	}
	
	public boolean shouldAcceptOffer(Offer o)
	{
		State newOffer = new State(o, utils);
		State current = new State(this.allocated, utils);
		if(newOffer.getReward() < current.getReward()) {
			return false;
		}
		return true;
		/*double myValue, opponentValue, jointValue;
		myValue = utils.myActualOfferValue(o);
		opponentValue = utils.getAdversaryValue(o);
		ServletUtils.log("acceptOffer method - Agent Value: " + myValue + ", Perceived Opponent Value: " + opponentValue + ", Opponent BATNA: " + utils.adversaryBATNA, ServletUtils.DebugLevels.DEBUG);
		jointValue = myValue + opponentValue;
		State s = new State(o, utils, game);
		s.getReward();
		return (myValue/jointValue > .7 & myValue >= utils.myPresentedBATNA) || 
			   (!utils.conflictBATNA(utils.myPresentedBATNA, utils.adversaryBATNA) && myValue >= utils.myPresentedBATNA);*/	//The threshold of .6 is semi-arbitrary
	}
	 
	@Override
	public void setLedgerBehavior(int currGame) { //THIS IS THE LEDGER BEHAVIOR FOR THE RETURNING FAVORS
//		if(utils.getVerbalLedger() > 0) { //we have to return favor
//			if(currGame == 3) {
//				if(game.getTotalTime() < 200 || playerThreatMe || this.playerLieThreshHold>=0.5) {
//					//do not return a favor..
//					ServletUtils.log("I'm BETRAYING", ServletUtils.DebugLevels.ERROR);
//					this.setLedgerBehavior("betraying");
//				}
//				/*if (free[userSecondBest] >= 1) {
//					propose.setItem(userSecondBest, new int[] {allocated.getItem(userSecondBest)[2], free[userSecondBest] - 1, allocated.getItem(userSecondBest)[0] + 1});
//					utils.modifyOfferLedger(1);
//					return propose;
//				}*/
//				else
//					this.setLedgerBehavior("limited");
//					
//			}
//			else {
//				if (this.playerLieThreshHold <= 0.5 || playerThreatMe) { // this is not game 3 and  the player not a liar
//					this.setLedgerBehavior("fair");
//					//gives the user best free items
//					/*utils.modifyOfferLedger(1);
//					propose.setItem(userBest, new int[] {allocated.getItem(userBest)[2], 0, allocated.getItem(userBest)[0] + free[userBest]});
//					return propose;*/
//				}
//				else
//					this.setLedgerBehavior("limited");
//			}
//		}
		this.setLedgerBehavior("fair");	}
	@Override
	protected void setCurrGame(int currentGameCount) {
		this.currGame = currentGameCount;
		
	}
	
	@Override
	public int getGameCount() {
		return this.currGame;
	}
}