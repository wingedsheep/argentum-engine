package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Namazu Trader — {3}{B} 3/4.
 * "When this creature enters, you lose 1 life and create a Treasure token.
 *  Whenever this creature attacks, you may sacrifice another creature or artifact. If you do, surveil 2."
 *
 * The sacrifice doesn't target, so the attack trigger asks one question as it resolves: which
 * permanent to sacrifice, or none.
 */
class NamazuTraderScenarioTest : ScenarioTestBase() {

    private fun attackingWithFodder(vararg fodder: String): TestGame {
        val builder = scenario()
            .withPlayers()
            .withCardOnBattlefield(1, "Namazu Trader")
            .withCardInLibrary(1, "Swamp")
            .withCardInLibrary(1, "Forest")
            .withActivePlayer(1)
            .inPhase(Phase.COMBAT, Step.BEGIN_COMBAT)
        fodder.forEach { builder.withCardOnBattlefield(1, it) }
        val game = builder.build()
        game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
        game.declareAttackers(mapOf("Namazu Trader" to 2)).error shouldBe null
        if (game.state.pendingDecision == null) game.resolveStack()
        return game
    }

    init {
        test("enters: you lose 1 life and create a Treasure token") {
            val game = scenario()
                .withPlayers()
                .withCardInHand(1, "Namazu Trader")
                .withLandsOnBattlefield(1, "Swamp", 4)
                .withCardInLibrary(1, "Swamp")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Namazu Trader").error shouldBe null
            game.resolveStack()

            withClue("controller lost 1 life") { game.getLifeTotal(1) shouldBe 19 }
            withClue("a Treasure token was created") { (game.findPermanent("Treasure") != null) shouldBe true }
        }

        test("attack trigger asks once: pick the permanent to sacrifice, then surveil 2") {
            val game = attackingWithFodder("Grizzly Bears", "Ornithopter")
            val bears = game.findPermanent("Grizzly Bears")!!
            val thopter = game.findPermanent("Ornithopter")!!

            val pick = game.state.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
            withClue("the first question is the sacrifice pick, offering only *other* creatures and artifacts") {
                pick.options.toSet() shouldBe setOf(bears, thopter)
                pick.minSelections shouldBe 0
                pick.maxSelections shouldBe 1
                pick.declineLabel shouldBe "Don't sacrifice"
            }

            game.selectCards(listOf(bears)).error shouldBe null

            withClue("the chosen creature was sacrificed and the other kept") {
                game.isInGraveyard(1, "Grizzly Bears") shouldBe true
                (game.findPermanent("Ornithopter") != null) shouldBe true
            }
            withClue("surveil 2 follows the sacrifice") {
                val surveil = game.state.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
                surveil.options.size shouldBe 2
            }
        }

        test("declining the pick sacrifices nothing and skips the surveil") {
            val game = attackingWithFodder("Grizzly Bears")
            game.state.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()

            game.selectCards(emptyList()).error shouldBe null

            withClue("Grizzly Bears is still on the battlefield") {
                (game.findPermanent("Grizzly Bears") != null) shouldBe true
            }
            withClue("no surveil without the sacrifice") { game.state.pendingDecision.shouldBeNull() }
        }

        test("with nothing else to sacrifice the trigger asks nothing") {
            val game = attackingWithFodder()
            game.resolveStack()
            game.state.pendingDecision.shouldBeNull()
            (game.findPermanent("Namazu Trader") != null) shouldBe true
        }
    }
}
