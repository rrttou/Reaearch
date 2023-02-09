package edu.usc.ict.iago.agent;

import edu.usc.ict.iago.utils.GameSpec;
import edu.usc.ict.iago.utils.History;
import edu.usc.ict.iago.utils.Offer;
import edu.usc.ict.iago.utils.Event;
import edu.usc.ict.iago.utils.Preference;
import edu.usc.ict.iago.utils.ServletUtils;	

import java.util.*;

public class State implements Reward{
	private AgentUtilsExtension utils;
	private GameSpec game;
	private Offer internalState;
	
	public State(Offer o, AgentUtilsExtension utils) {
		this.utils = utils;
		this.game = utils.getSpec();
		this.internalState = new Offer(this.game.getNumberIssues());
		for(int issue = 0; issue < game.getNumberIssues(); issue++)
			internalState.setItem(issue, o.getItem(issue));
	}
	
	public State(GameSpec g, AgentUtilsExtension utils) {
		this.game = g;
		this.utils = utils;
	}
	
	public void updateState(Offer o) {
		this.internalState = new Offer(game.getNumberIssues());
		for(int issue = 0; issue < game.getNumberIssues(); issue++)
			internalState.setItem(issue, o.getItem(issue));
	}
	
	public Offer getOffer() {
		return this.internalState;
	}
	
	@Override
	public double getReward() {
		int totalPointsOfAgent = 0;
		for(int i = 0; i < game.getNumberIssues(); i++) {
			totalPointsOfAgent += this.internalState.getItem(i)[0] * game.getSimplePoints(0).get(game.getIssuePluralText().get(i));
		}
		//ServletUtils.log("Total Points Of Me " + totalPointsOfAgent, ServletUtils.DebugLevels.DEBUG);
		
		int totalPointsOfFree = 0;
		for(int i = 0; i < game.getNumberIssues(); i++) {
			totalPointsOfFree += this.internalState.getItem(i)[1] * game.getSimplePoints(0).get(game.getIssuePluralText().get(i));
		}
		//ServletUtils.log("Total Points Of Free " + totalPointsOfFree, ServletUtils.DebugLevels.DEBUG);
		
		int totalPointsOfOpponent = utils.getMaxPossiblePoints() - totalPointsOfFree - totalPointsOfAgent; //Total Points that currently the human has 
		//ServletUtils.log("Adversary Value " + totalPointsOfOpponent, ServletUtils.DebugLevels.DEBUG);
		
		//First Option:
		double res = (double)(1)/(double)(totalPointsOfFree+totalPointsOfOpponent);
		//Second option that demonstrates the same Idea: w1*n1+w2*n2+..../n
		//double res = (double)(totalPointsOfAgent/50.0);
		return game.getBATNA(0) + res;


	}
	
}