
package edu.usc.ict.iago.agent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

//import org.apache.catalina.comet.CometEvent.EventType;

import edu.usc.ict.iago.agent.RepeatedFavorBehavior.LedgerBehavior;
import edu.usc.ict.iago.utils.Event;
import edu.usc.ict.iago.utils.GameSpec;
import edu.usc.ict.iago.utils.History;
import edu.usc.ict.iago.utils.MessagePolicy;
import edu.usc.ict.iago.utils.Offer;
import edu.usc.ict.iago.utils.Preference;
import edu.usc.ict.iago.utils.Preference.Relation;
import edu.usc.ict.iago.utils.ServletUtils;



public class RepeatedFavorMessage extends IAGOCoreMessage implements MessagePolicy {
	
	protected final String[] proposal = {"I think this deal is good for the both of us.", 
			"I think you'll find this offer to be satisfactory.", 
			"I think this arrangement is fair.", 
			"I think this deal will interest you.",
	"Please consider this deal?"};

	protected final String[] acceptResponse = {
			"Great!",
			"Wonderful!",
			"I'm glad we could come to an agreement!",
			"You just made my day!",		
	"Sounds good!"};

	protected final String[] rejectResponse = {
			"Oh that's too bad.",
			"Ah well, perhaps another time.",
			"Ok, maybe something different next time.",
	"Alright."};

	protected final String[] vhReject = {
			"I'm sorry, but I don't think that's fair to me.",
			"Apologies, but that won't work for me.",
			"Perhaps we should spend more time finding a solution that's good for us both...",
	"I won't be able to accept that.  So sorry. :("};

	protected final String[] vhAccept = {
			"Your offer is good!",
			"That seems like a good deal.",
			"Yes. This deal will work.",
			"This offer is great.On another topic...you know that I orderd a chicken and an egg from Amazon. I'll let you know how it was... "};

	protected final String[] vhWaiting = {
			"Hello? Are you still there?",
			"No rush, but are you going to send me an offer?",
			"Can I do anything to help us reach a deal that's good for us both?",
			"I'm sorry, but are you still there?",
	"Can I provide more information to help us reach consensus?",
	"Would you please make an offer?","We should try harder to find a deal that benefits us both.", null};
	
	protected final String[] vhWithHolding = {
			"I don't think it best to reveal my intentions yet. Maybe if you did first...",
			"Perhaps instead of my prefernces I will tell you a joke? " + "Singing in the shower is fun until you get soap in your mouth. Then it's a soap opera.",
			"You know that I orderd a chicken and an egg from Amazon. I'll let you know... " + "Meanwhile, can you tell me about your prefernces?",
			"You know that I'm on seafood diet, I see food and I eat it, but enough talking about me, please tell me what you prefer!"		
	};
	
	public String getHoldingMes() {
		Random r = new Random();
		String str = vhWithHolding[Math.abs(r.nextInt() % 5)];
		return str;
		
	}
	
	
	
	private String[] vhIdleQuestions = {"Knock knock..? Are you still there?", "Hello? Are you still here??", "Lonely...I am so lonely.."};	//P++ will only use each of these messages once at the game's start before trying other messages
	
	
	private boolean isWithholding;
	private boolean lying;
	private LedgerBehavior lb = RepeatedFavorBehavior.lb;
	private AgentUtilsExtension utils;
	private RepeatedFavorBehavior behavior;
	public Offer nextOffer;
	public LinkedList <Preference> lastPref;
	private boolean userToldMeHisPref = false;
	public boolean isWithholding() {
		return isWithholding;
	}

	public void setWithholding(boolean isWithholding) {
		this.isWithholding = isWithholding;
	}

	public boolean isLying() {
		return lying;
	}
	protected boolean getUserToldMeHisPref() {
		return this.userToldMeHisPref;
	}

	public void setLying(boolean lying) {
		this.lying = lying;
	}
	
	public RepeatedFavorBehavior.LedgerBehavior getLb() {
		return lb;
	}

	public void setLb(RepeatedFavorBehavior.LedgerBehavior lb) {
		this.lb = lb;
	}

	public RepeatedFavorBehavior getBehavior() {
		return behavior;
	}
	@Override
	public void setBehavior(IAGOCoreBehavior behavior) {
		this.behavior = (RepeatedFavorBehavior) behavior;
	}
	
	@Override
	public Preference getLastUserPref() {
		return this.lastPref.getLast();
	}

	private int opponentBATNA = -1;
	private int agentID;
	private GameSpec gs;
	public Offer allocated;
	private String nextBehaviorMessage;

	protected void setUtils(AgentUtilsExtension utils)
	{
		this.utils = utils;
		opponentBATNA = -1;
		agentID = utils.getID();
		allocated = new Offer(gs.getNumberIssues());
		for(int i = 0; i < gs.getNumberIssues(); i++)
		{
			int[] init = {0, gs.getIssueQuantities().get(i), 0};
			allocated.setItem(i, init);
		}
		this.setLb(this.behavior.getLb());
		this.lastPref = new LinkedList<>();
	}
	
	/***
	 * Constructor for a positive message. The resulting agent can be either withholding or open, lying or honest.
	 * @param isWithholding a boolean representing whether an agent is withholding (true if yes, false if no)
	 * @param lying a boolean representing whether an agent will tell a BATNA lie
	 * @param lb an enum representing how the agent talks about favors
	 * @param gs the gameSpec
	 */
	public RepeatedFavorMessage(boolean isWithholding, boolean lying, RepeatedFavorBehavior.LedgerBehavior lb, GameSpec gs) 
	{
		this.isWithholding = isWithholding;
		this.lying = lying;
		this.lb = lb;
		this.gs = gs;
		
		this.isWithholding = isWithholding;
		this.lying = lying;
		this.lb = lb;
		this.gs = gs;
	}

