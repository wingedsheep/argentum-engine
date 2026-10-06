package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.card
import io.kotest.matchers.shouldBe

class ShamblingSuitScenarioTest : ScenarioTestBase() {
    init {
        cardRegistry.register(card("Suit Test Relic") {
            manaCost = "{1}"
            typeLine = "Enchantment Artifact"
        })
        cardRegistry.register(card("Suit Test Enchantment") {
            manaCost = "{1}"
            typeLine = "Enchantment"
        })

        test("counts itself and each controlled artifact or enchantment once") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Shambling Suit")
                .withCardOnBattlefield(1, "Ornithopter")
                .withCardOnBattlefield(1, "Suit Test Relic")
                .withCardOnBattlefield(1, "Suit Test Enchantment")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardOnBattlefield(2, "Ornithopter")
                .withCardOnBattlefield(2, "Suit Test Enchantment")
                .withLandsOnBattlefield(1, "Forest", 2)
                .withCardInHand(1, "Naturalize")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val suit = game.findPermanent("Shambling Suit")!!
            game.state.projectedState.getProjectedValues(suit)?.power shouldBe 4
            game.state.projectedState.getProjectedValues(suit)?.toughness shouldBe 3

            game.castSpell(1, "Naturalize", game.findPermanent("Suit Test Relic")!!).error shouldBe null
            game.resolveStack()
            game.state.projectedState.getProjectedValues(suit)?.power shouldBe 3
            game.state.projectedState.getProjectedValues(suit)?.toughness shouldBe 3
        }

        test("starts as one three and grows when another artifact enters") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Shambling Suit")
                .withCardInHand(1, "Ornithopter")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val suit = game.findPermanent("Shambling Suit")!!
            game.state.projectedState.getProjectedValues(suit)?.power shouldBe 1
            game.castSpell(1, "Ornithopter").error shouldBe null
            game.resolveStack()
            game.state.projectedState.getProjectedValues(suit)?.power shouldBe 2
            game.state.projectedState.getProjectedValues(suit)?.toughness shouldBe 3
        }
    }
}
