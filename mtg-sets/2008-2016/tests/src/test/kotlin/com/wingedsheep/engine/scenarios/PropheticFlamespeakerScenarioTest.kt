package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Prophetic Flamespeaker (JOU #106) — {1}{R}{R} Creature — Human Shaman 1/3.
 *
 *   Double strike, trample
 *   Whenever this creature deals combat damage to a player, exile the top card of your library.
 *   You may play it this turn.
 *
 * Double strike deals combat damage to the player in both damage steps, so one unblocked attack
 * exiles two cards — both playable for the rest of the turn.
 */
class PropheticFlamespeakerScenarioTest : ScenarioTestBase() {
    init {
        test("an unblocked attack triggers twice and the exiled cards are playable this turn") {
            val game = scenario()
                .withPlayers("P1", "P2")
                .withCardOnBattlefield(1, "Prophetic Flamespeaker")
                .withLandsOnBattlefield(1, "Forest", 2)
                .withCardInLibrary(1, "Grizzly Bears")
                .withCardInLibrary(1, "Hill Giant")
                .withCardInLibrary(2, "Island")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Prophetic Flamespeaker" to 2)).error shouldBe null
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
            game.declareNoBlockers()
            game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)

            withClue("1 first-strike damage + 1 regular damage") {
                game.getLifeTotal(2) shouldBe 18
            }
            withClue("each damage step exiled the top card of the library") {
                game.isInExile(1, "Grizzly Bears") shouldBe true
                game.isInExile(1, "Hill Giant") shouldBe true
                game.librarySize(1) shouldBe 0
            }

            game.castSpellFromExile(1, "Grizzly Bears").error shouldBe null
            game.resolveStack()
            withClue("an exiled card can be cast this turn") {
                game.isOnBattlefield("Grizzly Bears") shouldBe true
            }
        }
    }
}
