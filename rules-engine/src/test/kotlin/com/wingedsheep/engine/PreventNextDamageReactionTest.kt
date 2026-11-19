package com.wingedsheep.engine

import com.wingedsheep.engine.state.components.battlefield.DamageComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * `PreventDamage(target = t, amount = …, onPrevented = …)` — the amount shield on one target with a
 * reaction ("prevent the next N damage …; if damage is prevented this way, …"), on a synthetic spell
 * so the primitive doesn't move with Acolyte's Reward (covered in `AcolytesRewardScenarioTest`).
 * Covers the reaction's amount, that damage to another recipient neither spends the shield nor runs
 * the reaction, and that the shield's remainder reacts again later.
 */
class PreventNextDamageReactionTest : FunSpec({

    val mercy = card("Test Mercy") {
        manaCost = "{W}"
        typeLine = "Instant"
        oracleText = "Prevent the next 4 damage that would be dealt to target creature this turn. " +
            "If damage is prevented this way, you gain that much life."
        spell {
            val creature = target(TargetFilter.Creature)
            effect = Effects.PreventDamage(
                target = creature,
                amount = DynamicAmounts.fixed(4),
                onPrevented = Effects.GainLife(DynamicAmounts.preventedDamage()),
            )
        }
    }

    fun newGame(): GameTestDriver = GameTestDriver().apply {
        registerCards(TestCards.all + mercy)
        initMirrorMatch(Deck.of("Mountain" to 40), skipMulligans = true, startingPlayer = 0)
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    fun GameTestDriver.cast(name: String, color: Color, target: EntityId) {
        giveMana(player1, color, 1)
        castSpell(player1, putCardInHand(player1, name), listOf(target)).error shouldBe null
        var passes = 0
        while (stackSize > 0 && passes++ < 20) bothPass()
    }

    fun GameTestDriver.damageOn(id: EntityId) = state.getEntity(id)?.get<DamageComponent>()?.amount ?: 0

    test("the reaction gets the amount prevented, and the remainder reacts again") {
        val d = newGame()
        val courser = d.putCreatureOnBattlefield(d.player2, "Centaur Courser")
        d.cast("Test Mercy", Color.WHITE, courser)

        d.cast("Lightning Bolt", Color.RED, courser)
        d.damageOn(courser) shouldBe 0
        d.getLifeTotal(d.player1) shouldBe 23

        d.cast("Lightning Bolt", Color.RED, courser)
        d.damageOn(courser) shouldBe 2
        d.getLifeTotal(d.player1) shouldBe 24
    }

    test("damage to another recipient neither spends the shield nor runs the reaction") {
        val d = newGame()
        val courser = d.putCreatureOnBattlefield(d.player2, "Centaur Courser")
        d.cast("Test Mercy", Color.WHITE, courser)

        d.cast("Lightning Bolt", Color.RED, d.player2)
        d.getLifeTotal(d.player2) shouldBe 17
        d.getLifeTotal(d.player1) shouldBe 20

        d.cast("Lightning Bolt", Color.RED, courser)
        d.damageOn(courser) shouldBe 0
        d.getLifeTotal(d.player1) shouldBe 23
    }
})
