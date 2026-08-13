package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.mkm.cards.DeadlyComplication
import com.wingedsheep.mtg.sets.definitions.mkm.cards.RepeatOffender
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Deadly Complication — "Choose one or both — • Destroy target creature. • Put a +1/+1 counter on
 * target suspected creature you control. You may have it become no longer suspected."
 *
 * What can go wrong here is mostly in mode 2:
 *
 *  1. its target must be narrowed to *suspected* creatures *you* control — a mode whose filter fell
 *     through to "any creature" would still pass a happy-path test, so the illegal-target case is
 *     asserted directly;
 *  2. "you may have it become no longer suspected" is a genuine choice, so declining has to leave the
 *     creature suspected (with its menace and can't-block intact) while the +1/+1 counter still lands
 *     — the counter is not conditional on the choice;
 *  3. accepting must strip the whole suspect application together (CR 701.60c), not just the status.
 *
 * Mode 1 is a plain destroy and is covered as part of the choose-both case.
 */
class DeadlyComplicationScenarioTest : FunSpec({

    fun newDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(DeadlyComplication)
        driver.registerCard(RepeatOffender)
        driver.initMirrorMatch(Deck.of("Swamp" to 40), skipMulligans = true, startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    /** Put a Repeat Offender under [player] and activate it so it becomes suspected. */
    fun suspectedOffender(driver: GameTestDriver, player: EntityId): EntityId {
        val offender = driver.putCreatureOnBattlefield(player, "Repeat Offender")
        driver.giveMana(player, Color.BLACK, 3)
        driver.submitSuccess(
            ActivateAbility(player, offender, RepeatOffender.activatedAbilities.first().id)
        )
        driver.bothPass()
        StateProjector().project(driver.state).isSuspected(offender) shouldBe true
        return offender
    }

    fun counterCount(driver: GameTestDriver, entity: EntityId): Int =
        driver.state.getEntity(entity)?.get<CountersComponent>()
            ?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    test("mode 2 alone — the counter lands and accepting the may strips the whole suspect") {
        val driver = newDriver()
        val offender = suspectedOffender(driver, driver.player1)

        val spell = driver.putCardInHand(driver.player1, "Deadly Complication")
        driver.giveMana(driver.player1, Color.BLACK, 4)
        driver.giveMana(driver.player1, Color.RED, 2)
        driver.submit(
            CastSpell(
                playerId = driver.player1,
                cardId = spell,
                targets = listOf(ChosenTarget.Permanent(offender)),
                chosenModes = listOf(1),
                modeTargetsOrdered = listOf(listOf(ChosenTarget.Permanent(offender)))
            )
        ).error shouldBe null
        driver.bothPass()

        driver.pendingDecision.shouldBeInstanceOf<YesNoDecision>()
        driver.submitYesNo(driver.player1, true)

        val projected = StateProjector().project(driver.state)
        withClue("+1/+1 counter is unconditional") { counterCount(driver, offender) shouldBe 1 }
        withClue("CR 701.60c removes status, menace and can't-block together") {
            projected.isSuspected(offender) shouldBe false
            projected.hasKeyword(offender, Keyword.MENACE) shouldBe false
            projected.cantBlock(offender) shouldBe false
        }
    }

    test("declining the may keeps the creature suspected but still adds the counter") {
        val driver = newDriver()
        val offender = suspectedOffender(driver, driver.player1)

        val spell = driver.putCardInHand(driver.player1, "Deadly Complication")
        driver.giveMana(driver.player1, Color.BLACK, 4)
        driver.giveMana(driver.player1, Color.RED, 2)
        driver.submit(
            CastSpell(
                playerId = driver.player1,
                cardId = spell,
                targets = listOf(ChosenTarget.Permanent(offender)),
                chosenModes = listOf(1),
                modeTargetsOrdered = listOf(listOf(ChosenTarget.Permanent(offender)))
            )
        ).error shouldBe null
        driver.bothPass()

        driver.submitYesNo(driver.player1, false)

        val projected = StateProjector().project(driver.state)
        withClue("the counter does not depend on the choice") {
            counterCount(driver, offender) shouldBe 1
        }
        withClue("declining leaves the suspect designation in place") {
            projected.isSuspected(offender) shouldBe true
            projected.cantBlock(offender) shouldBe true
        }
    }

    test("mode 2 cannot target an unsuspected creature you control") {
        val driver = newDriver()
        val plain = driver.putCreatureOnBattlefield(driver.player1, "Grizzly Bears")

        val spell = driver.putCardInHand(driver.player1, "Deadly Complication")
        driver.giveMana(driver.player1, Color.BLACK, 4)
        driver.giveMana(driver.player1, Color.RED, 2)
        val result = driver.submit(
            CastSpell(
                playerId = driver.player1,
                cardId = spell,
                targets = listOf(ChosenTarget.Permanent(plain)),
                chosenModes = listOf(1),
                modeTargetsOrdered = listOf(listOf(ChosenTarget.Permanent(plain)))
            )
        )
        withClue("the mode is restricted to suspected creatures you control") {
            result.isSuccess shouldBe false
        }
    }

    test("both modes — destroy one creature and pump another") {
        val driver = newDriver()
        val offender = suspectedOffender(driver, driver.player1)
        val victim = driver.putCreatureOnBattlefield(driver.player2, "Grizzly Bears")

        val spell = driver.putCardInHand(driver.player1, "Deadly Complication")
        driver.giveMana(driver.player1, Color.BLACK, 4)
        driver.giveMana(driver.player1, Color.RED, 2)
        driver.submit(
            CastSpell(
                playerId = driver.player1,
                cardId = spell,
                targets = listOf(ChosenTarget.Permanent(victim), ChosenTarget.Permanent(offender)),
                chosenModes = listOf(0, 1),
                modeTargetsOrdered = listOf(
                    listOf(ChosenTarget.Permanent(victim)),
                    listOf(ChosenTarget.Permanent(offender))
                )
            )
        ).error shouldBe null
        driver.bothPass()
        driver.submitYesNo(driver.player1, false)

        withClue("mode 1 destroyed the opponent's creature") {
            driver.findPermanent(driver.player2, "Grizzly Bears") shouldBe null
        }
        withClue("mode 2 still pumped your suspect") { counterCount(driver, offender) shouldBe 1 }
    }
})