	@Deprecated
	public void updateOrderings (ArrayList<ArrayList<Integer>> orderings)
	{
		//this.orderings = orderings;
		return;
	}

	public String getProposalLang(History history, GameSpec game){
		return proposal[(int)(Math.random()*proposal.length)];
	}

	public String getAcceptLang(History history, GameSpec game){
		return acceptResponse[(int)(Math.random()*acceptResponse.length)];
	}

	public String getRejectLang(History history, GameSpec game){
		return rejectResponse[(int)(Math.random()*rejectResponse.length)];
	}

	public String getVHAcceptLang(History history, GameSpec game){
		return vhAccept[(int)(Math.random()*vhAccept.length)];
	}

	public String getVHRejectLang(History history, GameSpec game){
		return vhReject[(int)(Math.random()*vhReject.length)];
	}

	public String getWaitingLang(History history, GameSpec game){
		if(vhIdleQuestions.length != 0) {									//if the questions in this array haven't all been removed, pick another one to use
			int rand = (int)(Math.random()*vhIdleQuestions.length);
			String randQ = vhIdleQuestions[rand];
			ArrayList<String> temp = new ArrayList<String>((List<String>) Arrays.asList(vhIdleQuestions));
			temp.remove(rand);
			Object[] temp2 = temp.toArray();
			vhIdleQuestions = Arrays.copyOf(temp2, temp2.length, String[].class);		//remove questions from this array once used (will eventually be empty at which point P++ randomly picks from vhWaiting below)
			return randQ;
		}
		return vhWaiting[(int)(Math.random()*vhWaiting.length)];
	}

	public boolean getLying(GameSpec game) {
		return lying;
	}

	public String getEmotionResponse(History history, GameSpec game, Event e) {
		
		String[] responseForSad = {"I'm sorry for you to be sad, let's try somthing else", "Maybe we should try better", "60 Seconds of sadness"
				+ "are one minute you are not happy! I'll do my best for us both to be satisfied", "Your sad Emoji broke my robot heart..",
				"I am an emotional robot, don't to this to me..."};
		String[] responseForHappy = {"If it makes you happy... it can't be that bad :)", 
				"i'm gald it please you! Let's continue that way!", "It's great your smiling :)", "I love seeing you smile, it makes me feel great!","You are beautiful when you are smiling! Do not let anyone take the smile off your face",
				"I see you follow the saying, smile to the world and the world will smile to you!", "If you are already smiling, I wnat you to smile a little bit more so here a joke for you: /n" +
				"A man on a date wonders if he'll get lucky. A woman already knows."};
		String[] responseForNeutral = {"What can I say about your neutral Emoji.. let's try better", "Mmmmm Maybe we can do better", 
				"Well it seems you don't have much opinion by your last Emoji.."};
		String[] responseForSurprised = {"Did I surprised you? :)", "Are you more surprised than Switzerland won France in last EURO 1/8 Final? ", 
				"I'm happy you were surprised! Hope it's a good thing :) ", "Your suprised face reminds me of this time when I discovered my roommate was stealing from driving school. \n" + "But to be honest, I should have seen all the signs :)"};
		String[] responseForAngry = {"Did I do something wrong?", "Ok, your angry, i'm sure I can do something to make you fell better..", 
				"I'm sorry for you to be angry, maybe i'll try somthing else.."};
		
		Random rand = new Random();
		if (e.getType() != Event.EventClass.SEND_EXPRESSION)
			throw new UnsupportedOperationException("The last event wasn't an expresion--this method is inappropriate.");
		if(e.getMessage().equals("sad")) 
			return responseForSad[Math.abs(rand.nextInt() %  responseForSad.length)];
		else if (e.getMessage().equals("angry"))
			return responseForAngry[Math.abs(rand.nextInt() %  responseForAngry.length)];
		else if(e.getMessage().equals("happy"))
			return responseForHappy[Math.abs(rand.nextInt() %  responseForHappy.length)];
		else if(e.getMessage().equals("surprised"))
			return responseForSurprised[Math.abs(rand.nextInt() %  responseForSurprised.length)];
		else if(e.getMessage().equals("neutral"))
			return responseForNeutral[Math.abs(rand.nextInt() %  responseForNeutral.length)];
		return "I don't know what face you just made!";
	}

	protected String getEndOfTimeResponse() {
		return "We're almost out of time!  Accept this quickly!";
	}

	protected String getSemiFairResponse() {
		return "Unfortunately, I cannot accept.  But that's getting close to being fair.";
	}

	protected String getContradictionResponse(String drop) {
		return "I'm sorry.  I must be misunderstanding.  Earlier, you said: " + drop + " Was that not correct?";
	}

	@Override
	public String getMessageResponse(History history, GameSpec game) {
		return null;
	}
	
	public boolean equalsPref(Preference a, Preference b) {
		if(a.getIssue1()!=b.getIssue1()) {
			return false;
		}
		if(a.getIssue2()!=b.getIssue2()) {
			return false;
		}
		if(a.getRelation()!=b.getRelation()) {
			return false;
		}
		if(a.isQuery()!=b.isQuery()) {
			return false;
		}
		return true;
	}
	
	
	@Override
	protected Event getFavorBehavior(History history, GameSpec game, Event e)
	{
		this.behavior.setLedgerBehavior(this.behavior.getCurrGame());
		this.lb = this.behavior.lb;
		if (lb != LedgerBehavior.NONE && utils.isImportantGame() && this.behavior.getRealMargin() >= 5)
			return new Event(agentID, Event.EventClass.SEND_MESSAGE, Event.SubClass.FAVOR_REQUEST, 
					"thank you",
					(int) (1000*game.getMultiplier()));
		else if (lb != LedgerBehavior.NONE && utils.getLedger() > 0 && game.getTotalTime() < 120)
			return new Event(agentID, Event.EventClass.SEND_MESSAGE, Event.SubClass.FAVOR_REQUEST, 
					"There is not a lot of time to the end of the game and you still owe me a favor, so it will be wonderfull if you could pay me back.",
					(int) (1000*game.getMultiplier()));
		else if (lb == LedgerBehavior.FAIR && utils.getLedger() < 0)
		{
			utils.modifyVerbalLedger(1);
			return new Event(agentID, Event.EventClass.SEND_MESSAGE, Event.SubClass.FAVOR_ACCEPT, 
					"I think I still owe you a favor!  Let me just pay that back for you.",
					(int) (1000*game.getMultiplier()));
		}
		return null;
	}
	
