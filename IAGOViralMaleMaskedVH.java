package edu.usc.ict.iago.agent;

import javax.websocket.Session;

import edu.usc.ict.iago.utils.GameSpec;


/**
 * @author mell
 * 
 */
public class IAGOViralMaleMaskedVH extends IAGOCoreVH {

	/**
	 * @author mell
	 * Instantiates a new  VH.
	 *
	 * @param name: agent's name
	 * @param game: gamespec value
	 * @param session: the session
	 */
	public IAGOViralMaleMaskedVH(String name, GameSpec game, Session session)
	{
		super("ViralMaleMasked", game, session, new RepeatedFavorBehavior(RepeatedFavorBehavior.LedgerBehavior.FAIR), new RepeatedFavorExpression(), 
				new RepeatedFavorMessage(false, false, RepeatedFavorBehavior.LedgerBehavior.FAIR, game));	
		
		super.safeForMultiAgent = true;
	}

	@Override
	public String getArtName() {
		return "BradMask";
	}

	@Override
	public String agentDescription() {
			return "<h1>Mongo</h1><p>They are excited to begin negotiating!</p>";
	}
}