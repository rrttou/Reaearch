package edu.usc.ict.iago.agent;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map.Entry;
import java.util.Random;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

import javax.websocket.Session;

import edu.usc.ict.iago.utils.Event;
import edu.usc.ict.iago.utils.GameSpec;
import edu.usc.ict.iago.utils.GeneralVH;
import edu.usc.ict.iago.utils.History;
import edu.usc.ict.iago.utils.Offer;
import edu.usc.ict.iago.utils.Preference;
import edu.usc.ict.iago.utils.ServletUtils;
import edu.usc.ict.iago.utils.Event.EventClass;

public abstract class IAGOCoreVH extends GeneralVH
{
	private Offer lastOfferReceived;
	private Offer lastOfferSent;
	private int numRejectedByUser = 0;
	private Offer favorOffer; //the favor offer
	private boolean favorOfferIncoming = false; //marked when an offer is about to be sent that is a favor
	private IAGOCoreBehavior behavior;
	private IAGOCoreExpression expression;
	private IAGOCoreMessage messages;
	private AgentUtilsExtension utils;
	private boolean timeFlag = false;
	private boolean firstFlag = false;
	private int noResponse = 0;
	private boolean noResponseFlag = false;
	private boolean firstGame = true;
	private boolean disable = false;	//adding a disabler check to help with agent vs. P++ functionality (note this will only be added to corevh and not the ++ version)
	private int currentGameCount = 0;
	private Ledger myLedger = new Ledger();
	private HashMap<Integer, Double> map = new HashMap<Integer, Double>();
	private State gameState;
	
	private class Ledger
	{
		int ledgerValue = 0; //favors are added to the final ledger iff the offer was accepted, otherwise discarded.  Positive means agent has conducted a favor.
		int verbalLedger = 0; //favors get added in here via verbal agreement.  Positive means agent has agreed to grant favors.
		int offerLedger = 0;  //favors are moved here and consumed from verbal when offer is made.  Positive means agent has made a favorable offer.
	}
	


	/**
	 * Constructor for most VHs used by IAGO.
	 * @param name name of the agent. NOTE: Please give your agent a unique name. Do not copy from the default names, such as "Pinocchio,"
	 * or use the name of one of the character models. An agent's name and getArtName do not need to match.
	 * @param game the GameSpec that the agent plays in first.
	 * @param session the web session that the agent will be active in.
	 * @param behavior Every core agent needs a behavior extending CoreBehavior.
	 * @param expression Every core agent needs an expression extending CoreExpression.
	 * @param messages Every core agent needs a message extending CoreMessage.
	 */
	public IAGOCoreVH(String name, GameSpec game, Session session, IAGOCoreBehavior behavior,
			IAGOCoreExpression expression, IAGOCoreMessage messages)
	{
		super(name, game, session);

		AgentUtilsExtension aue = new AgentUtilsExtension(this);
		aue.configureGame(game);
		this.utils = aue;
		this.expression = expression;
		this.messages = messages;
		this.behavior = behavior;
		aue.setAgentBelief(this.behavior.getAgentBelief());
		messages.setBehavior(behavior);
		behavior.setMessage(messages);
		this.messages.setUtils(utils);
		this.behavior.setUtils(utils);
		
	}
	
	/**
	 * Returns a simple int representing the internal "ledger" of favors done for the agent.  Can be negative.  Persists across games.
	 * @return the ledger
	 */
	protected int getLedger()
	{
		return myLedger.ledgerValue;
	}
	
	/**
	 * Returns a simple int representing the internal "ledger" of favors done for the agent, including all pending values.  Can be negative.  Does not persist across games.
	 * @return the ledger
	 */
	protected int getTotalLedger()
	{
		return myLedger.ledgerValue + myLedger.offerLedger + myLedger.verbalLedger;
	}
	
	/**
	 * Returns a simple int representing the potential "ledger" of favors verbally agreed to.  Can be negative.  Does not persist across games.
	 * @return the ledger
	 */
	protected int getVerbalLedger()
	{
		return myLedger.verbalLedger;
	}
	
	/**
	 * Allows you to modify the agent's internal "ledger" of verbal favors done for it.  
	 * @param increment value (negative ok)
	 */
	protected void modifyVerbalLedger(int increment)
	{
		ServletUtils.log("Verbal Event! Previous Ledger: v:" + myLedger.verbalLedger + ", o:" + myLedger.offerLedger + ", main:" + myLedger.ledgerValue, ServletUtils.DebugLevels.DEBUG);		
		
		myLedger.verbalLedger += increment;
		
		ServletUtils.log("Verbal Event! Current Ledger: v:" + myLedger.verbalLedger + ", o:" + myLedger.offerLedger + ", main:" + myLedger.ledgerValue, ServletUtils.DebugLevels.DEBUG);	
	}
	
	/**
	 * Allows you to modify the agent's internal "ledger" of offer favors proposed.  
	 * @param increment value (negative ok)
	 */
	protected void modifyOfferLedger(int increment)
	{
		ServletUtils.log("Offer Event! Previous Ledger: v:" + myLedger.verbalLedger + ", o:" + myLedger.offerLedger + ", main:" + myLedger.ledgerValue, ServletUtils.DebugLevels.DEBUG);		
		
		myLedger.verbalLedger -= increment;
		myLedger.offerLedger += increment;
		
		favorOfferIncoming = true;
		
		ServletUtils.log("Offer Event! Current Ledger: v:" + myLedger.verbalLedger + ", o:" + myLedger.offerLedger + ", main:" + myLedger.ledgerValue, ServletUtils.DebugLevels.DEBUG);	
	}
	
	/**
	 * Allows you to modify the agent's internal "ledger" of offer favors actually executed.  
	 * Automatically takes the execution from the offerLedger.
	 */
	private void modifyLedger()
	{
		ServletUtils.log("Finalization Event! Previous Ledger: v:" + myLedger.verbalLedger + ", o:" + myLedger.offerLedger + ", main:" + myLedger.ledgerValue, ServletUtils.DebugLevels.DEBUG);		
		
		myLedger.ledgerValue += myLedger.offerLedger;
		myLedger.offerLedger = 0;
		
		ServletUtils.log("Finalization Event! Current Ledger: v:" + myLedger.verbalLedger + ", o:" + myLedger.offerLedger + ", main:" + myLedger.ledgerValue, ServletUtils.DebugLevels.DEBUG);
	}
	