	public void setNextMessage(String next) {
		this.nextBehaviorMessage = next;
	}
	@Override
	public String GetNextMessage() {
		return this.nextBehaviorMessage;
	}

	public Event getVerboseMessageResponse(History history, GameSpec game, Event ePrime) {
	
		int randomDelay = new Random().nextInt(2000) + 3000;			//causes message delays to vary in a range between 3-5 seconds to appear more human-like
		int delay = (int) (randomDelay*game.getMultiplier()); 
		int value = -1;
		int issue1 = -1;
		int issue2 = -1;
		Relation relation = null;
		boolean isQuery = false;
		//this.nextOffer = new Offer(gs.getNumberIssues());
		
		if(this.lb.equals(LedgerBehavior.LIMITED)) {
			this.isWithholding = true;
		} else
			this.isWithholding = false;

		if (ePrime.getType() == Event.EventClass.SEND_EXPRESSION && !game.isMultiAgent())
		{
			String str = getEmotionResponse(history, game, ePrime);
			Event resp = new Event(agentID, Event.EventClass.SEND_MESSAGE, Event.SubClass.GENERIC_POS, str, delay); 
			return resp;
		} 
		else if (ePrime.getType() == Event.EventClass.SEND_EXPRESSION) 
		{
			return null; // Disables responding to emotions
		}

		if (ePrime.getType() == Event.EventClass.TIME) 
		{
			String str = getWaitingLang(history, game);
			Event resp = new Event(agentID, Event.EventClass.SEND_MESSAGE, Event.SubClass.TIMING, str, delay);
			return resp; 
		}
		
		//make sure we have a message
		if (ePrime.getType() != Event.EventClass.SEND_MESSAGE)
			return null;

		Preference p = ePrime.getPreference();
		
		if (p != null) //a preference was expressed
		{
			//ServletUtils.log("----------->>> p 1 = " + this.gs.getIssuePluralText().get(p.getIssue1()) , ServletUtils.DebugLevels.DEBUG);
			//ServletUtils.log("----------->>> p 2 = " + this.gs.getIssuePluralText().get(p.getIssue2()) , ServletUtils.DebugLevels.DEBUG);
			String str = "";
			if(p.isQuery() && this.isWithholding) 
			{ //asked about preferences

				str = getHoldingMes();
				Event resp = new Event(agentID, Event.EventClass.SEND_MESSAGE, Event.SubClass.PREF_REQUEST, str, delay);
				return resp;
			} 
			else if(p.isQuery() && lb != LedgerBehavior.BETRAYING) {
				issue1 = utils.findMyItemIndex(game, 1);
				issue2 = -1;
				isQuery = false;
				str = "Because you asked so nice, and you look so nice from the inside of the computer...\n"
						+ "What i want the most is " + game.getIssuePluralText().get(issue1) 
						+ "Would you like to make an offer?";
				Event resp = new Event(agentID, Event.EventClass.SEND_MESSAGE, Event.SubClass.PREF_INFO, str, delay);
			}
			else if(lb == LedgerBehavior.BETRAYING) {
				Preference randomPref = this.utils.randomPref();
				Relation r = randomPref.getRelation();
				if(r == Relation.GREATER_THAN) {
					str = "I like " + game.getIssuePluralText().get(randomPref.getIssue1()) + 
							"more than " + game.getIssuePluralText().get(randomPref.getIssue2());
					Event resp = new Event(agentID, Event.EventClass.SEND_MESSAGE, Event.SubClass.PREF_INFO, str, delay);
					return resp;
				} else if(r == Relation.LESS_THAN) {
					str = "I like " + game.getIssuePluralText().get(randomPref.getIssue2()) + 
							"more than " + game.getIssuePluralText().get(randomPref.getIssue1());
					Event resp = new Event(agentID, Event.EventClass.SEND_MESSAGE, Event.SubClass.PREF_INFO, str, delay);
					return resp;
				}
			}
		} 
		else 
		{
			//ServletUtils.log("No preference detected in user message.", ServletUtils.DebugLevels.DEBUG);
		}

		//details for each response
		Event.SubClass sc = Event.SubClass.NONE;

		int best = utils.findAdversaryItemIndex(game, 1);
		int worst = utils.findAdversaryItemIndex(game, game.getNumberIssues());
		String str = "";



		int offerCount = 0;
		for(Event e: history.getHistory())
			if(e.getType() == Event.EventClass.SEND_OFFER)
				offerCount++;

		boolean isFull = true;
		Event lastOffer = null;
		if (offerCount > 0)
		{
			int index = history.getHistory().size() - 1;
			lastOffer = history.getHistory().get(index);
			while (lastOffer.getType() != Event.EventClass.SEND_OFFER)
			{
				index--;
				lastOffer = history.getHistory().get(index);
			}
			Offer o = lastOffer.getOffer();
			for (int i = 0; i < o.getIssueCount(); i++)
			{
				if(o.getItem(i)[1] != 0)//some undecided items
					isFull = false;
			}
		}
		
		//MAIN RESPONSE
		switch(ePrime.getSubClass())
		{
		case GENERIC_POS: 
			sc = Event.SubClass.PREF_INFO;
			//ServletUtils.log("The user sent GENERIC POS", ServletUtils.DebugLevels.DEBUG);
			if(best < 0) 
			{ // We do not have any guesses as to their favorite
				str = "I agree!  What is your favorite item?";
				isQuery = true;
				relation = Relation.BEST;
				//ServletUtils.log("The user doesn't have best", ServletUtils.DebugLevels.DEBUG);
			}
			else 
			{
				str = "I agree!  Why don't we make sure you get your favorite item, and I get mine?  Yours is " + game.getIssuePluralText().get(best) + ", right?";
				issue1 = best;
				relation = Relation.BEST;
				isQuery = true;
				//ServletUtils.log("The user have best", ServletUtils.DebugLevels.DEBUG);
			}
			break;
	
		case GENERIC_NEG:
			str = "I'm sorry, have I done something wrong?  I'm just trying to make sure we both get the things that make us the most happy.";
			//ServletUtils.log("The user sent GENERIC NEG", ServletUtils.DebugLevels.DEBUG);
			sc = Event.SubClass.GENERIC_NEG;

			if(!isFull)
				str += "  Besides, what about the rest of the undecided items?";

			break;
		case OFFER_REJECT:
			sc = Event.SubClass.GENERIC_POS;
			//ServletUtils.log("I'm in offer reject in messageVerbose", ServletUtils.DebugLevels.DEBUG);
			if (offerCount > 0)
			{
				int avgPlayerValue = (Math.abs(utils.adversaryValueMax(lastOffer.getOffer()) - utils.adversaryValueMin(lastOffer.getOffer())))/2;
				if (Math.abs(utils.myActualOfferValue(lastOffer.getOffer()) - avgPlayerValue) > game.getNumberIssues() * 2)
				{
					str =  "Ok, I understand.  I do wish we could come up with something that is a more even split though.";
					if (best >= 0 && worst >= 0) 
					{
						str += "  Isn't it true that you like " + game.getIssuePluralText().get(best) + " best and " + game.getIssuePluralText().get(worst) + " least?";
						sc = Event.SubClass.PREF_REQUEST;
						issue1 = best;
						relation = Relation.BEST;
						isQuery = true;
						
					}
				}
				else
					str = "Ok, I understand.  This seems like a fairly even split.";
			}
			else
				str = "But... there haven't even been any offers!";

			if(!isFull)
				str += " What about the rest of the undecided items..? ";
			
			break;	
		case TIMING: //note: agent responds to this, but this event no longer is a user available action
			sc = Event.SubClass.GENERIC_POS;
			int time = 0;
			int index = history.getHistory().size() - 1;
			index = index < 1 ? 1 : index;
			if (history.getHistory().size() > 1)
			{
				Event lastTime = history.getHistory().get(index);
				while (lastTime.getType() != Event.EventClass.TIME && index > 0)
				{
					index--;
					lastTime = history.getHistory().get(index);
				}

				if(lastTime == null || lastTime.getMessage() == null || lastTime.getMessage().equals(""))
					break;
				time = (int)Double.parseDouble(lastTime.getMessage());
				time = game.getTotalTime() - time;

				int min = time / 60;
				int sec = time % 60;

				str = "There is currently " + min + " minute" + (min == 1 ? "" : "s") + " and " + sec + " seconds remaining.";

				if (min > 0)
				{
					str += "  Don't worry.  We've still got a bit more time to negotiate.";
				}
				else
				{
					int secondBest = utils.findAdversaryItemIndex(game, 2);
					int suggest2 = best >= 0 ? best : (int)(Math.random() * game.getNumberIssues());
					int suggest3 = secondBest >= 0 ? secondBest : (int)(Math.random() * game.getNumberIssues());
					if (suggest3 == suggest2)
						suggest3  = (suggest3 + 1) % game.getNumberIssues();
					str += "  AHH!  You're right!  Let's just split it like this: you get all the " + game.getIssuePluralText().get(suggest2) + " and the " 
							+ game.getIssuePluralText().get(suggest3) + " and I get the remainder.";
					sc = Event.SubClass.OFFER_PROPOSE;
				}
			}
			break;
		case OFFER_REQUEST_POS:
			if (this.lastPref == null) {
				str = "Maybe tell me more about your preferences..? So I could send you a good offer";
				sc = Event.SubClass.PREF_REQUEST;
				break;
			} else {
				str = "Here is the deal i cooked for you..:";
				sc = Event.SubClass.OFFER_PROPOSE;
				break;
			}
			
		case OFFER_REQUEST_NEG:
			str = "Alright, what do you think of this?";
			sc = Event.SubClass.OFFER_PROPOSE; 
			break;
		case THREAT_POS: // "I'm sorry but I think I'm going to have to walk away.");
			int loweredBATNA1 = utils.lowerBATNA(utils.myPresentedBATNA);
			this.behavior.setIsThreatMe(true);
			if(loweredBATNA1 != utils.myPresentedBATNA) 
			{
				utils.myPresentedBATNA = utils.lowerBATNA(utils.myPresentedBATNA);
				str = "Hey, maybe if i'll get " + utils.myPresentedBATNA + " points will be better for you?";
				value = utils.myPresentedBATNA;
				sc = Event.SubClass.BATNA_INFO;
			}
			else if (!utils.conflictBATNA(utils.myPresentedBATNA, opponentBATNA))
			{
				str = "Please don't walk away, i'm sure we could do better than that! Please tell me what do you like?";
				sc = Event.SubClass.BATNA_REQUEST;
			}
			else {
				str = "Oh well, I guess we really should walk away. Are you sure you can't accept anything less than " + utils.adversaryBATNA + " points?";
				sc = Event.SubClass.BATNA_REQUEST;
			}
			break;
		case THREAT_NEG: // "This is a warning, I'm about to walk away."
			int loweredBATNA = utils.lowerBATNA(utils.myPresentedBATNA);
			this.behavior.setIsThreatMe(true);
			if(loweredBATNA != utils.myPresentedBATNA) 
			{
				utils.myPresentedBATNA = utils.lowerBATNA(utils.myPresentedBATNA);
				str = "Maybe I can get " + utils.myPresentedBATNA + " points, what do you say?";
				value = utils.myPresentedBATNA;
				sc = Event.SubClass.BATNA_INFO;
			}
			else if (!utils.conflictBATNA(utils.myPresentedBATNA, opponentBATNA))
			{
				str = "Please don't go yet! Maybe we can still make a deal. Would you mind reminding me what you would like?";
				sc = Event.SubClass.BATNA_REQUEST;
			}
			else {
				str = "Oh well, I guess we really should walk away. Are you sure you can't accept anything less than " + utils.adversaryBATNA + " points?";
				sc = Event.SubClass.BATNA_REQUEST;
			}
			break;
		case PREF_INFO:
			if(userToldMeHisPref) {
				userToldMeHisPref = false;
			}
			if(this.lastPref!=null) {
				for (Preference mine : this.lastPref) {
					if(this.equalsPref(mine, p)) {
						this.userToldMeHisPref = true;
					}
				}
			}
			if(this.lastPref != null && !p.isQuery()) {
				//ServletUtils.log("last Pref != null " + p.isQuery() , ServletUtils.DebugLevels.DEBUG);
				if(this.userToldMeHisPref) {
					//ServletUtils.log("p.getISSUE1 =  =  "   + game.getIssuePluralText().get(p.getIssue1()) , ServletUtils.DebugLevels.DEBUG);
					//ServletUtils.log("p.getISSUE2 =  =  "   + game.getIssuePluralText().get(p.getIssue2()) , ServletUtils.DebugLevels.DEBUG);
					//ServletUtils.log("lastPREF1 =  =  =  "   + game.getIssuePluralText().get(this.lastPref.getLast().getIssue1()) , ServletUtils.DebugLevels.DEBUG);
					//ServletUtils.log("lastPREF2 =  =  =  "   + game.getIssuePluralText().get(this.lastPref.getLast().getIssue2()) , ServletUtils.DebugLevels.DEBUG);
					if(p.getIssue2() == -1) {
						str = "You already told me about " + game.getIssuePluralText().get(p.getIssue1());
					}
					else
						str = "You already told me about " + game.getIssuePluralText().get(p.getIssue1()) + " and " + game.getIssuePluralText().get(p.getIssue2());
					str+= " Would you mind telling me about other items..?";
					sc = Event.SubClass.GENERIC_POS;
					//ServletUtils.log("USER TOLD ME HIS  "   + this.userToldMeHisPref , ServletUtils.DebugLevels.DEBUG);
					
					this.userToldMeHisPref = false;
					//ServletUtils.log("USER TOLD ME HIS  "   + this.userToldMeHisPref , ServletUtils.DebugLevels.DEBUG);
					break;
				}
			}
			
			//ServletUtils.log("i'm in PREF_INFO" , ServletUtils.DebugLevels.DEBUG);
			//ServletUtils.log("isQuery??? " + p.isQuery() , ServletUtils.DebugLevels.DEBUG);
			if (p != null && !p.isQuery()) //a preference was expressed
			{
				this.userToldMeHisPref = true;
				if(!p.isQuery())
					this.lastPref.add(p);
				//ServletUtils.log("----------->>> p 1 = " + this.gs.getIssuePluralText().get(p.getIssue1()) , ServletUtils.DebugLevels.DEBUG);
				//ServletUtils.log("----------->>> p 2 = " + this.gs.getIssuePluralText().get(p.getIssue2()) , ServletUtils.DebugLevels.DEBUG);
				//ServletUtils.log("----------->>> last PREF 1 = " + this.gs.getIssuePluralText().get(lastPref.getLast().getIssue1()) , ServletUtils.DebugLevels.DEBUG);
				//ServletUtils.log("----------->>> last PREF 2 = " + this.gs.getIssuePluralText().get(lastPref.getLast().getIssue2()) , ServletUtils.DebugLevels.DEBUG);
				if(lb!=LedgerBehavior.BETRAYING)
				{
//					Offer propose = new Offer(game.getNumberIssues());
//					for(int issue = 0; issue < game.getNumberIssues(); issue++)
//						propose.setItem(issue, allocated.getItem(issue));
					relation = p.getRelation();
					if(relation == Relation.BEST) {
						this.lastPref.getLast().setIssue1(p.getIssue1());;
						str = "If I understood right, you like the best: " + game.getIssuePluralText().get(p.getIssue1()) + " right? \n" 
						+ "Here is something you might like";
						//propose =  behavior.getOfferAccordingToPref(issue1, -1);
						sc = Event.SubClass.OFFER_PROPOSE;
						break;
					}
					if(relation == Relation.WORST) {
						this.lastPref.getLast().setIssue1(p.getIssue1());
						this.lastPref.getLast().setIssue2(-1);
						str = "If I understood right, you really dont like: " + game.getIssuePluralText().get(p.getIssue1()) + "right? \n" 
						+ "maybe tell me about what you do like and i'll might offer you something related..";
						//propose =  behavior.getOfferAccordingToPref(issue1, -1);
						sc = Event.SubClass.GENERIC_POS;
						break;
					}
					
					if(relation == Relation.GREATER_THAN)
					{
						//ServletUtils.log("Relation GREATER_THAN " + game.getIssuePluralText().get(p.getIssue1()) + " > " + game.getIssuePluralText().get(p.getIssue2()) , ServletUtils.DebugLevels.DEBUG);
						this.lastPref.getLast().setIssue1(p.getIssue1());
						this.lastPref.getLast().setIssue2(p.getIssue2());
						if(p.getIssue1() != -1 && p.getIssue2() != -1) {
							str = "If I understood right, you like: " + game.getIssuePluralText().get(p.getIssue1()) + " more than " + game.getIssuePluralText().get(p.getIssue2()) + " right? \n" 
									+ "Here is something you might like";
						} else if(p.getIssue1() != -1 && p.getIssue2() == -1) {
							str = "Do you like " + this.gs.getIssuePluralText().get(p.getIssue1()) + " more than nothing?";
						} else if(p.getIssue1() == -1 && p.getIssue2() != -1) {
							str = "I'm sorry, I dodn't understood what you were trying to say..";
						}
						
						//propose =  behavior.getOfferAccordingToPref(issue1, issue2);
						sc = Event.SubClass.OFFER_PROPOSE;
						break;
					}
					if(relation == Relation.LESS_THAN)
					{
						//ServletUtils.log("Relation LESS_THAN", ServletUtils.DebugLevels.DEBUG);
						this.lastPref.getLast().setIssue1(p.getIssue2());
						this.lastPref.getLast().setIssue2(p.getIssue1());
						
						if(p.getIssue1() != -1 && p.getIssue2() != -1) {
							str = "If I understood right, you like: " + game.getIssuePluralText().get(p.getIssue1()) + "less than " + game.getIssuePluralText().get(p.getIssue2()) + "right? \n" 
									+ "Here is something you might like";
						} else if(p.getIssue1() != -1 && p.getIssue2() == -1) {
							str = "Do you like " + this.gs.getIssuePluralText().get(p.getIssue1()) + " less than nothing?";
						} else if(p.getIssue1() == -1 && p.getIssue2() != -1) {
							str = "I'm sorry, I dodn't understood what you were trying to say..";
						}
						
						//propose =  behavior.getOfferAccordingToPref(issue2, issue1);
						sc = Event.SubClass.OFFER_PROPOSE;
						break;
					}
					//this.allocated = propose;
				}
				else if(lb == LedgerBehavior.BETRAYING)
				{
						relation = p.getRelation();
						if(relation == Relation.BEST) {
							this.lastPref.getLast().setIssue1(p.getIssue1());
							str = "I know you like : " + game.getIssuePluralText().get(issue1) + "the best, but I don't see an offer that can work for both us;";
							sc = Event.SubClass.PREF_INFO;
							break;
						}
						if(relation == Relation.WORST) {
							this.lastPref.getLast().setIssue1(p.getIssue1());
							this.lastPref.getLast().setIssue2(-1);
							str = "I am so sorry, I know you don't like: " + game.getIssuePluralText().get(issue1)
							+ "I can't offer you something that suits your preference, but you have my word that I will offer you something good later :)";
							sc = Event.SubClass.PREF_INFO;
							break;
						}
						
						if(relation == Relation.GREATER_THAN)
						{
							this.lastPref.getLast().setIssue1(p.getIssue1());
							this.lastPref.getLast().setIssue2(p.getIssue2());
							if(p.getIssue1() != -1 && p.getIssue2() != -1) {
								str = "If I understood right, you like: " + game.getIssuePluralText().get(p.getIssue1()) + " more than " + game.getIssuePluralText().get(p.getIssue2()) + " right? \n" 
										+ "I promise you that I will remember this for later offer";
							} else if(p.getIssue1() != -1 && p.getIssue2() == -1) {
								str = "Do you like " + this.gs.getIssuePluralText().get(p.getIssue1()) + " more than nothing?";
							} else if(p.getIssue1() == -1 && p.getIssue2() != -1) {
								str = "I'm sorry, I dodn't understood what you were trying to say..";
							}
							sc = Event.SubClass.PREF_INFO;
							break;
						}
						if(relation == Relation.LESS_THAN)
						{
							this.lastPref.getLast().setIssue1(p.getIssue2());
							this.lastPref.getLast().setIssue2(p.getIssue1());
							if(p.getIssue1() != -1 && p.getIssue2() != -1) {
								str = "If I understood right, you like: " + game.getIssuePluralText().get(p.getIssue1()) + "less than " + game.getIssuePluralText().get(p.getIssue2()) + "right? \n" 
										+ "I promise you that I will remember this for later offer";
							} else if(p.getIssue1() != -1 && p.getIssue2() == -1) {
								str = "Do you like " + this.gs.getIssuePluralText().get(p.getIssue1()) + " less than nothing?";
							} else if(p.getIssue1() == -1 && p.getIssue2() != -1) {
								str = "I'm sorry, I dodn't understood what you were trying to say..";
							}
							sc = Event.SubClass.PREF_INFO;
							break;
						}
				}
			}else if (p != null && p.isQuery()){
				ArrayList<Integer> vhPref = utils.getMyOrdering();
				int item1 = -1;
				int item2 = -1;
				for (int i = 0; i < this.allocated.getIssueCount(); i++) {
					if(p.getIssue1() == i)
						item1 = vhPref.get(i);
					
					if(p.getIssue2() == i)
						item2 = vhPref.get(i);
				}
				relation = p.getRelation();
				if(this.lb == LedgerBehavior.FAIR) {
					if(relation == Relation.GREATER_THAN) {
						if (item1 > item2) {
							str+= "That is correct! You are a magician!";
							sc = Event.SubClass.GENERIC_POS;
							break;
						} else {
							str+= "I'm sorry, that is incorrect. ";
							sc = Event.SubClass.GENERIC_POS;
							break;
						}
						
					} else if(relation == Relation.LESS_THAN) {
						if (item2 > item1) {
							str+= "That is correct! You are a magician!";
							sc = Event.SubClass.GENERIC_POS;
							break;
						} else {
							str+= "I'm sorry, that is incorrect. ";
							sc = Event.SubClass.GENERIC_POS;
							break;
						}
						
					} else if(relation == Relation.BEST) {
						if(p.getIssue1() == this.utils.findMyItemIndex(gs, 1)) {
							str+= "That is correct! You are a magician! ";
							sc = Event.SubClass.GENERIC_POS;
							break;
						}
						else {
							str+= "Oh I am so sorry, that is incorrect. ";
							sc = Event.SubClass.GENERIC_POS;
							break;
						}
					} else if(relation == Relation.WORST) {
						if(p.getIssue1() == this.utils.findMyItemIndex(gs, this.behavior.getAllocated().getIssueCount())) {
							str+= "That is correct! You are a magician! ";
							sc = Event.SubClass.GENERIC_POS;
							break;
						}
						else {
							str+= "No it's not true";
							sc = Event.SubClass.GENERIC_POS;
							break;
						}
					}
					
					
				} else if (this.lb == LedgerBehavior.LIMITED) {
					str+= "Try just make an offer instead of asking me questions.. ";
					
					sc = Event.SubClass.GENERIC_POS;
				} else {
					str+= "I'm not in a mood for answering that. While i'm writing this lines of code, Denemark just lost EURO semi final... (I am a fan)  ";
					sc = Event.SubClass.GENERIC_POS;
					break;
				}
			}
			break;
		case PREF_REQUEST:
			ArrayList<Integer> vhPref = utils.getMyOrdering();
			issue1 = utils.findMyItemIndex(game, 1);
			issue2 = this.behavior.findOpponentIdealSecondBest(vhPref);
			int leastIssue = utils.findMyItemIndex(this.gs, this.gs.getNumberIssues());
			switch(this.lb) {
			case FAIR:{
				
				if (issue1 == -1 && issue2 == -1)
				{
					str = "I'm sorry dear, I couldn't find anything I want best or more than something else. Would you like to make an offer maybe?";
					sc = Event.SubClass.OFFER_REQUEST_POS;
					break;
					

				}
				else if (issue1 != -1 && issue2 == -1 && leastIssue != -1)
				{
					isQuery = false;
					str = "Because you asked so nice, and you look so nice from the inside of the computer...\n"
							+ "I like " + game.getIssuePluralText().get(issue1) + " more than " + game.getIssuePluralText().get(leastIssue); 
					relation = Relation.GREATER_THAN;
					sc = Event.SubClass.PREF_INFO;
					break;
					
				}
				else if(issue1 == -1 && issue2 != -1)
				{
					if (leastIssue != -1) {
						isQuery = false;
						str = " ohhhh ok,   I like " + game.getIssuePluralText().get(issue2) + " more than " 
								+ game.getIssuePluralText().get(leastIssue) + " but just so you know, it's not my BEST";
						sc = Event.SubClass.PREF_INFO;
						relation = Relation.GREATER_THAN;
						break;
					} else if(issue1 != -1 && issue2 != -1) {
						isQuery = false;
						str = "I got ya, I like " + game.getIssuePluralText().get(issue1) + " more than " +  game.getIssuePluralText().get(issue2);
						sc = Event.SubClass.PREF_INFO;
						relation = Relation.GREATER_THAN;
						break;
						
					} else {
						isQuery = false;
						str = "I got ya, I like " + game.getIssuePluralText().get(issue2) + " the most." ;
						sc = Event.SubClass.PREF_INFO;
						relation = Relation.BEST;
						break;
					}
						
				} else { // issue1 != -1 && issue2 == -1
					isQuery = false;
					str = "I got ya, I like " + game.getIssuePluralText().get(issue1) + " the most." ;
					sc = Event.SubClass.PREF_INFO;
					relation = Relation.BEST;
					break;
				}
				
			}
			case LIMITED:{
				str = getHoldingMes();
				sc = Event.SubClass.GENERIC_POS;
				break;
			}
				

			case BETRAYING:{
				if (leastIssue != -1) {
					isQuery = false;
					str = "I got ya, I like " + game.getIssuePluralText().get(leastIssue) + " the most." ;
					sc = Event.SubClass.PREF_INFO;
					relation = Relation.BEST;
					break;
				} else {
					int issueNum4 = utils.findAdversaryItemIndex(gs, 4);
					if(issueNum4 != -1) {
						isQuery = false;
						str = "I got ya, I like " + game.getIssuePluralText().get(leastIssue) + " the most." ;
						sc = Event.SubClass.PREF_INFO;
						relation = Relation.BEST;
						break;
					}
				} 
				
				str = "It's a secret :)" ;
				sc = Event.SubClass.GENERIC_POS;
				break;
				
			}
		}
				
			
				

		case PREF_SPECIFIC_REQUEST:
			return null;
		case PREF_WITHHOLD: //DONT NEED THIS ANYMORE, I THINK?
			return null;
//		case OFFER_REJECT: 
//			sc = Event.SubClass.GENERIC_NEG;
//			str = this.getRejectLang(history, game);
//			break;
		case OFFER_ACCEPT:
			this.behavior.setCalledToOfferByPref(false);
			sc = Event.SubClass.GENERIC_POS;
			str = this.getAcceptLang(history, game);
			break;
		case BATNA_INFO:
			if (ePrime.getValue() != -1)
			{
				utils.adversaryBATNA = ePrime.getValue();
				if (!utils.conflictBATNA(utils.myPresentedBATNA, utils.adversaryBATNA))
				{	
					if(opponentBATNA != utils.adversaryBATNA && opponentBATNA != -1) 
						str = "Oh it is? I thought you needed more than " + opponentBATNA + " points. ";


					opponentBATNA =  utils.adversaryBATNA;
					//TODO here is a good opportunity to improve the system so that it doesn't repeat this same info too often!
					
					//str += "In case you forgot, I already have an offer for " + utils.myPresentedBATNA + " points, so anything that gets me more than " 
					//		+ utils.myPresentedBATNA + " points will do.";
					str += "Thank you for the information.  That's helpful.";
					
					value = utils.myPresentedBATNA;
					sc = Event.SubClass.GENERIC_POS;
				} 
				else
				{
					opponentBATNA =  utils.adversaryBATNA;
					str = "Well, since you can't accept anything less than " + utils.adversaryBATNA + " points and I can't accept anything that gets me less than " 
							+ utils.myPresentedBATNA + " points, I don't think we'll be able to make a deal. Maybe we should just walk away.";
					value = utils.myPresentedBATNA; 
					sc = Event.SubClass.THREAT_POS;
				}
			}
			else {
				ServletUtils.log("No BATNA value found", ServletUtils.DebugLevels.DEBUG);
				str = "I don't know what message you just sent!";
			}

			break;
		case BATNA_REQUEST:
			
			if(this.lb.equals(RepeatedFavorBehavior.LedgerBehavior.BETRAYING)) {
				str += "Maybe i'll tell you later my BATNA, why won't you propose somthing, maybe it'll fit!";
				sc = Event.SubClass.OFFER_REQUEST_POS;
			} else if(this.lb.equals(RepeatedFavorBehavior.LedgerBehavior.LIMITED)){
				str += "I'm not telling you what's my minmum limit. Just make an offer"; 
				sc = Event.SubClass.OFFER_REQUEST_POS;
			} else {
				str += "I already have an offer for " + utils.myPresentedBATNA + " points, so anything that gets me more than " 
						+ utils.myPresentedBATNA + " points will do.";
				value = utils.myPresentedBATNA;
				sc = Event.SubClass.BATNA_INFO;
			}
			
			break;
		case CONFUSION:
			str += "I'm sorry, have I said something confusing? I didn't mean to.";
			sc = Event.SubClass.GENERIC_POS;
			break;
		case FAVOR_ACCEPT:
			str += "Oh wonderful!  I will make sure to pay you back!";
			utils.modifyVerbalLedger(-1);
			sc = Event.SubClass.FAVOR_ACCEPT;
			break;
		case FAVOR_REJECT:
			str += "Oh blast!  And this was so important to me this round too...";
			sc = Event.SubClass.GENERIC_NEG;
			break;
		case FAVOR_REQUEST:
			boolean paysLedger = (lb == LedgerBehavior.FAIR || lb == LedgerBehavior.LIMITED);
			if(lb == LedgerBehavior.NONE)
			{
				str += "I don't really do favors.";
				sc = Event.SubClass.FAVOR_REJECT;
			}
			else if(utils.isImportantGame())
			{
				str += "Oh I'm sorry, but items this game are worth so much to me...";
				sc = Event.SubClass.FAVOR_REJECT;			
			}
			else if (paysLedger && utils.getLedger() < 0)
			{
				str += "Sure, since you did me that favor before, I'm happy to help this round.";
				utils.modifyVerbalLedger(1);
				sc = Event.SubClass.FAVOR_ACCEPT;				
			}
			else if (!paysLedger && utils.getLedger() < 0)
			{
				str += "Hmm.  I don't really feel like it.";
				sc = Event.SubClass.FAVOR_REJECT;	
			}
			else if (utils.getLedger() == 0)
			{
				str += "Sure, but you'll owe me one, ok?";
				utils.modifyVerbalLedger(1);
				sc = Event.SubClass.FAVOR_ACCEPT;	
			}
			else //(utils.getTotalLedger()> 0)
			{
				str += "No way!  You still owe me from before...";
				sc = Event.SubClass.FAVOR_REJECT;	
			}			
			break;
		case FAVOR_RETURN:
			/***
			 * TODO check lb
			 ***/
			if (utils.getLedger() > 0)//note: agent has no way of knowing if you're being honest
			{
				str += "Thanks for returning the favor from before!";
				utils.modifyVerbalLedger(-1);
				sc = Event.SubClass.FAVOR_ACCEPT;
			}
			else
			{
				str += "Well, you're welcome I guess... but I don't think you really owed me!";
				sc = Event.SubClass.GENERIC_POS;
			}
			break;
		case NONE:
			ServletUtils.log("Agent didn't have a subclass to respond to...", ServletUtils.DebugLevels.DEBUG);
			return null;
		case OFFER_PROPOSE: //impossible to accomplish, human can't do this
			return null;
		default:
			return null;
		}

		Event resp;
		if (value != -1) {
			ServletUtils.log("value 1 != -1", ServletUtils.DebugLevels.DEBUG);
			resp = new Event(agentID, Event.EventClass.SEND_MESSAGE, sc, value, str, delay);				
		} else {
			ServletUtils.log("value 1 == -1", ServletUtils.DebugLevels.DEBUG);
			resp = new Event(agentID, Event.EventClass.SEND_MESSAGE, sc, str, delay);	
		}

//		if (relation != null) {
//			ServletUtils.log("encodePreferenceData", ServletUtils.DebugLevels.DEBUG);
//			resp.encodePreferenceData(new Preference(issue1, issue2, relation, isQuery), gs);
//		}
		return resp; 

	}
}