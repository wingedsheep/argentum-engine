package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe

/** Invasion of Zendikar // Awakened Skyclave. */
class InvasionOfZendikarScenarioTest : ScenarioTestBase() {
    init {
        test("front: ETB puts up to two basic lands onto the battlefield tapped") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardInHand(1, "Invasion of Zendikar")
                .withLandsOnBattlefield(1, "Forest", 4)
                .withCardInLibrary(1, "Forest")
                .withCardInLibrary(1, "Plains")
                .withCardInLibrary(1, "Grizzly Bears")
                .withCardInLibrary(2, "Island")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Invasion of Zendikar").error shouldBe null
            game.resolveStack()

            val d = game.getPendingDecision() as SelectCardsDecision
            // Only the two basic lands are searchable; Grizzly Bears is excluded.
            d.options shouldHaveSize 2
            game.selectCards(d.options).error shouldBe null
            game.resolveStack()

            val plains = game.findPermanent("Plains")!!
            game.state.getEntity(plains)!!.has<TappedComponent>() shouldBe true
            game.findPermanents("Forest") shouldHaveSize 5
            game.findCardsInLibrary(1, "Grizzly Bears") shouldHaveSize 1
        }

        test("back: Awakened Skyclave is a 4/4 vigilance haste creature land") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Invasion of Zendikar")
                .withCardInHand(1, "Lightning Bolt")
                .withLandsOnBattlefield(1, "Mountain", 1)
                .withCardInLibrary(1, "Island")
                .withCardInLibrary(2, "Island")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            game.checkStateBasedActions()

            game.castSpell(1, "Lightning Bolt", game.findPermanent("Invasion of Zendikar")!!).error shouldBe null
            game.resolveStack()
            game.answerYesNo(true).error shouldBe null
            game.resolveStack()

            val skyclave = game.findPermanent("Awakened Skyclave")!!
            val projected = game.state.projectedState
            projected.isCreature(skyclave) shouldBe true
            projected.hasType(skyclave, "LAND") shouldBe true
            projected.getPower(skyclave) shouldBe 4
            projected.getToughness(skyclave) shouldBe 4
            projected.hasKeyword(skyclave, Keyword.VIGILANCE) shouldBe true
            projected.hasKeyword(skyclave, Keyword.HASTE) shouldBe true

            // Haste: it can attack the turn it arrives, and vigilance keeps it untapped.
            game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Awakened Skyclave" to 2)).error shouldBe null
            game.state.getEntity(skyclave)!!.has<TappedComponent>() shouldBe false
        }
    }
}
