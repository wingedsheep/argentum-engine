package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.DistributeDecision
import com.wingedsheep.engine.core.DistributionResponse
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.mechanics.layers.SerializableModification
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Candles' Glow (CHK #5) — "Prevent the next 3 damage that would be dealt to any target this turn.
 * You gain life equal to the damage prevented this way. Splice onto Arcane {1}{W}"
 *
 * The life is credited by the shield as it actually prevents damage: a partial prevention gains
 * the prevented part, an unused shield gains nothing, and a combat damage step split across
 * several attackers (CR 615.7 distribution) still credits the caster.
 */
class CandlesGlowScenarioTest : FunSpec({

    val blast = card("Candles Test Blast") {
        manaCost = "{R}"
        typeLine = "Instant"
        spell {
            val t = target(Targets.Any)
            effect = Effects.DealDamage(5, t)
        }
    }

    fun driver(): GameTestDriver {
        val d = GameTestDriver()
        d.registerCards(TestCards.all + listOf(blast))
        d.initMirrorMatch(deck = Deck.of("Plains" to 40), skipMulligans = true, startingLife = 20)
        return d
    }

    fun GameTestDriver.castGlow(caster: EntityId, target: EntityId) {
        val glow = putCardInHand(caster, "Candles' Glow")
        giveMana(caster, Color.WHITE, 2)
        castSpell(caster, glow, listOf(target)).outcome shouldBe Outcome.Done
        bothPass()
    }

    test("prevents 3 damage to a creature and its caster gains 3") {
        val d = driver()
        val p1 = d.activePlayer!!
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val bears = d.putCreatureOnBattlefield(p1, "Grizzly Bears")

        d.castGlow(p1, bears)
        d.assertLifeTotal(p1, 20)

        val bolt = d.putCardInHand(p1, "Lightning Bolt")
        d.giveMana(p1, Color.RED, 1)
        d.castSpell(p1, bolt, listOf(bears)).outcome shouldBe Outcome.Done
        d.bothPass()

        d.findPermanent(p1, "Grizzly Bears") shouldBe bears
        d.assertLifeTotal(p1, 23)
    }

    test("a partial prevention gains only what was prevented, and the shield is spent") {
        val d = driver()
        val p1 = d.activePlayer!!
        val p2 = d.getOpponent(p1)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)

        d.castGlow(p1, p2)

        val blastCard = d.putCardInHand(p1, "Candles Test Blast")
        d.giveMana(p1, Color.RED, 1)
        d.castSpell(p1, blastCard, listOf(p2)).outcome shouldBe Outcome.Done
        d.bothPass()

        // 3 of the 5 prevented: p2 takes 2, p1 (the Glow's controller, not the protected player) gains 3.
        d.assertLifeTotal(p2, 18)
        d.assertLifeTotal(p1, 23)

        // The shield is used up — the next damage is dealt and gains nothing.
        val bolt = d.putCardInHand(p1, "Lightning Bolt")
        d.giveMana(p1, Color.RED, 1)
        d.castSpell(p1, bolt, listOf(p2)).outcome shouldBe Outcome.Done
        d.bothPass()
        d.assertLifeTotal(p2, 15)
        d.assertLifeTotal(p1, 23)
    }

    test("a shield that prevents nothing gains nothing and expires at end of turn") {
        val d = driver()
        val p1 = d.activePlayer!!
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)

        d.castGlow(p1, p1)
        d.state.floatingEffects.any { it.effect.modification is SerializableModification.PreventNextDamage } shouldBe true

        d.passPriorityUntil(Step.UPKEEP)
        d.assertLifeTotal(p1, 20)
        d.state.floatingEffects.any { it.effect.modification is SerializableModification.PreventNextDamage } shouldBe false
    }

    test("combat damage split across two attackers still credits the caster") {
        val d = driver()
        val p1 = d.activePlayer!!
        val p2 = d.getOpponent(p1)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val bears1 = d.putCreatureOnBattlefield(p1, "Grizzly Bears")
        val bears2 = d.putCreatureOnBattlefield(p1, "Grizzly Bears")
        d.removeSummoningSickness(bears1)
        d.removeSummoningSickness(bears2)

        // The attacker shields the defending player: the Glow's controller is the one who gains.
        d.castGlow(p1, p2)

        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        d.declareAttackers(p1, listOf(bears1, bears2), p2).outcome shouldBe Outcome.Done
        d.passPriorityUntil(Step.DECLARE_BLOCKERS)
        d.declareNoBlockers(p2).outcome shouldBe Outcome.Done

        d.passPriorityUntil(Step.COMBAT_DAMAGE)
        // 4 damage from two sources against a 3-point shield: the protected player splits it.
        val decision = d.pendingDecision
        decision.shouldBeInstanceOf<DistributeDecision>()
        decision.playerId shouldBe p2
        d.submitDecision(p2, DistributionResponse(decision.id, mapOf(bears1 to 2, bears2 to 1))).error shouldBe null
        d.passPriorityUntil(Step.POSTCOMBAT_MAIN)

        d.assertLifeTotal(p2, 19)
        d.assertLifeTotal(p1, 23)
        d.state.pendingDecision shouldNotBe decision
    }
})
