package edu.usc.ict.iago.agent;

import edu.usc.ict.iago.agent.RepeatedFavorBehavior.LedgerBehavior;
import edu.usc.ict.iago.utils.BehaviorPolicy;
import edu.usc.ict.iago.utils.History;
import edu.usc.ict.iago.utils.Offer;

public abstract class IAGOCoreBehavior implements BehaviorPolicy
{
	/**
	 * Update the internal representation of what offers are considered "firm" currently.
	 * @param update the total summed offer to save
	 */
	protected abstract void updateAllocated (Offer update);
	
	/**
	 * Retrieves the internal representation of what offers are considered "firm" currently.
	 * @return the total summed offer
	 */
	protected abstract Offer getOfferAccordingToPref(int issue1, int issue2);
	protected abstract Offer getAllocated ();
	protected abstract Offer getFavorOffer();
	public abstract void setIsThreatMe(boolean didHe);
	public abstract boolean isCalledToOfferByPref();
	public abstract void setCalledToOfferByPref(boolean bool);
	
	public abstract float getPlayerLieThreshHold();
	public abstract void setPlayerLieThreshHold(float bool);
	
	public abstract void setLedgerBehavior(int numGame);
	
	/**
	 * Gets the offer that comes when you're nearly out of time.
	 * @param history the history to use
	 * @return the final offer
	 */
	protected abstract Offer getFinalOffer(History history);
	public abstract LedgerBehavior getLb();
	
	/**
	 * Helper for adding an AgentUtilsExtension.
	 * @param utils the utils to add
	 */
	protected abstract void setUtils(AgentUtilsExtension utils);
	public abstract void setMessage(IAGOCoreMessage message);

	/**
	 * Gets the offer that comes when you've been idle.
	 * @param history the history to use
	 * @return the idle offer
	 */
	protected abstract Offer getTimingOffer(History history);

	/**
	 * Gets the offer that comes after the agent's adversary accepts an offer.
	 * @param history the history to use
	 * @return the followup offer
	 */
	protected abstract Offer getAcceptOfferFollowup(History history);

	/**
	 * Gets that is proposed immediately once the game starts.
	 * @param history the history to use
	 * @return the first offer
	 */
	protected abstract Offer getFirstOffer(History history);

	/**
	 * Gets the amount of points that the agent requires to be ahead of its opponent before accepting.
	 * @return the point margin (a negative means an agent won't accept offers that are worse for it than an opponent in any circumstance)
	 */
	protected abstract int getAcceptMargin();

	/**
	 * Gets that is proposed after the agent's adversary rejects an offer.
	 * @param history the history to use
	 * @return the first offer
	 */
	protected abstract Offer getRejectOfferFollowup(History history);

	/**
	 * Retrieves the internal representation of what the agent is maintaining as its heuristic currently.
	 * @return the total summed offer
	 */
	protected abstract Offer getConceded();
	
	/**
	 * Returns the value of the agent belief, true if fixed pie, false if integrative
	 * @return the agent belief, true if fixed pie, false if integrative
	 */
	protected boolean getAgentBelief()
	{
		return false;
	}
	
	/**
	 * Allows you to change the adverse Events counter, which weakens the agent's margin
	 * @param change the amount to change by
	 */
	protected abstract void updateAdverseEvents (int change);

	protected abstract void setCurrGame(int currentGameCount);
	
	protected abstract int getGameCount();
	
	protected abstract  Offer myNextOffer(History history); 
}
