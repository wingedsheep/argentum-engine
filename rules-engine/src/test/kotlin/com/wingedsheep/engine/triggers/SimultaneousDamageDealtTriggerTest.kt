package com.wingedsheep.engine.triggers

import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.events.Recipient
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * The source side of the simultaneous-damage fold: "whenever this deals [combat] damage", with no
 * recipient restriction, triggers once when combat damage reaches several recipients at once —
 * trample over a blocker, or several blockers — carrying the total (Vessel of the All-Consuming's
 * and Umezawa's Jitte's rulings, CR 603.2c / CR 510.2). A recipient-restricted form ("deals damage
 * to a creature") still triggers once per recipient.
 */
class SimultaneousDamageDealtTriggerTest : FunSpec({

    // "Whenever this creature deals damage, if it was 3 or more, you gain that much life." Two
    // 2-damage triggers both miss the gate, so the life total tells one folded trigger apart.
    val threeOrMore = Conditions.CompareAmounts(DynamicAmounts.triggerDamageAmount(), ComparisonOperator.GTE, 3)

    val trampler = card("Test Folding Trampler") {
        manaCost = "{1}"
        typeLine = "Creature — Beast"
        power = 4
        toughness = 4
        keywords(Keyword.TRAMPLE)
        triggeredAbility {
            trigger = Triggers.self.dealsDamage()
            triggerRestriction = threeOrMore
            effect = Effects.GainLife(DynamicAmounts.triggerDamageAmount())
        }
    }

    val brawler = card("Test Folding Brawler") {
        manaCost = "{1}"
        typeLine = "Creature — Beast"
        power = 4
        toughness = 4
        triggeredAbility {
            trigger = Triggers.self.dealsCombatDamage()
            triggerRestriction = threeOrMore
            effect = Effects.GainLife(DynamicAmounts.triggerDamageAmount())
        }
    }

    // "Whenever this creature deals damage to a creature, you gain 1 life."
    val perCreature = card("Test Per Creature Brawler") {
        manaCost = "{1}"
        typeLine = "Creature — Beast"
        power = 4
        toughness = 4
        triggeredAbility {
            trigger = Triggers.self.dealsDamage(Recipient.AnyCreature)
            effect = Effects.GainLife(1)
        }
    }

    val bear = card("Test Bear") {
        manaCost = "{1}"
        typeLine = "Creature — Bear"
        power = 0
        toughness = 2
    }

    fun createDriver(): GameTestDriver = GameTestDriver().apply {
        registerCards(TestCards.all + listOf(trampler, brawler, perCreature, bear))
        initMirrorMatch(deck = Deck.of("Mountain" to 40), startingLife = 20)
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    /** [attacker] attacks; each of [blockers] blocks it; combat damage uses the default split. */
    fun GameTestDriver.attackIntoBlockers(attacker: EntityId, blockers: List<EntityId>) {
        val active = activePlayer!!
        val defender = getOpponent(active)
        (blockers + attacker).forEach(::removeSummoningSickness)
        passPriorityUntil(Step.DECLARE_ATTACKERS)
        declareAttackers(active, listOf(attacker), defender).outcome shouldBe Outcome.Done
        bothPass()
        declareBlockers(defender, blockers.associateWith { listOf(attacker) }).outcome shouldBe Outcome.Done
        passPriorityUntil(Step.END_COMBAT)
    }

    test("trampling over a blocker is one trigger carrying the total") {
        val d = createDriver()
        val me = d.activePlayer!!
        val them = d.getOpponent(me)
        val attacker = d.putCreatureOnBattlefield(me, "Test Folding Trampler")
        val blocker = d.putCreatureOnBattlefield(them, "Test Bear")

        d.attackIntoBlockers(attacker, listOf(blocker))

        // 2 to the bear, 2 tramples over: one trigger for 4, not two of 2.
        d.getLifeTotal(them) shouldBe 18
        d.getLifeTotal(me) shouldBe 24
    }

    test("combat damage to two blockers is one trigger for the combat-damage wording too") {
        val d = createDriver()
        val me = d.activePlayer!!
        val them = d.getOpponent(me)
        val attacker = d.putCreatureOnBattlefield(me, "Test Folding Brawler")
        val b1 = d.putCreatureOnBattlefield(them, "Test Bear")
        val b2 = d.putCreatureOnBattlefield(them, "Test Bear")

        d.attackIntoBlockers(attacker, listOf(b1, b2))

        d.getLifeTotal(me) shouldBe 24
    }

    test("a recipient-restricted trigger still fires once per damaged creature") {
        val d = createDriver()
        val me = d.activePlayer!!
        val them = d.getOpponent(me)
        val attacker = d.putCreatureOnBattlefield(me, "Test Per Creature Brawler")
        val b1 = d.putCreatureOnBattlefield(them, "Test Bear")
        val b2 = d.putCreatureOnBattlefield(them, "Test Bear")

        d.attackIntoBlockers(attacker, listOf(b1, b2))

        d.getLifeTotal(me) shouldBe 22
    }
})
