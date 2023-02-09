package edu.usc.ict.iago.agent;

import edu.usc.ict.iago.utils.ExpressionPolicy;

public abstract class IAGOCoreExpression implements ExpressionPolicy
{
	protected abstract String getSemiFairEmotion();
	
	protected abstract String getFairEmotion();
	
	protected abstract String getUnfairEmotion();
	
	protected abstract String getHappy();
	
	protected abstract String getSad();
	protected abstract String getNeutral();
		
	
	protected abstract String getAngry();
		
	
	protected abstract String getSurprised();
}
