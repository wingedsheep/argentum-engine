package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.PlayLand
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Veilborn Ghoul (M13 #114) — {4}{B} 4/1 Creature — Zombie.
 *   This creature can't block.
 *   Whenever a Swamp you control enters, you may return this card from your graveyard to your hand.
 */
class VeilbornGhoulScenarioTest : ScenarioTestBase() {
    init {
        test("playing a Swamp returns it from the graveyard to your hand") {
            val game = scenario()
                .withPlayers("P1", "P2")
                .withCardInGraveyard(1, "Veilborn Ghoul")
                .withCardInHand(1, "Swamp")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val swamp = game.findCardsInHand(1, "Swamp").single()

            withClue("playing the Swamp is legal") {
                game.execute(PlayLand(game.player1Id, swamp)).error shouldBe null
            }
            game.resolveStack()
            game.getPendingDecision().shouldBeInstanceOf<YesNoDecision>()
            game.answerYesNo(true)
            game.resolveStack()

            game.isInHand(1, "Veilborn Ghoul") shouldBe true
            game.isInGraveyard(1, "Veilborn Ghoul") shouldBe false
        }

        test("can't block") {
            val game = scenario()
                .withPlayers("P1", "P2")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardOnBattlefield(2, "Veilborn Ghoul")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Grizzly Bears" to 2)).error shouldBe null
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
            game.declareBlockers(mapOf("Veilborn Ghoul" to listOf("Grizzly Bears"))).error shouldNotBe null
        }
    }
}
