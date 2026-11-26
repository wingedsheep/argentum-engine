package com.wingedsheep.engine

import com.wingedsheep.engine.core.ChooseNumberDecision
import com.wingedsheep.engine.core.CombatResolutionDecision
import com.wingedsheep.engine.core.NumberChosenResponse
import com.wingedsheep.engine.mechanics.layers.SerializableModification
import com.wingedsheep.engine.state.components.battlefield.DamageComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.GameObjectFilter
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * `PreventDamage(amount = N, toGroup = …, alsoToYou = true, onPrevented = …)` — one "prevent the next
 * N damage" pool shared by the controller and a group of their permanents, with a reaction (Divine
 * Deflection's shape, on synthetic spells so the primitive doesn't move with the card). Covers:
 * the pool covers the controller and their permanents but not an opponent's; it is spent across
 * recipients; simultaneous damage over capacity is split by the protected player (CR 615.7); the
 * reaction runs once per damage event for the pool's total, not once per recipient; a recipient
 * that joins the group after resolution is covered (the group is read when damage would be dealt).
 */
class PreventNextDamageGroupPoolTest : FunSpec({

    val reflect = card("Test Pool Reflect") {
        manaCost = "{W}"
        typeLine = "Instant"
        oracleText = "Prevent the next 3 damage that would be dealt to you and/or permanents you control this turn. " +
            "If damage is prevented this way, this deals that much damage to any target."
        spell {
            val victim = target(Targets.Any)
            effect = Effects.PreventDamage(
                amount = DynamicAmounts.fixed(3),
                toGroup = GameObjectFilter.Permanent.youControl(),
                alsoToYou = true,
                onPrevented = Effects.DealDamage(DynamicAmounts.preventedDamage(), victim),
            )
        }
    }

    val insight = card("Test Pool Insight") {
        manaCost = "{W}"
        typeLine = "Instant"
        oracleText = "Prevent the next 3 damage that would be dealt to you and/or permanents you control this turn. " +
            "If damage is prevented this way, draw a card."
        spell {
            effect = Effects.PreventDamage(
                amount = DynamicAmounts.fixed(3),
                toGroup = GameObjectFilter.Permanent.youControl(),
                alsoToYou = true,
                onPrevented = Effects.DrawCards(1),
            )
        }
    }

    val shock = card("Test Pool Shock") {
        manaCost = "{R}"
        typeLine = "Instant"
        spell {
            val t = target(Targets.Any)
            effect = Effects.DealDamage(2, t)
        }
    }

    fun newGame(): GameTestDriver = GameTestDriver().apply {
        registerCards(TestCards.all + reflect + insight + shock)
        initMirrorMatch(Deck.of("Plains" to 40), skipMulligans = true, startingPlayer = 0)
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    fun GameTestDriver.cast(caster: EntityId, name: String, color: Color, targets: List<EntityId> = emptyList()) {
        giveMana(caster, color, 1)
        castSpell(caster, putCardInHand(caster, name), targets).error shouldBe null
        var passes = 0
        while (stackSize > 0 && passes++ < 20) bothPass()
    }

    fun GameTestDriver.damageOn(id: EntityId) = state.getEntity(id)?.get<DamageComponent>()?.amount ?: 0

    fun GameTestDriver.pool() = state.floatingEffects.mapNotNull { it.effect.modification as? SerializableModification.PreventNextDamage }

    test("the pool covers you and your permanents, is spent across them, and reflects what it prevents") {
        val d = newGame()
        val p1 = d.player1
        val p2 = d.player2
        val courser = d.putCreatureOnBattlefield(p1, "Centaur Courser")
        d.cast(p1, "Test Pool Reflect", Color.WHITE, listOf(p2))
        d.pool().single().remainingAmount shouldBe 3

        // 2 of the pool on your creature: prevented, reflected at the opponent.
        d.giveMana(p1, Color.RED, 1)
        d.castSpell(p1, d.putCardInHand(p1, "Test Pool Shock"), listOf(courser)).error shouldBe null
        d.bothPass()
        d.damageOn(courser) shouldBe 0
        d.getLifeTotal(p2) shouldBe 18
        d.pool().single().remainingAmount shouldBe 1

        // The last 1 on you: 2 of the Bolt gets through.
        d.cast(p1, "Lightning Bolt", Color.RED, listOf(p1))
        d.getLifeTotal(p1) shouldBe 18
        d.getLifeTotal(p2) shouldBe 17
        d.pool() shouldBe emptyList()
    }

    test("an opponent's permanent isn't covered and doesn't spend the pool") {
        val d = newGame()
        val p1 = d.player1
        val p2 = d.player2
        val theirs = d.putCreatureOnBattlefield(p2, "Force of Nature")
        d.cast(p1, "Test Pool Reflect", Color.WHITE, listOf(p2))

        d.cast(p1, "Lightning Bolt", Color.RED, listOf(theirs))
        d.damageOn(theirs) shouldBe 3
        d.getLifeTotal(p2) shouldBe 20
        d.pool().single().remainingAmount shouldBe 3
    }

    test("a permanent that arrives after resolution is covered") {
        val d = newGame()
        val p1 = d.player1
        val p2 = d.player2
        d.cast(p1, "Test Pool Reflect", Color.WHITE, listOf(p2))
        val late = d.putCreatureOnBattlefield(p1, "Centaur Courser")

        d.cast(p1, "Lightning Bolt", Color.RED, listOf(late))
        d.damageOn(late) shouldBe 0
        d.getLifeTotal(p2) shouldBe 17
    }

    test("simultaneous combat damage over capacity is split by the protected player, and reacts once") {
        val d = newGame()
        val p1 = d.player1
        val p2 = d.player2
        val attacker1 = d.putCreatureOnBattlefield(p1, "Centaur Courser")
        val attacker2 = d.putCreatureOnBattlefield(p1, "Centaur Courser")
        d.removeSummoningSickness(attacker1)
        d.removeSummoningSickness(attacker2)
        val blocker = d.putCreatureOnBattlefield(p2, "Force of Nature")

        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        d.declareAttackers(p1, listOf(attacker1, attacker2), p2).error shouldBe null
        d.passPriorityUntil(Step.DECLARE_BLOCKERS)
        d.declareBlockers(p2, mapOf(blocker to listOf(attacker1))).error shouldBe null
        // The defender shields themselves and their blocker, then passes into damage.
        d.passPriority(p1)
        d.giveMana(p2, Color.WHITE, 1)
        d.castSpell(p2, d.putCardInHand(p2, "Test Pool Insight")).error shouldBe null
        d.bothPass()
        val handBefore = d.getHandSize(p2)

        d.passPriorityUntil(Step.COMBAT_DAMAGE)
        if (d.pendingDecision is CombatResolutionDecision) d.confirmCombatDamage().error shouldBe null
        // 3 to the defender and 3 to their blocker against a 3-point pool: they choose the split.
        val decision = d.pendingDecision
        decision.shouldBeInstanceOf<ChooseNumberDecision>()
        decision.playerId shouldBe p2
        decision.minValue shouldBe 0
        decision.maxValue shouldBe 3
        d.submitDecision(p2, NumberChosenResponse(decision.id, 2)).error shouldBe null
        d.pendingDecision shouldBe null

        // 6 dealt, 3 prevented between the two recipients as chosen (2 on one, the last 1 on the other).
        ((20 - d.getLifeTotal(p2)) + d.damageOn(blocker)) shouldBe 3
        setOf(20 - d.getLifeTotal(p2), d.damageOn(blocker)) shouldBe setOf(1, 2)
        d.pool() shouldBe emptyList()
        // One damage event, one reaction — not one per recipient.
        d.getHandSize(p2) shouldBe handBefore + 1
    }
})
