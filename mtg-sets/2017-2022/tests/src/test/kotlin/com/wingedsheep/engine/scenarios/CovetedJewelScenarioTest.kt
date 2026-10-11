package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Coveted Jewel (C18) — "Whenever one or more creatures an opponent controls attack you and aren't
 * blocked, that player draws three cards and gains control of this artifact. Untap it."
 *
 * The payoff hinges on "that player": the trigger fires from the block declaration, which names no
 * attacking player, so the engine binds the unblocked attackers' controller. A tapped Jewel moving
 * over untapped is the "Untap it" rider.
 */
class CovetedJewelScenarioTest : ScenarioTestBase() {

    init {
        test("an unblocked attacker steals the Jewel: attacker draws three, gains control, Jewel untaps") {
            val game = scenario()
                .withPlayers("Jewel Owner", "Attacker")
                .withCardOnBattlefield(1, "Coveted Jewel", tapped = true)
                .withCardOnBattlefield(2, "Grizzly Bears", summoningSickness = false)
                .withCardInLibrary(2, "Plains")
                .withCardInLibrary(2, "Plains")
                .withCardInLibrary(2, "Plains")
                .withCardInLibrary(2, "Plains")
                .withActivePlayer(2)
                .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                .build()

            val attackerHand = game.handSize(2)
            game.declareAttackers(mapOf("Grizzly Bears" to 1)).error shouldBe null
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
            game.declareNoBlockers().error shouldBe null
            game.resolveStack()

            val jewel = game.findPermanent("Coveted Jewel")!!
            withClue("that player — the attacker — draws three") {
                game.handSize(2) shouldBe attackerHand + 3
            }
            withClue("and gains control of the Jewel, untapped") {
                game.state.projectedState.getController(jewel) shouldBe game.player2Id
                game.state.getEntity(jewel)!!.has<TappedComponent>() shouldBe false
            }
        }

        test("a blocked attacker leaves the Jewel where it is") {
            val game = scenario()
                .withPlayers("Jewel Owner", "Attacker")
                .withCardOnBattlefield(1, "Coveted Jewel")
                .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
                .withCardOnBattlefield(2, "Centaur Courser", summoningSickness = false)
                .withCardInLibrary(2, "Plains")
                .withCardInLibrary(2, "Plains")
                .withCardInLibrary(2, "Plains")
                .withCardInLibrary(2, "Plains")
                .withActivePlayer(2)
                .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                .build()

            val attackerHand = game.handSize(2)
            game.declareAttackers(mapOf("Centaur Courser" to 1)).error shouldBe null
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
            game.declareBlockers(mapOf("Grizzly Bears" to listOf("Centaur Courser"))).error shouldBe null
            game.resolveStack()

            game.handSize(2) shouldBe attackerHand
            game.state.projectedState.getController(game.findPermanent("Coveted Jewel")!!) shouldBe game.player1Id
        }
    }
}
