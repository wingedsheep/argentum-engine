package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.one.cards.BarbedBatterfist
import com.wingedsheep.mtg.sets.definitions.one.cards.MoltenRebuke
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Molten Rebuke — {4}{R} Sorcery (ONE).
 *
 * Choose one or both —
 * • Molten Rebuke deals 5 damage to target creature or planeswalker.
 * • Destroy target Equipment.
 */
class MoltenRebukeScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(MoltenRebuke, BarbedBatterfist))
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun GameTestDriver.cast(
        caster: EntityId,
        modes: List<Int>,
        modeTargets: List<List<ChosenTarget>>,
    ): String? {
        giveMana(caster, Color.RED, 5)
        val spell = putCardInHand(caster, "Molten Rebuke")
        return submit(
            CastSpell(
                playerId = caster,
                cardId = spell,
                targets = modeTargets.flatten(),
                chosenModes = modes,
                modeTargetsOrdered = modeTargets,
            )
        ).error
    }

    test("both modes — 5 damage kills a 5-toughness creature and the Equipment is destroyed") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val opp = driver.getOpponent(me)

        val forceOfNature = driver.putCreatureOnBattlefield(opp, "Force of Nature")
        val fist = driver.putPermanentOnBattlefield(opp, "Barbed Batterfist")

        driver.cast(
            me,
            modes = listOf(0, 1),
            modeTargets = listOf(
                listOf(ChosenTarget.Permanent(forceOfNature)),
                listOf(ChosenTarget.Permanent(fist)),
            ),
        ) shouldBe null
        driver.bothPass()

        driver.findPermanent(opp, "Force of Nature") shouldBe null
        driver.findPermanent(opp, "Barbed Batterfist") shouldBe null
    }

    test("only the Equipment mode — the creature is untouched") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val opp = driver.getOpponent(me)

        val courser = driver.putCreatureOnBattlefield(opp, "Centaur Courser")
        val fist = driver.putPermanentOnBattlefield(opp, "Barbed Batterfist")

        driver.cast(
            me,
            modes = listOf(1),
            modeTargets = listOf(listOf(ChosenTarget.Permanent(fist))),
        ) shouldBe null
        driver.bothPass()

        driver.findPermanent(opp, "Centaur Courser") shouldBe courser
        driver.findPermanent(opp, "Barbed Batterfist") shouldBe null
    }

    test("the Equipment mode cannot target a non-Equipment creature") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val opp = driver.getOpponent(me)

        val courser = driver.putCreatureOnBattlefield(opp, "Centaur Courser")

        driver.cast(
            me,
            modes = listOf(1),
            modeTargets = listOf(listOf(ChosenTarget.Permanent(courser))),
        ) shouldNotBe null
    }
})
