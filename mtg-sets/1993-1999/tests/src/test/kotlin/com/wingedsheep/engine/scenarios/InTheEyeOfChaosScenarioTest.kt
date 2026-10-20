package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * In the Eye of Chaos — "Whenever a player casts an instant spell, counter it unless that player
 * pays {X}, where X is its mana value."
 */
class InTheEyeOfChaosScenarioTest : ScenarioTestBase() {
    init {
        cardRegistry.register(card("Eye X instant") {
            manaCost = "{X}"; typeLine = "Instant"
            spell { effect = Effects.GainLife(DynamicAmounts.xValue()) }
        })

        test("an instant whose caster can't pay its mana value again is countered") {
            val game = scenario().withPlayers()
                .withCardOnBattlefield(1, "In the Eye of Chaos")
                .withCardInHand(2, "Lightning Bolt")
                .withLandsOnBattlefield(2, "Mountain", 1)
                .withActivePlayer(2).build()

            game.castSpellTargetingPlayer(2, "Lightning Bolt", 1).error shouldBe null
            game.resolveStack()

            game.state.pendingDecision shouldBe null
            game.isInGraveyard(2, "Lightning Bolt") shouldBe true
            game.getLifeTotal(1) shouldBe 20
        }

        test("the caster, not the enchantment's controller, pays {X} and the instant resolves") {
            val game = scenario().withPlayers()
                .withCardOnBattlefield(1, "In the Eye of Chaos")
                .withCardInHand(2, "Lightning Bolt")
                .withLandsOnBattlefield(2, "Mountain", 2)
                .withActivePlayer(2).build()

            game.castSpellTargetingPlayer(2, "Lightning Bolt", 1).error shouldBe null
            game.resolveStack()

            game.state.pendingDecision.shouldBeInstanceOf<YesNoDecision>().playerId shouldBe game.player2Id
            game.answerYesNo(true).error shouldBe null
            game.submitManaSourcesAutoPay().error shouldBe null
            game.resolveStack()

            game.getLifeTotal(1) shouldBe 17
        }

        test("its own controller's instants are taxed too, and X counts toward the mana value") {
            val game = scenario().withPlayers()
                .withCardOnBattlefield(1, "In the Eye of Chaos")
                .withCardInHand(1, "Eye X instant")
                .withLandsOnBattlefield(1, "Island", 5)
                .withActivePlayer(1).build()

            // X = 3 spends three Islands; the trigger asks for {3}, and only two remain.
            game.castXSpell(1, "Eye X instant", 3).error shouldBe null
            game.resolveStack()

            game.state.pendingDecision shouldBe null
            game.isInGraveyard(1, "Eye X instant") shouldBe true
            game.getLifeTotal(1) shouldBe 20
        }

        test("sorceries are not taxed") {
            val game = scenario().withPlayers()
                .withCardOnBattlefield(1, "In the Eye of Chaos")
                .withCardInHand(2, "Lava Axe")
                .withLandsOnBattlefield(2, "Mountain", 5)
                .withActivePlayer(2).build()

            game.castSpellTargetingPlayer(2, "Lava Axe", 1).error shouldBe null
            game.resolveStack()

            game.state.pendingDecision shouldBe null
            game.getLifeTotal(1) shouldBe 15
        }
    }
}
