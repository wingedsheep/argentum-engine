package com.wingedsheep.ai.engine

import com.wingedsheep.ai.puzzles.advanceToDeclaration
import com.wingedsheep.ai.puzzles.seatId
import com.wingedsheep.engine.core.DeclareAttackers
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldNotContain

/**
 * An activated ability that sets a creature's base power and toughness ([SetBaseStatsEffect]) changes
 * how a fight resolves, so [com.wingedsheep.ai.engine.knowledge.CombatReach] reads it as FIGHT and the
 * combat simulation gives it to the defender as a response.
 *
 * Sorceress Queen ("{T}: Target creature other than Sorceress Queen has base power and toughness 0/2
 * until end of turn") beside Grizzly Bears: a Hill Giant attacking into them reads as a free three
 * damage — the Bears can't block a 3/3 profitably — until the Queen makes the blocked Giant a 0/2
 * that the Bears kill.
 */
class SetBaseStatsCombatReadingAiTest : ScenarioTestBase() {

    init {
        test("doesn't attack into a defender's set-base-P/T ability that wins the block") {
            val game = scenario().withRngSeed(20261106L).withPlayers()
                .withCardOnBattlefield(1, "Hill Giant")
                .withCardOnBattlefield(2, "Grizzly Bears")
                .withCardOnBattlefield(2, "Sorceress Queen")
                .build()
                .advanceToDeclaration(1, Step.DECLARE_ATTACKERS)
            val giant = game.findPermanent("Hill Giant")!!

            val action = AIPlayer.create(cardRegistry, game.seatId(1), AiProfile.LIVE).chooseAction(game.state)
            val attackers = (action as? DeclareAttackers)?.attackers?.keys.orEmpty()
            withClue("combat reads SetBaseStatsEffect as a fight answer: Sorceress Queen makes the blocked Hill Giant a 0/2 the Bears kill") {
                attackers shouldNotContain giant
            }
        }
    }
}
