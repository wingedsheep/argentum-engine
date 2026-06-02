package com.wingedsheep.engine.handlers.triggers

import com.wingedsheep.engine.core.AttackersDeclaredEvent
import com.wingedsheep.engine.event.TriggerDetector
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.EventPattern.AttackEvent
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TriggerBinding
import com.wingedsheep.sdk.scripting.TriggerSpec
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe

/**
 * BDD tests for the per-creature filtered attack trigger:
 * "Whenever a creature you control with menace attacks, [do X]."
 *
 * The trigger must fire once per qualifying attacker — not once per attack step
 * and not once per declare-attackers event. The menace predicate is evaluated
 * against the projected state of each attacking creature at the moment of
 * attack declaration.
 */
class WheneverACreatureYouControlWithMenaceAttacksTest : FunSpec({

    val menaceWarrior = CardDefinition.creature(
        name = "Test Menace Warrior",
        manaCost = ManaCost.parse("{1}{R}"),
        subtypes = setOf(Subtype("Warrior")),
        power = 2,
        toughness = 2,
        keywords = setOf(Keyword.MENACE)
    )

    val vanillaBear = CardDefinition.creature(
        name = "Test Vanilla Bear",
        manaCost = ManaCost.parse("{1}{G}"),
        subtypes = setOf(Subtype("Bear")),
        power = 2,
        toughness = 2
    )

    // Observer permanent: "Whenever a creature you control with menace attacks, gain 1 life."
    val menaceObserver = card("Menace Observer") {
        manaCost = "{2}"
        typeLine = "Enchantment"

        triggeredAbility {
            trigger = TriggerSpec(
                AttackEvent(filter = GameObjectFilter.Creature.withKeyword(Keyword.MENACE).youControl()),
                TriggerBinding.ANY
            )
            effect = Effects.GainLife(1)
        }
    }

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(menaceWarrior, vanillaBear, menaceObserver))
        driver.initMirrorMatch(deck = Deck.of("Forest" to 20, "Mountain" to 20))
        return driver
    }

    fun detectorFor(driver: GameTestDriver): TriggerDetector = TriggerDetector(driver.cardRegistry)

    context("per-creature filtered attack trigger fires once per qualifying attacker") {

        test("two menace attackers and one non-menace attacker produce exactly two trigger instances") {
            val driver = createDriver()
            driver.putPermanentOnBattlefield(driver.player1, "Menace Observer")
            val m1 = driver.putCreatureOnBattlefield(driver.player1, "Test Menace Warrior")
            val m2 = driver.putCreatureOnBattlefield(driver.player1, "Test Menace Warrior")
            val nm = driver.putCreatureOnBattlefield(driver.player1, "Test Vanilla Bear")

            val event = AttackersDeclaredEvent(
                attackers = listOf(m1, m2, nm),
                attackingPlayerId = driver.player1
            )

            val triggers = detectorFor(driver).detectTriggers(driver.state, listOf(event))

            // Exactly two instances — one per menace attacker, not one batch and not three
            triggers shouldHaveSize 2
        }

        test("non-menace attacker alone does not fire the menace-gated trigger") {
            val driver = createDriver()
            driver.putPermanentOnBattlefield(driver.player1, "Menace Observer")
            val nm = driver.putCreatureOnBattlefield(driver.player1, "Test Vanilla Bear")

            val event = AttackersDeclaredEvent(
                attackers = listOf(nm),
                attackingPlayerId = driver.player1
            )

            val triggers = detectorFor(driver).detectTriggers(driver.state, listOf(event))

            triggers.filter { it.sourceName == "Menace Observer" }.shouldBeEmpty()
        }

        test("menace predicate is scoped to creatures controlled by the observer's controller — opponent's menace attacker does not fire the trigger") {
            val driver = createDriver()
            // Observer controlled by player1; attacker controlled by player2
            driver.putPermanentOnBattlefield(driver.player1, "Menace Observer")
            val opponentMenace = driver.putCreatureOnBattlefield(driver.player2, "Test Menace Warrior")

            val event = AttackersDeclaredEvent(
                attackers = listOf(opponentMenace),
                attackingPlayerId = driver.player2
            )

            val triggers = detectorFor(driver).detectTriggers(driver.state, listOf(event))

            triggers.filter { it.sourceName == "Menace Observer" }.shouldBeEmpty()
        }

        test("each trigger instance carries the triggering attacker's entity id in context") {
            val driver = createDriver()
            driver.putPermanentOnBattlefield(driver.player1, "Menace Observer")
            val m1 = driver.putCreatureOnBattlefield(driver.player1, "Test Menace Warrior")
            val m2 = driver.putCreatureOnBattlefield(driver.player1, "Test Menace Warrior")

            val event = AttackersDeclaredEvent(
                attackers = listOf(m1, m2),
                attackingPlayerId = driver.player1
            )

            val triggers = detectorFor(driver).detectTriggers(driver.state, listOf(event))

            triggers shouldHaveSize 2
            val triggeringIds = triggers.map { it.triggerContext.triggeringEntityId }.toSet()
            triggeringIds shouldBe setOf(m1, m2)
        }
    }
})
