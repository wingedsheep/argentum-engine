package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.card
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Wasp of the Bitter End (HOU #206) — {1}{B} Creature — Insect Horror 2/1, flying.
 *
 * "Whenever you cast a Bolas planeswalker spell, you may sacrifice this creature. If you do,
 *  destroy target creature."
 *
 * Pins the cast trigger's Bolas-planeswalker filter, the target chosen as the trigger is stacked,
 * and the resolution-time "you may sacrifice … if you do" gate in both directions.
 */
class WaspOfTheBitterEndScenarioTest : ScenarioTestBase() {

    private val bolasWalker = card("Wasp Test Bolas Walker") {
        manaCost = "{B}"
        typeLine = "Legendary Planeswalker — Bolas"
        startingLoyalty = 3
        oracleText = ""
    }
    private val otherWalker = card("Wasp Test Other Walker") {
        manaCost = "{B}"
        typeLine = "Legendary Planeswalker — Liliana"
        startingLoyalty = 3
        oracleText = ""
    }

    init {
        cardRegistry.register(bolasWalker)
        cardRegistry.register(otherWalker)

        fun builder() = scenario()
            .withPlayers("Player", "Opponent")
            .withCardOnBattlefield(1, "Wasp of the Bitter End")
            .withCardInHand(1, "Wasp Test Bolas Walker")
            .withCardInHand(1, "Wasp Test Other Walker")
            .withLandsOnBattlefield(1, "Swamp", 2)
            .withCardOnBattlefield(2, "Grizzly Bears")
            .withCardInLibrary(1, "Swamp")
            .withCardInLibrary(2, "Swamp")
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)

        fun TestGame.castBolasAndTargetBears() {
            castSpell(1, "Wasp Test Bolas Walker").error shouldBe null
            val decision = state.pendingDecision
            decision.shouldBeInstanceOf<ChooseTargetsDecision>()
            val bears = findPermanent("Grizzly Bears")!!
            decision.legalTargets[0]!! shouldContain bears
            selectTargets(listOf(bears)).error shouldBe null
            resolveStack()
        }

        test("accepting sacrifices the Wasp and destroys the target") {
            val game = builder().build()
            game.castBolasAndTargetBears()
            game.answerYesNo(true).error shouldBe null
            game.resolveStack()

            game.isInGraveyard(1, "Wasp of the Bitter End") shouldBe true
            game.isInGraveyard(2, "Grizzly Bears") shouldBe true
            game.isOnBattlefield("Wasp Test Bolas Walker") shouldBe true
        }

        test("declining keeps both the Wasp and the target") {
            val game = builder().build()
            game.castBolasAndTargetBears()
            game.answerYesNo(false).error shouldBe null
            game.resolveStack()

            game.isOnBattlefield("Wasp of the Bitter End") shouldBe true
            game.isOnBattlefield("Grizzly Bears") shouldBe true
        }

        test("a non-Bolas planeswalker spell does not trigger it") {
            val game = builder().build()
            game.castSpell(1, "Wasp Test Other Walker").error shouldBe null
            game.state.pendingDecision shouldBe null
            game.resolveStack()

            game.isOnBattlefield("Wasp of the Bitter End") shouldBe true
            game.isOnBattlefield("Grizzly Bears") shouldBe true
            game.isOnBattlefield("Wasp Test Other Walker") shouldBe true
        }
    }
}
