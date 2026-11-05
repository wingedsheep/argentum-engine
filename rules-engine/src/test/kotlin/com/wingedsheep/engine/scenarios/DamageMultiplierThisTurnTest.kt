package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.mechanics.layers.SerializableModification
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.AmplifyDamageThisTurnEffect
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * The multiplying form of the turn-scoped damage replacement effect (CR 614):
 * `Effects.DoubleDamageThisTurn` — `AmplifyDamageThisTurnEffect` with `multiplier = 2` — as on
 * Insult // Injury: "If a source you control would deal damage this turn, it deals double that
 * damage instead."
 */
class DamageMultiplierThisTurnTest : FunSpec({
    val doubler = card("Turn Doubler Spell") {
        manaCost = "{0}"
        typeLine = "Sorcery"
        spell { effect = Effects.DoubleDamageThisTurn(EventPattern.DamageEvent(source = GameObjectFilter.Any.youControl())) }
    }
    val bonus = card("Turn Bonus Spell") {
        manaCost = "{0}"
        typeLine = "Sorcery"
        spell {
            effect = Effects.AmplifyDamageThisTurn(
                DynamicAmounts.fixed(2), EventPattern.DamageEvent(source = GameObjectFilter.Any.youControl())
            )
        }
    }

    fun driver() = GameTestDriver().apply {
        registerCards(TestCards.all + listOf(doubler, bonus))
        initMirrorMatch(Deck.of("Mountain" to 40), startingPlayer = 0)
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    fun GameTestDriver.cast(name: String, targets: List<EntityId> = emptyList()) {
        if (name == "Lightning Bolt") giveMana(player1, Color.RED)
        castSpell(player1, putCardInHand(player1, name), targets).error shouldBe null
        bothPass().error shouldBe null
    }

    fun GameTestDriver.amplifiers() =
        state.floatingEffects.filter { it.effect.modification is SerializableModification.AmplifyDamage }

    test("doubles damage from a source its controller controls for the rest of the turn") {
        val d = driver()
        d.cast("Turn Doubler Spell")
        d.cast("Lightning Bolt", listOf(d.player2))
        d.getLifeTotal(d.player2) shouldBe 14
        d.cast("Lightning Bolt", listOf(d.player2))
        d.getLifeTotal(d.player2) shouldBe 8
    }

    test("two installs each double once, quadrupling") {
        val d = driver()
        d.cast("Turn Doubler Spell")
        d.cast("Turn Doubler Spell")
        d.cast("Lightning Bolt", listOf(d.player2))
        d.getLifeTotal(d.player2) shouldBe 8
    }

    test("does not apply to a source another player controls") {
        val d = driver()
        d.cast("Turn Doubler Spell")
        val opponentBolt = d.putCardInHand(d.player2, "Lightning Bolt")
        d.passPriority(d.player1)
        d.giveMana(d.player2, Color.RED)
        d.castSpell(d.player2, opponentBolt, listOf(d.player1)).error shouldBe null
        d.bothPass().error shouldBe null
        d.getLifeTotal(d.player1) shouldBe 17
    }

    for ((first, damage) in listOf("double" to 8, "add 2" to 10)) {
        test("the affected player orders it against a +2 bonus, applying $first first") {
            val d = driver()
            d.cast("Turn Doubler Spell")
            d.cast("Turn Bonus Spell")
            d.cast("Lightning Bolt", listOf(d.player2))
            val choice = d.pendingDecision.shouldBeInstanceOf<ChooseOptionDecision>()
            choice.playerId shouldBe d.player2
            val index = choice.options.indexOfFirst { it.contains(first, ignoreCase = true) }
            (index >= 0) shouldBe true
            d.submitDecision(d.player2, OptionChosenResponse(choice.id, index)).error shouldBe null
            d.getLifeTotal(d.player2) shouldBe 20 - damage
        }
    }

    test("expires at end of turn") {
        val d = driver()
        d.cast("Turn Doubler Spell")
        d.amplifiers().size shouldBe 1
        d.passPriorityUntil(Step.UPKEEP)
        d.amplifiers().size shouldBe 0
    }

    test("describes itself as doubling") {
        val effect = Effects.DoubleDamageThisTurn(EventPattern.DamageEvent(source = GameObjectFilter.Any.youControl()))
            .shouldBeInstanceOf<AmplifyDamageThisTurnEffect>()
        effect.multiplier shouldBe 2
        effect.description shouldContain "double that damage instead"
    }
})
