package com.wingedsheep.engine.handlers.triggers

import com.wingedsheep.engine.core.GreatestPowerOpponentCreatureDiedEvent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe

/**
 * BDD specification for the per-controller greatest-power death trigger.
 *
 * The trigger fires from player AP's perspective whenever a creature that an opponent of AP
 * controls dies AND that creature had the strictly greatest power among all creatures that
 * opponent controlled at the instant it left the battlefield. Ties count as "not strictly
 * greatest" — the trigger does not fire for any tied creature.
 *
 * Covered cases:
 * - OP's 3-power creature dies while OP's 5-power is still alive → no fire (3 < 5)
 * - OP's 5-power creature dies while OP's 2-power is still alive → fires (5 > 2, strict greatest)
 * - AP's 7-power creature dies (AP is not OP from AP's perspective) → no fire
 */
class TriggeredAbilityWhenACreatureAnOpponentControlsWithTheGreatestPowerAmongCreaturesThatPlayerControlsDiesTest :
    FunSpec({

        // -----------------------------------------------------------------------
        // Test-only creature definitions with distinct power values
        // -----------------------------------------------------------------------

        // OP creatures
        val opFivePower = CardDefinition.creature(
            name = "OP Five Power Creature",
            manaCost = ManaCost.parse("{4}{G}"),
            subtypes = setOf(Subtype("Test")),
            power = 5,
            toughness = 5
        )
        val opThreePower = CardDefinition.creature(
            name = "OP Three Power Creature",
            manaCost = ManaCost.parse("{2}{G}"),
            subtypes = setOf(Subtype("Test")),
            power = 3,
            toughness = 3
        )
        val opTwoPower = CardDefinition.creature(
            name = "OP Two Power Creature",
            manaCost = ManaCost.parse("{1}{G}"),
            subtypes = setOf(Subtype("Test")),
            power = 2,
            toughness = 2
        )

        // AP creature (control case — controller is not an opponent of AP)
        val apSevenPower = CardDefinition.creature(
            name = "AP Seven Power Creature",
            manaCost = ManaCost.parse("{6}{G}"),
            subtypes = setOf(Subtype("Test")),
            power = 7,
            toughness = 7
        )

        fun createDriver(): GameTestDriver {
            val driver = GameTestDriver()
            driver.registerCards(TestCards.all)
            driver.registerCard(opFivePower)
            driver.registerCard(opThreePower)
            driver.registerCard(opTwoPower)
            driver.registerCard(apSevenPower)
            return driver
        }

        // -----------------------------------------------------------------------
        // Scenario
        // -----------------------------------------------------------------------

        test(
            "death of opponent's strictly-greatest-power creature fires trigger; " +
                "ties and non-greatest deaths do not"
        ) {
            val driver = createDriver()
            driver.initMirrorMatch(
                deck = Deck.of("Forest" to 20, "Swamp" to 20),
                startingLife = 20
            )

            val ap = driver.activePlayer!!
            val op = driver.getOpponent(ap)

            driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

            // ── Battlefield setup ────────────────────────────────────────────────
            // OP controls three creatures with powers 5, 3, and 2.
            // AP controls one creature with power 7 (irrelevant for per-OP comparison).
            val opFiveId = driver.putCreatureOnBattlefield(op, "OP Five Power Creature")
            val opThreeId = driver.putCreatureOnBattlefield(op, "OP Three Power Creature")
            driver.putCreatureOnBattlefield(op, "OP Two Power Creature")  // stays alive; needed as context
            val apSevenId = driver.putCreatureOnBattlefield(ap, "AP Seven Power Creature")

            // ── Resolution 1: kill OP's 3-power while 5-power is still alive ────
            // At this instant OP controls {5, 3, 2}; 3 < 5 → NOT the strict greatest → no fire.
            driver.giveMana(ap, Color.BLACK, 2)
            val doom1 = driver.putCardInHand(ap, "Doom Blade")
            driver.castSpellWithTargets(ap, doom1, listOf(ChosenTarget.Permanent(opThreeId)))
            driver.bothPass()

            // ── Resolution 2: kill OP's 5-power while 2-power is still alive ────
            // At this instant OP controls {5, 2}; 5 > 2 → IS the strict greatest → trigger fires.
            driver.giveMana(ap, Color.BLACK, 2)
            val doom2 = driver.putCardInHand(ap, "Doom Blade")
            driver.castSpellWithTargets(ap, doom2, listOf(ChosenTarget.Permanent(opFiveId)))
            driver.bothPass()

            // ── Control case: kill AP's own 7-power creature ─────────────────────
            // The dying creature is controlled by AP, not an opponent of AP → must NOT fire.
            driver.giveMana(ap, Color.BLACK, 2)
            val doom3 = driver.putCardInHand(ap, "Doom Blade")
            driver.castSpellWithTargets(ap, doom3, listOf(ChosenTarget.Permanent(apSevenId)))
            driver.bothPass()

            // ── Assertions ───────────────────────────────────────────────────────
            val fired = driver.events.filterIsInstance<GreatestPowerOpponentCreatureDiedEvent>()

            // Trigger fires exactly once — only for OP's 5-power creature.
            fired shouldHaveSize 1

            val evt = fired.single()

            // Payload identifies the dying entity by id.
            evt.dyingEntityId shouldBe opFiveId

            // AP is named as the observing player — the filter is evaluated per-opponent
            // using the last-known battlefield snapshot of the dying creature.
            evt.observingPlayerId shouldBe ap
        }
    })
