package com.wingedsheep.engine.mechanics.combat

import com.wingedsheep.engine.core.LifeChangeReason
import com.wingedsheep.engine.core.LifeChangedEvent
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe

/**
 * A "prevent the next N damage … you gain life equal to the damage prevented this way" shield
 * (`PreventDamage(amount = …, gainLifeFromPrevented = true)`, Candles' Glow).
 *
 * All combat damage in a step is one simultaneous event (CR 510.2), so everything the shield
 * prevents in a step is one life gain — a "whenever you gain life" trigger sees it once. Noncombat
 * damage events each gain separately. An ordinary amount shield gains nothing.
 */
class PreventNextDamageLifeGainTest : FunSpec({

    fun shield(name: String, gainsLife: Boolean) = card(name) {
        manaCost = "{W}"
        typeLine = "Instant"
        spell {
            val t = target(Targets.Any)
            effect = Effects.PreventDamage(target = t, amount = DynamicAmount.Fixed(5), gainLifeFromPrevented = gainsLife)
        }
    }
    val glow = shield("Test Life Shield", gainsLife = true)
    val plain = shield("Test Plain Shield", gainsLife = false)

    fun driver(): GameTestDriver {
        val d = GameTestDriver()
        d.registerCards(TestCards.all + listOf(glow, plain))
        d.initMirrorMatch(deck = Deck.of("Plains" to 40), skipMulligans = true, startingLife = 20)
        return d
    }

    fun GameTestDriver.castShield(caster: EntityId, name: String, target: EntityId) {
        val spell = putCardInHand(caster, name)
        giveMana(caster, Color.WHITE, 1)
        castSpell(caster, spell, listOf(target)).outcome shouldBe Outcome.Done
        bothPass()
    }

    fun GameTestDriver.lifeGainsOf(player: EntityId) =
        events.filterIsInstance<LifeChangedEvent>().filter { it.playerId == player && it.reason == LifeChangeReason.LIFE_GAIN }

    fun GameTestDriver.attackWithTwoLions(attacker: EntityId, defender: EntityId) {
        val lions = List(2) { putCreatureOnBattlefield(attacker, "Savannah Lions").also { removeSummoningSickness(it) } }
        passPriorityUntil(Step.DECLARE_ATTACKERS)
        declareAttackers(attacker, lions, defender).outcome shouldBe Outcome.Done
        passPriorityUntil(Step.DECLARE_BLOCKERS)
        declareNoBlockers(defender).outcome shouldBe Outcome.Done
        passPriorityUntil(Step.POSTCOMBAT_MAIN)
    }

    test("damage from two attackers in one combat damage step is prevented and gained as one event") {
        val d = driver()
        val p1 = d.activePlayer!!
        val p2 = d.getOpponent(p1)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)

        d.castShield(p1, "Test Life Shield", p2)
        d.attackWithTwoLions(p1, p2)

        d.assertLifeTotal(p2, 20)
        d.assertLifeTotal(p1, 22)
        d.lifeGainsOf(p1) shouldHaveSize 1
    }

    test("separate noncombat damage events each gain what they prevented") {
        val d = driver()
        val p1 = d.activePlayer!!
        val p2 = d.getOpponent(p1)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)

        d.castShield(p1, "Test Life Shield", p2)
        repeat(2) {
            val bolt = d.putCardInHand(p1, "Lightning Bolt")
            d.giveMana(p1, Color.RED, 1)
            d.castSpell(p1, bolt, listOf(p2)).outcome shouldBe Outcome.Done
            d.bothPass()
        }

        // 3, then the shield's last 2 of the second 3, are prevented: two events, two gains.
        d.assertLifeTotal(p2, 19)
        d.assertLifeTotal(p1, 25)
        d.lifeGainsOf(p1) shouldHaveSize 2
    }

    test("an ordinary amount shield prevents without gaining life") {
        val d = driver()
        val p1 = d.activePlayer!!
        val p2 = d.getOpponent(p1)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)

        d.castShield(p1, "Test Plain Shield", p2)
        d.attackWithTwoLions(p1, p2)

        d.assertLifeTotal(p2, 20)
        d.assertLifeTotal(p1, 20)
        d.lifeGainsOf(p1) shouldHaveSize 0
    }
})