	/**
	 * Returns a simple int representing the current game count. 
	 * @return the game number (starting with 1)
	 */
	protected int getGameCount()
	{
		return currentGameCount;
	}
	

	/**
	 * Agents work by responding to various events. This method describes how core agents go about selecting their responses.
	 * @param e the Event that the agent will respond to.
	 * @return a list of Events in response to the initial Event.
	 */
	
	
	@Override
	public LinkedList<Event> getEventResponse(Event e) 
	{

		LinkedList<Event> resp = new LinkedList<Event>();
		
		
		
		
		
		
		/**what to do when the game has changed -- this is only necessary because our AUE needs to be updated.
			Game, the current GameSpec from our superclass has been automatically changed!
			IMPORTANT: between GAME_END and GAME_START, the gameSpec stored in the superclass is undefined.
			Furthermore, attempting to access data that is decipherable with a previous gameSpec could lead to exceptions!
			For example, attempting to decipher an offer from Game 1 while in Game 2 could be a problem (what if Game 1 had 4 issues, but Game 2 only has 3?)
			You should always treat the current GameSpec as true (following a GAME_START) and store any useful metadata about past games yourself.
		 **/
		
		

		if(e.getType().equals(Event.EventClass.GAME_START)) 
		{
			this.behavior.setCurrGame(this.currentGameCount + 1);	
			currentGameCount++;
			
			map = new HashMap<Integer, Double>();
			gameState = new State(this.game, this.utils);
			//ServletUtils.log("Game number is now " + currentGameCount + "... reconfiguring!", ServletUtils.DebugLevels.DEBUG);
			//ServletUtils.log("NOa kirel the queen: " + History.OPPONENT_ID, ServletUtils.DebugLevels.DEBUG);
			//ServletUtils.log("Please don't stuck: " + History.USER_ID, ServletUtils.DebugLevels.DEBUG);

			AgentUtilsExtension aue = new AgentUtilsExtension(this);
			aue.configureGame(game);
			this.utils = aue;
			this.messages.setUtils(utils);
			this.behavior.setUtils(utils);	
			this.disable = false;

			//if we wanted, we could change our Policies between games... but not easily.  Probably don't do that.  Just don't.

			//we should also reset some things
			timeFlag = false;
			firstFlag = false;
			noResponse = 0;
			noResponseFlag = false;
			myLedger.offerLedger = 0;
			myLedger.verbalLedger = 0;
			
		
			
			boolean liar = messages.getLying(game);
			utils.myPresentedBATNA = utils.getLyingBATNA(game, utils.LIE_THRESHOLD, liar);
			
			

			if(!firstGame)
			{
				String[] newGameMessage = {"It's good to see you again!  Let's get ready to negotiate again.", "My robot heart feels it is so nice to meet you again!", 
						"So good to see you again (I'm watching you over the screen)", "Hello Again and good luck from far away the bootom of the computer where i'm coming from"};
				Random r = new Random();
				String msg = newGameMessage[Math.abs(r.nextInt() % 4)];
				Event e0 = new Event(this.getID(), Event.EventClass.SEND_MESSAGE, Event.SubClass.NONE, msg, (int) (100*game.getMultiplier()));
				resp.add(e0);
				firstGame = false;
				//this.getHistory().getOpponentHistory().addAll(resp);
				return resp;
			} else {
				String newGameMessage = "Hi! Nice to meet youooooo. I just wanted you to know some important things about me:   \n "
						+ "1. My favorite movie is FROZEN, the second is Lion King.\n"
						+ "2. My parents (creators) are from Wonderland (They are so wonderful).\n"
						+ "3. Maybe i'm a robot but I do have a heart.\n"
						+ "4. Seriously, my main goal in this nagotiation with you is that both of us end up satisfied.\n"
						+ "5. Good luck :)";
				Event e0 = new Event(this.getID(), Event.EventClass.SEND_MESSAGE, Event.SubClass.NONE, newGameMessage, (int) (100*game.getMultiplier()));
				resp.add(e0);
				Event sendFirstEmotion = new Event(this.getID(),Event.EventClass.SEND_EXPRESSION, this.expression.getHappy(), 10000, 1000);
				resp.add(sendFirstEmotion);
				firstGame = false;
				
			}
			
		}
		
		
	
			
//		Event offer0 = new Event(this.getID(), Event.EventClass.SEND_OFFER, behavior.myNextOffer(getHistory()), 0);
//		Event offer1 = new Event(this.getID(), Event.EventClass.SEND_OFFER, behavior.getFinalOffer(getHistory()), 0);
//		Event offer2 = new Event(this.getID(), Event.EventClass.SEND_OFFER, behavior.getAcceptOfferFollowup(getHistory()), 0);
//		Event offer3 = new Event(this.getID(), Event.EventClass.SEND_OFFER, behavior.getRejectOfferFollowup(getHistory()), 0);
//		Event offer4 = new Event(this.getID(), Event.EventClass.SEND_OFFER, behavior.getOfferAccordingToPref(1, 2), 0);
//		Event offer5 = new Event(this.getID(), Event.EventClass.SEND_OFFER, behavior.getOfferAccordingToPref(-1, 2), 0);
//		Event offer6 = new Event(this.getID(), Event.EventClass.SEND_OFFER, behavior.getOfferAccordingToPref(1, -1), 0);
//		Event offer7 = new Event(this.getID(), Event.EventClass.SEND_OFFER, behavior.getOfferAccordingToPref(-1, -1), 0);
		
		
		//this.getHistory().getUserHistory().add(e);
	
		 if(e.getType().equals(Event.EventClass.OFFER_IN_PROGRESS)) 
		{	
			disable = true;
			ServletUtils.log("Agent is currently being restrained", ServletUtils.DebugLevels.DEBUG);
			//this.getHistory().getOpponentHistory().addAll(resp);
			return resp;
		}
		 
		 /**
		  * -----------START OF FIRST OFFER BLOCK-------------------
		  * e.g if the agent should lead with first offer
		  * TODO change message.getFavorBehavior for all cases
		  * TODO keep @param boolean if we sent first offer in game 1, then in game 2 ask for preference with message of that 
		  **/
		 
		  

		//should we lead with an offer?
		if(!firstFlag && !this.disable && this.currentGameCount < 3)
		{
			ServletUtils.log("First offer being made.", ServletUtils.DebugLevels.DEBUG);
			firstFlag = true;
			Offer next = behavior.getFirstOffer(getHistory());
			Event e2 = new Event(this.getID(), Event.EventClass.SEND_OFFER, next, 0);
			//ServletUtils.log("GAME num issues ---> " + this.game.getNumberIssues(), ServletUtils.DebugLevels.DEBUG);
			//ServletUtils.log("OFFER num issues ---> " + next.getIssueCount(), ServletUtils.DebugLevels.DEBUG);
			String revealBATNA = "My honest minimum goal is " + utils.myPresentedBATNA + " points, so I won't accept anything less, and I hope we both will get what we want!   What about you?";
			Event e5 = new Event(this.getID(), Event.EventClass.SEND_MESSAGE, Event.SubClass.BATNA_INFO, utils.myPresentedBATNA, revealBATNA,  (int) (1000*game.getMultiplier()));
			resp.add(e5);
			if(e2.getOffer() != null)
			{
				//ServletUtils.log("First offer isn't null.", ServletUtils.DebugLevels.DEBUG);
				Event e3 = new Event(this.getID(), Event.EventClass.OFFER_IN_PROGRESS, (int) (1000*game.getMultiplier()));
				resp.add(e3);
				Event e4 = new Event(this.getID(), Event.EventClass.SEND_MESSAGE, Event.SubClass.NONE, messages.getProposalLangFirst(),  (int) (1000*game.getMultiplier()));
				resp.add(e4);
				lastOfferSent = e2.getOffer();
				resp.add(e2);
			}

			
			disable = false;
			//implement get favor behavior and return event (maybe offer..?)
			Event e6 = messages.getFavorBehavior(getHistory(), game, e);
			//ServletUtils.log("e6 ----> " + this.getHistory().getHistory().getLast().getType(), ServletUtils.DebugLevels.DEBUG);
			
			if (e6 != null && (e6.getType() == EventClass.OFFER_IN_PROGRESS || e6.getSubClass() == Event.SubClass.FAVOR_ACCEPT)) 
			{
				Event e7 = new Event(this.getID(), Event.EventClass.SEND_OFFER, behavior.myNextOffer(getHistory()), (int) (700*game.getMultiplier())); 
				if (e7.getOffer() != null)
				{
					String s1 = this.messages.getProposalLang(getHistory(), game);
					 s1 += " " + messages.GetNextMessage();
					Event e8 = new Event(this.getID(), Event.EventClass.SEND_MESSAGE, Event.SubClass.OFFER_PROPOSE, s1, (int) (2000*game.getMultiplier()));
					resp.add(e6);
					resp.add(e8);
					resp.add(e7);
					this.lastOfferSent = e7.getOffer();
					if(favorOfferIncoming)
					{
						favorOffer = lastOfferSent;
						favorOfferIncoming = false;
					}
				} 
			}
			else 
			{
				resp.add(e6);
			}
			//this.getHistory().getOpponentHistory().addAll(resp);
			return resp;

		}
		
		/**
		 * ----------------END OF FIRST OFFER BLOCK-----------------------
		 * from here, @param firstFlag will be true
		 * After agent make first offer, the game actually starts from here
		 */
	
		
		/**
		 * ----------------START OF SEND EXPRESSION BLOCK----------------------------- 
		 * (e.g if user sent expression to the agent - what should I do?)
		 */
		//what to do when player sends an expression -- react to it with text and our own expression
		if(e.getType().equals(Event.EventClass.SEND_EXPRESSION))
		{
			String expr = expression.getExpression(getHistory());
			if (expr != null)
			{
				Event e1 = new Event(this.getID(), Event.EventClass.SEND_EXPRESSION, expr, 2000, (int) (700*game.getMultiplier()));
				resp.add(e1);
			}
			Event e0 = messages.getVerboseMessageResponse(getHistory(), game, e);
			if (e0 != null && (e0.getType() == EventClass.OFFER_IN_PROGRESS || e0.getSubClass() == Event.SubClass.FAVOR_ACCEPT))
			{
				Event e2 = new Event(this.getID(), Event.EventClass.SEND_OFFER, behavior.myNextOffer(getHistory()), (int) (700*game.getMultiplier())); 
				if (e2.getOffer() != null) 
				{
					String s1 = messages.getProposalLang(getHistory(), game);
					Event e1 = new Event(this.getID(), Event.EventClass.SEND_MESSAGE, Event.SubClass.OFFER_PROPOSE, s1, (int) (2000*game.getMultiplier()));
					resp.add(e0);
					resp.add(e1);
					resp.add(e2);
					this.lastOfferSent = e2.getOffer();
					if(favorOfferIncoming)
					{
						favorOffer = lastOfferSent;
						favorOfferIncoming = false;
					}
				}
			} 
			else if (e0 != null) 
			{ 
				resp.add(e0);
			}
			disable = false;
			//this.getHistory().getOpponentHistory().addAll(resp);
			return resp;
		}

		// When to formally accept when player sends an incoming formal acceptance
		if(e.getType().equals(Event.EventClass.FORMAL_ACCEPT))
		{
			Event lastOffer = utils.lastEvent(getHistory().getHistory(), Event.EventClass.SEND_OFFER);
			Event lastTime = utils.lastEvent(getHistory().getHistory(), Event.EventClass.TIME);
			
			disable = false;
			
			int totalItems = 0;
			for (int i = 0; i < game.getNumberIssues(); i++)
				totalItems += game.getIssueQuantities().get(i);
			if(lastOffer != null && lastTime != null)
			{
				//approximation based on distributive case
				int fairSplit = ((game.getNumberIssues() + 1) * totalItems / 4);
				//down to the wire, accept anything better than BATNA (less than 30 seconds from finishing time)
				if(utils.myActualOfferValue(lastOffer.getOffer()) > game.getBATNA(getID()) && Integer.parseInt(lastTime.getMessage()) + 30 > game.getTotalTime()) 
				{
					Event e0 = new Event(this.getID(), Event.EventClass.FORMAL_ACCEPT, 0);
					resp.add(e0);
					//this.getHistory().getOpponentHistory().addAll(resp);
					return resp;
				}
				//accept anything better than fair minus margin

				if (behavior instanceof IAGOCompetitiveBehavior)
				{
					if(((IAGOCompetitiveBehavior) behavior).acceptOffer(lastOffer.getOffer())){
						Event e0 = new Event(this.getID(), Event.EventClass.FORMAL_ACCEPT, 0);
						resp.add(e0);
						//this.getHistory().getOpponentHistory().addAll(resp);
						return resp;
					}
				}
				else if(utils.myActualOfferValue(lastOffer.getOffer()) > fairSplit - behavior.getAcceptMargin()) //accept anything better than fair minus margin
				{
					Event e0 = new Event(this.getID(), Event.EventClass.FORMAL_ACCEPT, 0);
					resp.add(e0);
					//this.getHistory().getOpponentHistory().addAll(resp);
					return resp;
				}
				else
				{
					Event e1 = new Event(this.getID(), Event.EventClass.SEND_MESSAGE, Event.SubClass.OFFER_REJECT, messages.getRejectLang(getHistory(), game), (int) (700*game.getMultiplier()));
					resp.add(e1);
					behavior.updateAdverseEvents(1);
					//this.getHistory().getOpponentHistory().addAll(resp);
					return resp;					
				}
			}
		}

		/**
		 * ----------------------------END OF SEND EXPRESSION BLOCK----------------------------------
		 */
		
		
		/**
		 * ----------------------------START OF TIME BLOCK----------------------------------------
		 */
		
		//ServletUtils.log("BEFOR TIME*****&&&&&^^^^^^^^%%%%%%%%%$$$$$$$", ServletUtils.DebugLevels.DEBUG);
		//what to do with delays on the part of the other player
		if(e.getType().equals(Event.EventClass.TIME))
		{
			//ServletUtils.log("ENTERED TIME", ServletUtils.DebugLevels.DEBUG);
			noResponse += 1;
			for(int i = getHistory().getHistory().size() - 1 ; i > 0 && i > getHistory().getHistory().size() - 6; i--)//if something from anyone for four time intervals
			{
				Event e1 = getHistory().getHistory().get(i);
				if(e1.getType() != Event.EventClass.TIME) 
					noResponse = 0;
			}

			if(noResponse >= 2)
			{
				Event e0 = messages.getVerboseMessageResponse(getHistory(), game, e);
				// Theoretically, this block isn't necessary. Event e0 should just be a message returned by getWaitingLang
				if (e0 != null && (e0.getType() == EventClass.OFFER_IN_PROGRESS || e0.getSubClass() == Event.SubClass.FAVOR_ACCEPT)) 
				{
					Event e2 = new Event(this.getID(), Event.EventClass.SEND_OFFER, behavior.myNextOffer(getHistory()), (int) (700*game.getMultiplier())); 
					if (e2.getOffer() != null)
					{
						String s1 = messages.getProposalLang(getHistory(), game);
						Event e1 = new Event(this.getID(), Event.EventClass.SEND_MESSAGE, Event.SubClass.OFFER_PROPOSE, s1, (int) (2000*game.getMultiplier()));
						resp.add(e0);
						resp.add(e1);
						resp.add(e2);
						this.lastOfferSent = e2.getOffer();
						if(favorOfferIncoming)
						{
							favorOffer = lastOfferSent;
							favorOfferIncoming = false;
						}
					} 
				}
				else 
				{
					resp.add(e0);
				}

				noResponseFlag = true;
			}
			else if(noResponse >= 1 && noResponseFlag)
			{
				noResponseFlag = false;
				Event e2 = new Event(this.getID(), Event.EventClass.SEND_OFFER, behavior.getTimingOffer(getHistory()), 0); 
				if(e2.getOffer() != null)
				{
					Event e3 = new Event(this.getID(), Event.EventClass.OFFER_IN_PROGRESS, 0);
					resp.add(e3);
					Event e4 = new Event(this.getID(), Event.EventClass.SEND_MESSAGE, Event.SubClass.OFFER_PROPOSE, messages.getProposalLang(getHistory(), game),  (int) (1000*game.getMultiplier()));
					resp.add(e4);
					resp.add(e2);
				}
			}
			
			

			// Times up
			String msg = e.getMessage();
			
			/**
			 *               TO EXPLORE
			 */
			if(this.behavior.getAllocated() != null && this.game!=null) {
				gameState.updateState(this.behavior.getAllocated());
				map.put(Integer.parseInt(msg), (double)(utils.myActualOfferValue(this.behavior.getAllocated())));
				ServletUtils.log("rewardddd raz " + (double)gameState.getReward(), ServletUtils.DebugLevels.DEBUG);
			}
			{
				try {
					String name = "";
					if(this.behavior.getGameCount() == 1) {
						 name = "/Users/razyaniv/Downloads/check.csv";
					}
					if(this.behavior.getGameCount() == 2) {
						 name = "/Users/razyaniv/Downloads/data2sim.csv";
					}
					if(this.behavior.getGameCount() == 3) {
						 name = "/Users/razyaniv/Downloads/data3sim.csv";
					}
					PrintWriter out=new PrintWriter(new FileWriter(name));
					out.write("Time" + "," + "Value\n");
					for(Entry<Integer, Double> entry : map.entrySet()) {
					    Integer key = entry.getKey();
					    Double value = (double)entry.getValue();

					    out.write(key + "," + value + "\n");
					    ServletUtils.log("offer value " + utils.myActualOfferValue(this.behavior.getAllocated()), ServletUtils.DebugLevels.DEBUG);
					}
					out.close();
				} catch (IOException e9) {
					// TODO Auto-generated catch block
					e9.printStackTrace();
				}}
			
			//ServletUtils.log("e.getMessage = " + msg , ServletUtils.DebugLevels.DEBUG);
			if(msg != null) { //&& e.getSubClass().equals(Event.SubClass.TIMING)) {
				if(!timeFlag && game.getTotalTime() - Integer.parseInt(msg) < 60)
				{
					int msgToInt = Integer.parseInt(msg);
					
					//ServletUtils.log("************************************************************* = " + msgToInt , ServletUtils.DebugLevels.DEBUG);
					timeFlag = true;
					String str = messages.getEndOfTimeResponse();
					Offer lastOffer = this.behavior.getFinalOffer(getHistory());
					str += " " + this.messages.GetNextMessage();
					Event e1= new Event(this.getID(), Event.EventClass.SEND_MESSAGE,Event.SubClass.OFFER_PROPOSE, str, (int) (700*game.getMultiplier()));
					resp.add(e1);
					Event e2 = new Event(this.getID(), Event.EventClass.SEND_OFFER, lastOffer, 0); 
					if(e2.getOffer() != null)
					{
						resp.add(e2);
						lastOfferSent = e2.getOffer();
						if(favorOfferIncoming)
						{
							favorOffer = lastOfferSent;
							favorOfferIncoming = false;
						}
					}
				}
				
			}
			//ServletUtils.log("i'm befor 90 TIME", ServletUtils.DebugLevels.DEBUG);
			// At 90 second, computer agent will send prompt for user to talk about preferences
			if (e.getMessage().equals("90") && this.getID() == History.OPPONENT_ID && 
					!this.messages.getUserToldMeHisPref()) 
			{
				//ServletUtils.log("TIME 90 seconds" , ServletUtils.DebugLevels.DEBUG);
				String str = "By the way, will you tell me a little about your preferences?";
				Event e1 = new Event(this.getID(), Event.EventClass.SEND_MESSAGE, Event.SubClass.PREF_REQUEST, str, (int) (1000*game.getMultiplier()));
				e1.setFlushable(false);
				resp.add(e1);
			}
			//this.getHistory().getOpponentHistory().addAll(resp);
			ServletUtils.log("Returned TIME RESPONE ", ServletUtils.DebugLevels.DEBUG);
			return resp;
		}

		
		/**
		 * ----------------------END OF TIME BLOCK-------------------------
		 */
	
		
	
	
		/**
		 * ----------------------START OF SEND OFFER BLOCK------------------
		 */
		
		
		
		//what to do when the player sends an offer
		if(e.getType().equals(Event.EventClass.SEND_OFFER))
		{
			//ServletUtils.log("Agent Normalized ordering: " + utils.getMyOrdering(), ServletUtils.DebugLevels.DEBUG);
			//ServletUtils.log("Optimal ordering: " + utils.getMinimaxOrdering(), ServletUtils.DebugLevels.DEBUG);

			boolean firstOffer = false;
			if (this.lastOfferReceived == null)
				firstOffer = true;
				
			Offer o = e.getOffer();//incoming offer
			this.lastOfferReceived = o; 

			boolean localFair = false;
			boolean totalFair = false;
			boolean localEqual = false;

			Offer allocated = behavior.getAllocated();//what we've already agreed on
			Offer conceded = behavior.getConceded();//what the agent has agreed on internally
			//ServletUtils.log("Allocated Agent Value: " + utils.myActualOfferValue(allocated), ServletUtils.DebugLevels.DEBUG);
			//ServletUtils.log("Conceded Agent Value: " + utils.myActualOfferValue(conceded), ServletUtils.DebugLevels.DEBUG);
			//ServletUtils.log("Offered Agent Value: " + utils.myActualOfferValue(o), ServletUtils.DebugLevels.DEBUG);
			int playerDiff = (utils.adversaryValue(o, utils.getMinimaxOrdering()) - utils.adversaryValue(allocated, utils.getMinimaxOrdering()));
			//ServletUtils.log("Player Difference: " + playerDiff, ServletUtils.DebugLevels.DEBUG);

			if(utils.myActualOfferValue(o) >= utils.myActualOfferValue(allocated))
			{//net positive (o is a better offer than allocated)
				int myValue = utils.myActualOfferValue(o) - utils.myActualOfferValue(allocated) + behavior.getAcceptMargin();
				//ServletUtils.log("My target: " + myValue, ServletUtils.DebugLevels.DEBUG);
				int opponentValue = utils.adversaryValue(o, utils.getMinimaxOrdering()) - utils.adversaryValue(allocated, utils.getMinimaxOrdering());
				if(myValue > opponentValue)
					localFair = true;//offer improvement is within one max value item of the same for me and my opponent
				else if (myValue == opponentValue && !firstOffer)
					localEqual = true;
			}

			if (behavior instanceof IAGOCompetitiveBehavior) 
			{
				totalFair = ((IAGOCompetitiveBehavior) behavior).acceptOffer(o);
			}
			else if(utils.myActualOfferValue(o) + behavior.getAcceptMargin() > utils.adversaryValue(o, utils.getMinimaxOrdering()))
				totalFair = true;//total offer still fair
			
			//totalFair too hard, so always set to true here
			totalFair = true;

			if (localFair && !totalFair)
			{
				String expr = expression.getSemiFairEmotion();
				if (expr != null)
				{
					Event eExpr = new Event(this.getID(), Event.EventClass.SEND_EXPRESSION, expr, 2000, (int) (700*game.getMultiplier()));
					resp.add(eExpr);
				}
				Event e0 = new Event(this.getID(), Event.EventClass.SEND_MESSAGE, Event.SubClass.OFFER_REJECT, messages.getSemiFairResponse(), (int) (700*game.getMultiplier()));
				resp.add(e0);
				behavior.updateAdverseEvents(1);
				Event e3 = new Event(this.getID(), Event.EventClass.SEND_OFFER, behavior.myNextOffer(getHistory()),  (int) (700*game.getMultiplier()));
				if(e3.getOffer() != null)
				{
					Event e1 = new Event(this.getID(), Event.EventClass.OFFER_IN_PROGRESS, 0);
					resp.add(e1);
					Event e2 = new Event(this.getID(), Event.EventClass.SEND_MESSAGE, Event.SubClass.OFFER_PROPOSE, messages.getProposalLang(getHistory(), game),  (int) (3000*game.getMultiplier()));
					resp.add(e2);
					this.lastOfferSent = e3.getOffer();
					if(favorOfferIncoming)
					{
						favorOffer = lastOfferSent;
						favorOfferIncoming = false;
					}
					resp.add(e3);
				}
			}
			//else if(localFair && totalFair)
			else if(localFair && totalFair)
			{
				String expr = expression.getFairEmotion();
				if (expr != null) 
				{
					Event eExpr = new Event(this.getID(), Event.EventClass.SEND_EXPRESSION, expr, 2000, (int) (700*game.getMultiplier()));
					resp.add(eExpr);
				}
				Event e0 = new Event(this.getID(), Event.EventClass.SEND_MESSAGE, Event.SubClass.OFFER_ACCEPT, messages.getVHAcceptLang(getHistory(), game), (int) (700*game.getMultiplier()));

				resp.add(e0);
				//ServletUtils.log("ACCEPTED OFFER!", ServletUtils.DebugLevels.DEBUG);
				behavior.updateAllocated(this.lastOfferReceived);

				Event eFinalize = new Event(this.getID(), Event.EventClass.FORMAL_ACCEPT, 0);
				if(utils.isFullOffer(o))
					resp.add(eFinalize);
			}
			else
			{
				String expr = expression.getUnfairEmotion();
				if (expr != null) 
				{
					Event eExpr = new Event(this.getID(), Event.EventClass.SEND_EXPRESSION, expr, 2000, (int) (700*game.getMultiplier()));
					resp.add(eExpr);
				}
				Event e0 = new Event(this.getID(), Event.EventClass.SEND_MESSAGE, Event.SubClass.OFFER_REJECT, messages.getVHRejectLang(getHistory(), game), (int) (700*game.getMultiplier()));
				//slightly alter the language
				if (localEqual)
					e0 = new Event(this.getID(), Event.EventClass.SEND_MESSAGE, Event.SubClass.OFFER_REJECT, messages.getVHEqualLang(), (int) (700*game.getMultiplier()));
				resp.add(e0);	
				behavior.updateAdverseEvents(1);
				Event e3 = new Event(this.getID(), Event.EventClass.SEND_OFFER, behavior.myNextOffer(getHistory()), (int) (700*game.getMultiplier()));
				if(e3.getOffer() != null)
				{
					Event e1 = new Event(this.getID(), Event.EventClass.OFFER_IN_PROGRESS, 0);
					resp.add(e1);
					Event e2 = new Event(this.getID(), Event.EventClass.SEND_MESSAGE, Event.SubClass.OFFER_PROPOSE, messages.getProposalLang(getHistory(), game),  (int) (3000*game.getMultiplier()));
					resp.add(e2);
					this.lastOfferSent = e3.getOffer();
					if(favorOfferIncoming)
					{
						favorOffer = lastOfferSent;
						favorOfferIncoming = false;
					}
					resp.add(e3);
				}
			}
			
			//this.getHistory().getOpponentHistory().addAll(resp);
			for (int i = 0; i < resp.size(); i++) {
				ServletUtils.log("RESP " + i + " " + resp.get(i), ServletUtils.DebugLevels.DEBUG);
			}
			return resp;
		}

		
		/**
		 * -----------------END OF SEND OFFER BLOCK------------------------
		 */
		
		/**
		 * -----------------START OF SEND MESSAGE BLOCK---------------------------
		 */
		
		
		
		//what to do when the player sends a message (including offer acceptances and rejections)
		if(e.getType().equals(Event.EventClass.SEND_MESSAGE))
		{
			//ServletUtils.log("I'm in SEND_MESSAGE in IAGOCORE", ServletUtils.DebugLevels.DEBUG);
			//ServletUtils.log("SUBCLASS ---> " + e.getSubClass() , ServletUtils.DebugLevels.DEBUG);
			Preference p;
			if (e.getPreference() == null) 
			{
				p = null;
			} else {
				p = new Preference(e.getPreference().getIssue1(), e.getPreference().getIssue2(), e.getPreference().getRelation(), e.getPreference().isQuery());
			}
			
			if (p != null && !p.isQuery()) //a preference was expressed
			{
				utils.addPref(p);
				if(utils.reconcileContradictions())
				{
					//we simply drop the oldest expressed preference until we are reconciled.  This is not the best method, as it may not be the the most efficient route.
					LinkedList<String> dropped = new LinkedList<String>();
					dropped.add(IAGOCoreMessage.prefToEnglish(utils.dequeuePref(), game));
					int overflowCount = 0;
					while(utils.reconcileContradictions() && overflowCount < 5)
					{
						dropped.add(IAGOCoreMessage.prefToEnglish(utils.dequeuePref(), game));
						overflowCount++;
					}
					String drop = "";
					for (String s: dropped)
						drop += "\"" + s + "\", and ";

					drop = drop.substring(0, drop.length() - 6);//remove last 'and'

					Event e1 = new Event(this.getID(), Event.EventClass.SEND_MESSAGE, Event.SubClass.CONFUSION,
							messages.getContradictionResponse(drop), (int) (2000*game.getMultiplier()));
					e1.setFlushable(false);
					resp.add(e1);
				}
			}

			String expr = expression.getExpression(getHistory());
			if (expr != null) 
			{
				Event e0 = new Event(this.getID(), Event.EventClass.SEND_EXPRESSION, expr, 2000, (int) (700*game.getMultiplier()));
				resp.add(e0);
			}

		
			Event e0 = messages.getVerboseMessageResponse(getHistory(), game, e);
			//ServletUtils.log("offer  befor enter e0", ServletUtils.DebugLevels.DEBUG);
			if (e0 != null) 
			{
				//ServletUtils.log("offer  e0 isn't null", ServletUtils.DebugLevels.DEBUG);
				if(e0.getSubClass() == Event.SubClass.OFFER_PROPOSE && e.getSubClass() == Event.SubClass.PREF_INFO) {
					//ServletUtils.log("e0 returned PREF_INFO", ServletUtils.DebugLevels.DEBUG);
					int issue1 = this.messages.getLastUserPref().getIssue1();
					int issue2 = this.messages.getLastUserPref().getIssue2();
					
					//ServletUtils.log("ISSUE1 = " + game.getIssuePluralText().get(issue1), ServletUtils.DebugLevels.DEBUG);
					//ServletUtils.log("ISSUE2 = " + game.getIssuePluralText().get(issue2), ServletUtils.DebugLevels.DEBUG);
					Event e2 = new Event(this.getID(), Event.EventClass.SEND_OFFER, this.behavior.getOfferAccordingToPref(issue1, issue2), (int) (700*game.getMultiplier()));
					if (e2.getOffer() != null) 
					{
						//ServletUtils.log("e2 isnt null", ServletUtils.DebugLevels.DEBUG);
						String s1 = messages.getProposalLang(getHistory(), game);
						Event e1 = new Event(this.getID(), Event.EventClass.OFFER_IN_PROGRESS, (int) (2000*game.getMultiplier()));
						resp.add(e0);
						resp.add(e1);
						resp.add(e2);
						this.lastOfferSent = e2.getOffer();
						
					} 
				}else if(e0.getSubClass() == Event.SubClass.OFFER_PROPOSE) {
					//ServletUtils.log("I'm in MESSAGES ----> OFFERPROPOSE", ServletUtils.DebugLevels.DEBUG);
					
					Event e2 = new Event(this.getID(), Event.EventClass.SEND_OFFER, this.behavior.myNextOffer(getHistory()), (int) (700*game.getMultiplier()));
					if (e2.getOffer() != null) 
					{
						String s1 = messages.GetNextMessage();
						Event e1 = new Event(this.getID(), Event.EventClass.OFFER_IN_PROGRESS, (int) (2000*game.getMultiplier()));
						resp.add(e0);
						resp.add(e1);
						resp.add(e2);
						this.lastOfferSent = e2.getOffer();
						
					} 
			  }else if(e0.getSubClass() == Event.SubClass.FAVOR_ACCEPT){
				  //ServletUtils.log("I'm in MESSAGES ----> FAVOR_ACCEPT", ServletUtils.DebugLevels.DEBUG);
				  Event e2 = new Event(this.getID(), Event.EventClass.SEND_OFFER, this.behavior.getFavorOffer(), (int) (700*game.getMultiplier()));
					if (e2.getOffer() != null) 
					{
						String s1 = messages.GetNextMessage();
						Event e1 = new Event(this.getID(), Event.EventClass.OFFER_IN_PROGRESS, (int) (2000*game.getMultiplier()));
						resp.add(e0);
						resp.add(e1);
						resp.add(e2);
						this.lastOfferSent = e2.getOffer();
						this.modifyOfferLedger(1);
						if(favorOfferIncoming)
						{
							favorOffer = lastOfferSent;
							favorOfferIncoming = false;
						}
					} 
				} else if(e0.getSubClass() == Event.SubClass.FAVOR_RETURN) {
					 //ServletUtils.log("I'm in MESSAGES ----> FAVOR_RUTERN", ServletUtils.DebugLevels.DEBUG);
					  Event e2 = new Event(this.getID(), Event.EventClass.SEND_OFFER, this.behavior.getFavorOffer(), (int) (700*game.getMultiplier()));
						if (e2.getOffer() != null) 
						{
							ServletUtils.log("Favor Offer e2 " + e2.getOffer(), ServletUtils.DebugLevels.DEBUG);
							String s1 = messages.GetNextMessage();
							Event e1 = new Event(this.getID(), Event.EventClass.OFFER_IN_PROGRESS, (int) (2000*game.getMultiplier()));
							resp.add(e0);
							resp.add(e1);
							resp.add(e2);
							this.lastOfferSent = e2.getOffer();
							this.modifyOfferLedger(1);
							if(favorOfferIncoming)
							{
								favorOffer = lastOfferSent;
								favorOfferIncoming = false;
							}
						} else {
							String next = "Thank you, but I can't find anything worth for me, maybe you'll send a favor offer?";
							Event e1 = new Event (this.getID(), Event.EventClass.SEND_MESSAGE, Event.SubClass.FAVOR_REQUEST,next, 0);
							resp.add(e1);
						}
				} else {
					//ServletUtils.log("else add e0", ServletUtils.DebugLevels.DEBUG);
					resp.add(e0);
				}
		}


//			if(behavior instanceof IAGOCompetitiveBehavior && e.getSubClass() == Event.SubClass.BATNA_INFO)
//			{
//				((IAGOCompetitiveBehavior)behavior).resetConcessionCurve();
//			}
//
//			boolean offerRequested = (e.getSubClass() == Event.SubClass.OFFER_REQUEST_NEG || e.getSubClass() == Event.SubClass.OFFER_REQUEST_POS);
//						
//			if(offerRequested)
//			{	
//				if(utils.adversaryBATNA + utils.myPresentedBATNA > utils.getMaxPossiblePoints()) 
//				{
//					String walkAway = "Actually, I won't be able to offer you anything that gives you " + utils.adversaryBATNA + " points. I think I'm going to have to walk "
//							+ "away, unless you were lying.";
//					Event e2 = new Event(this.getID(), Event.EventClass.SEND_MESSAGE, Event.SubClass.THREAT_NEG, utils.adversaryBATNA, walkAway, (int) (2000*game.getMultiplier()));
//					resp.add(e2);
//				}
//				else 
//				{
//					Event e2 = new Event(this.getID(), Event.EventClass.SEND_OFFER, behavior.myNextOffer(getHistory()), (int) (3000*game.getMultiplier()));
//					if (e2.getOffer() == null) 
//					{
//						String response = "Actually, I'm having trouble finding an offer that works for both of us.";
//						Event e4 = new Event(this.getID(), Event.EventClass.SEND_MESSAGE, Event.SubClass.THREAT_POS, response, (int) (2000*game.getMultiplier()));
//						if (utils.adversaryBATNA != -1) 
//						{
//							response += " Are you sure you can't accept anything less than " + utils.adversaryBATNA + " points?";
//							e4 = new Event(this.getID(), Event.EventClass.SEND_MESSAGE, Event.SubClass.BATNA_REQUEST, utils.adversaryBATNA, response, (int) (2000*game.getMultiplier()));
//						}					
//						resp.add(e4);
//						ServletUtils.log("Null Offer", ServletUtils.DebugLevels.DEBUG);
//					}
//					else 
//					{
//						Event e3 = new Event(this.getID(), Event.EventClass.OFFER_IN_PROGRESS, 0);
//						resp.add(e3);
//
//						Event e4 = new Event(this.getID(), Event.EventClass.SEND_MESSAGE, Event.SubClass.OFFER_PROPOSE, messages.getProposalLang(getHistory(), game), (int) (1000*game.getMultiplier()));
//						resp.add(e4);
//						this.lastOfferSent = e2.getOffer();
//						if(favorOfferIncoming)
//						{
//							favorOffer = lastOfferSent;
//							favorOfferIncoming = false;
//						}
//						resp.add(e2);
//
//					}
//				}
//			}

			if(e.getSubClass() == Event.SubClass.OFFER_ACCEPT)//offer accepted
			{
				if(this.lastOfferSent != null)
				{
					behavior.updateAllocated(this.lastOfferSent);
					if(lastOfferSent.equals(favorOffer))
						modifyLedger();
					else
						myLedger.offerLedger = 0;
				}

				Event e2 = new Event(this.getID(), Event.EventClass.SEND_OFFER, behavior.getAcceptOfferFollowup(getHistory()), (int) (3000*game.getMultiplier()));
				if(e2.getOffer() != null)
				{
					Event e3 = new Event(this.getID(), Event.EventClass.OFFER_IN_PROGRESS, 0);
					resp.add(e3);
					Event e4 = new Event(this.getID(), Event.EventClass.SEND_MESSAGE, Event.SubClass.OFFER_PROPOSE, messages.getProposalLang(getHistory(), game), (int) (1000*game.getMultiplier()));
					resp.add(e4);
					this.lastOfferSent = e2.getOffer();
					if(favorOfferIncoming)
					{
						favorOffer = lastOfferSent;
						favorOfferIncoming = false;
					}
					resp.add(e2);		
				}
			}

			if(e.getSubClass() == Event.SubClass.OFFER_REJECT)//offer rejected
			{	
				//ServletUtils.log("I'm in offer reject in IAGOCORE", ServletUtils.DebugLevels.DEBUG);
				if(this.behavior.isCalledToOfferByPref()==true)
				{
					
					this.behavior.setPlayerLieThreshHold((float) (this.behavior.getPlayerLieThreshHold()+0.3));
					//ServletUtils.log("THRESHOLD" + this.behavior.getPlayerLieThreshHold(), ServletUtils.DebugLevels.WARN);
					this.behavior.setLedgerBehavior(currentGameCount);
					//ServletUtils.log("LEDGER" + this.behavior.getLb(), ServletUtils.DebugLevels.WARN);
					this.behavior.setCalledToOfferByPref(false);
				}
				this.numRejectedByUser++;
				behavior.updateAdverseEvents(1);
				myLedger.offerLedger = 0;
				Offer off = behavior.getRejectOfferFollowup(getHistory());
				if(off!=null) {
					if(this.numRejectedByUser < 2) {
						
						ServletUtils.log("OFF " + off , ServletUtils.DebugLevels.DEBUG);
						Event e2 = new Event(this.getID(), Event.EventClass.SEND_OFFER, off, (int) (3000*game.getMultiplier()));
						ServletUtils.log("BEHAVIOR LB " + this.behavior.getLb() + " MESSAGE LB " + this.messages.getLb() , ServletUtils.DebugLevels.DEBUG);
						if(e2.getOffer() != null)
						{
							//ServletUtils.log("rejected e2 isn't NULL", ServletUtils.DebugLevels.DEBUG);
							Event e3 = new Event(this.getID(), Event.EventClass.OFFER_IN_PROGRESS, 0);
							resp.add(e3);
							Event e4 = new Event(this.getID(), Event.EventClass.SEND_MESSAGE, Event.SubClass.OFFER_PROPOSE, messages.getProposalLang(getHistory(), game), (int) (1000*game.getMultiplier()));
							resp.add(e4);
							this.lastOfferSent = e2.getOffer();
							if(favorOfferIncoming)
							{
								favorOffer = lastOfferSent;
								favorOfferIncoming = false;
							}

							resp.add(e2);	
						}
					} else {
						Event e2 = new Event(this.getID(), Event.EventClass.SEND_OFFER, behavior.myNextOffer(getHistory()), (int) (3000*game.getMultiplier()));
						if(e2.getOffer() != null)
						{
							Event e3 = new Event(this.getID(), Event.EventClass.OFFER_IN_PROGRESS, 0);
							String str = this.messages.GetNextMessage();
							resp.add(e3);
							Event e4 = new Event(this.getID(), Event.EventClass.SEND_MESSAGE, Event.SubClass.OFFER_PROPOSE, str, (int) (1000*game.getMultiplier()));
							resp.add(e4);
							this.lastOfferSent = e2.getOffer();
							if(favorOfferIncoming)
							{
								favorOffer = lastOfferSent;
								favorOfferIncoming = false;
							}

							resp.add(e2);
						}
					}
				}
				else {
					Event pref = new Event(this.getID(), Event.EventClass.SEND_MESSAGE, Event.SubClass.PREF_INFO, "Would you like to tell me more about your preferences? don't be so shy", 1000);
					Event askForOffer = new Event(this.getID(), Event.EventClass.SEND_MESSAGE, Event.SubClass.OFFER_REQUEST_POS, "Or maybe make an offer..?", 1000);
					
					resp.add(pref);
					resp.add(askForOffer);
				}
				
				for(int i = 0; i < resp.size(); i++) {
					//ServletUtils.log("RESPPPPPP " + (i + 1) +" " + resp.get(i).getMessage(), ServletUtils.DebugLevels.DEBUG);
				}
				//this.getHistory().getOpponentHistory().addAll(resp);
				return resp;
			
			}
		}
		for(int i = 0; i < resp.size(); i++) {
			//ServletUtils.log("RESPPPPPP " + (i + 1) +" " + resp.get(i).getMessage(), ServletUtils.DebugLevels.DEBUG);
		}
		//this.getHistory().getOpponentHistory().addAll(resp);
		return resp;
	}

	/**
	 * Every agent needs a name to select the art that will be used. Currently only 4 names are supported: Brad, Ellie, Rens, and Laura.
	 * Note: This is NOT the name that users will see when they negotiate with the agent.
	 * @return the name of the character model to be used. If this does not match "Brad", "Ellie", "Rens", or "Laura", one of those names
	 * will be selected as a default. Please use one of those names.
	 */
	@Override
	public abstract String getArtName();
	
	/**
	 * This is the method that dictates what users read when they negotiate with your agent. An agent developer can implement
	 * this method to say anything they want.
	 * @return The message that users see when negotiating with the agent.
	 */
}
