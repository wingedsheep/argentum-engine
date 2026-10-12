package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe

/**
 * Slagstone Refinery (BRO #243) — {4} Artifact. "Whenever this artifact or another nontoken artifact
 * you control is put into a graveyard from the battlefield or is put into exile from the battlefield,
 * create a tapped Powerstone token."
 */
class SlagstoneRefineryScenarioTest : ScenarioTestBase() {

    init {
        test("another nontoken artifact you control being destroyed creates a tapped Powerstone") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Slagstone Refinery")
                .withCardOnBattlefield(1, "Ornithopter")
                .withCardInHand(1, "Shatter")
                .withLandsOnBattlefield(1, "Mountain", 2)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Shatter", game.findPermanent("Ornithopter")!!).error shouldBe null
            game.resolveStack()

            game.isInGraveyard(1, "Ornithopter") shouldBe true
            val powerstones = game.findAllPermanents("Powerstone")
            powerstones shouldHaveSize 1
            game.state.getEntity(powerstones.single())!!.has<TappedComponent>() shouldBe true
            game.state.projectedState.getController(powerstones.single()) shouldBe game.player1Id
        }

        test("another nontoken artifact you control being exiled creates a Powerstone") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Slagstone Refinery")
                .withCardOnBattlefield(1, "Ornithopter")
                .withCardInHand(1, "Revoke Existence")
                .withLandsOnBattlefield(1, "Plains", 2)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Revoke Existence", game.findPermanent("Ornithopter")!!).error shouldBe null
            game.resolveStack()

            game.isInExile(1, "Ornithopter") shouldBe true
            game.findAllPermanents("Powerstone") shouldHaveSize 1
        }

        test("the Refinery itself being exiled creates a Powerstone") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Slagstone Refinery")
                .withCardInHand(1, "Revoke Existence")
                .withLandsOnBattlefield(1, "Plains", 2)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Revoke Existence", game.findPermanent("Slagstone Refinery")!!).error shouldBe null
            game.resolveStack()

            game.isInExile(1, "Slagstone Refinery") shouldBe true
            game.findAllPermanents("Powerstone") shouldHaveSize 1
        }

        test("the Refinery itself being destroyed creates a Powerstone") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Slagstone Refinery")
                .withCardInHand(1, "Shatter")
                .withLandsOnBattlefield(1, "Mountain", 2)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Shatter", game.findPermanent("Slagstone Refinery")!!).error shouldBe null
            game.resolveStack()

            game.isInGraveyard(1, "Slagstone Refinery") shouldBe true
            game.findAllPermanents("Powerstone") shouldHaveSize 1
        }

        test("the Refinery exiled together with another nontoken artifact triggers once for each") {
            // Ruling 2022-10-14: leaving at the same time as other nontoken artifacts, it triggers once
            // for each of them plus once for itself (CR 603.10a look-back).
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Slagstone Refinery")
                .withCardOnBattlefield(1, "Ornithopter")
                .withCardInHand(1, "Decree of Annihilation")
                .withLandsOnBattlefield(1, "Mountain", 10)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Decree of Annihilation").error shouldBe null
            game.resolveStack()

            game.isInExile(1, "Slagstone Refinery") shouldBe true
            game.isInExile(1, "Ornithopter") shouldBe true
            game.findAllPermanents("Powerstone") shouldHaveSize 2
        }

        test("an artifact token you control being destroyed does not trigger") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Slagstone Refinery")
                .withCardOnBattlefield(1, "Ornithopter", isToken = true)
                .withCardInHand(1, "Shatter")
                .withLandsOnBattlefield(1, "Mountain", 2)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Shatter", game.findPermanent("Ornithopter")!!).error shouldBe null
            game.resolveStack()

            game.findPermanent("Ornithopter") shouldBe null
            game.findAllPermanents("Powerstone").shouldBeEmpty()
        }

        test("an opponent's artifact being destroyed does not trigger") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Slagstone Refinery")
                .withCardOnBattlefield(2, "Ornithopter")
                .withCardInHand(1, "Shatter")
                .withLandsOnBattlefield(1, "Mountain", 2)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Shatter", game.findPermanent("Ornithopter")!!).error shouldBe null
            game.resolveStack()

            game.isInGraveyard(2, "Ornithopter") shouldBe true
            game.findAllPermanents("Powerstone").shouldBeEmpty()
        }
    }
}
