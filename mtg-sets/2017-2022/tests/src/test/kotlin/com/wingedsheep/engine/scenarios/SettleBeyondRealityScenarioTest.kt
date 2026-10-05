package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.mh1.cards.SettleBeyondReality
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.effects.ModalEffect
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Settle Beyond Reality (MH1 #27, reprinted J22 #242) — {4}{W} Sorcery.
 *
 *   Choose one or both —
 *   • Exile target creature you don't control.
 *   • Exile target creature you control, then return it to the battlefield under its owner's control.
 */
class SettleBeyondRealityScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(SettleBeyondReality))
        driver.initMirrorMatch(deck = Deck.of("Plains" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun GameTestDriver.castSettle(
        caster: EntityId,
        modes: List<Int>,
        modeTargets: List<List<ChosenTarget>>,
    ): String? {
        giveMana(caster, Color.WHITE, 5)
        val spell = putCardInHand(caster, "Settle Beyond Reality")
        val result = submit(
            CastSpell(
                playerId = caster,
                cardId = spell,
                targets = modeTargets.flatten(),
                chosenModes = modes,
                modeTargetsOrdered = modeTargets,
            )
        )
        if (result.error == null) bothPass()
        return result.error
    }

    test("both modes — the opponent's creature stays exiled, mine comes back as a fresh untapped permanent") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val opp = driver.getOpponent(me)

        val theirs = driver.putCreatureOnBattlefield(opp, "Centaur Courser")
        val mine = driver.putCreatureOnBattlefield(me, "Savannah Lions")
        driver.tapPermanent(mine)

        driver.castSettle(
            me,
            modes = listOf(0, 1),
            modeTargets = listOf(
                listOf(ChosenTarget.Permanent(theirs)),
                listOf(ChosenTarget.Permanent(mine)),
            ),
        ) shouldBe null

        driver.findPermanent(opp, "Centaur Courser") shouldBe null
        driver.getExileCardNames(opp) shouldContain "Centaur Courser"

        val returned = driver.findPermanent(me, "Savannah Lions")
        returned shouldNotBe null
        driver.isTapped(returned!!) shouldBe false
        driver.state.getEntity(returned)!!.get<ControllerComponent>()?.playerId shouldBe me
        driver.getExileCardNames(me) shouldNotContain "Savannah Lions"
    }

    test("only the exile mode — my own creature is untouched") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val opp = driver.getOpponent(me)

        val theirs = driver.putCreatureOnBattlefield(opp, "Centaur Courser")
        val mine = driver.putCreatureOnBattlefield(me, "Savannah Lions")
        driver.tapPermanent(mine)

        driver.castSettle(
            me,
            modes = listOf(0),
            modeTargets = listOf(listOf(ChosenTarget.Permanent(theirs))),
        ) shouldBe null

        driver.getExileCardNames(opp) shouldContain "Centaur Courser"
        driver.findPermanent(me, "Savannah Lions") shouldBe mine
        driver.isTapped(mine) shouldBe true
    }

    test("only the blink mode — the opponent's creature stays on the battlefield") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val opp = driver.getOpponent(me)

        val theirs = driver.putCreatureOnBattlefield(opp, "Centaur Courser")
        val mine = driver.putCreatureOnBattlefield(me, "Savannah Lions")
        driver.tapPermanent(mine)

        driver.castSettle(
            me,
            modes = listOf(1),
            modeTargets = listOf(listOf(ChosenTarget.Permanent(mine))),
        ) shouldBe null

        driver.findPermanent(opp, "Centaur Courser") shouldBe theirs
        val returned = driver.findPermanent(me, "Savannah Lions")
        returned shouldNotBe null
        driver.isTapped(returned!!) shouldBe false
    }

    test("the exile mode cannot target a creature you control") {
        val driver = createDriver()
        val me = driver.activePlayer!!

        val mine = driver.putCreatureOnBattlefield(me, "Savannah Lions")

        driver.castSettle(
            me,
            modes = listOf(0),
            modeTargets = listOf(listOf(ChosenTarget.Permanent(mine))),
        ) shouldNotBe null
        driver.findPermanent(me, "Savannah Lions") shouldBe mine
    }

    test("the blink mode cannot target a creature you don't control") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val opp = driver.getOpponent(me)

        val theirs = driver.putCreatureOnBattlefield(opp, "Centaur Courser")

        driver.castSettle(
            me,
            modes = listOf(1),
            modeTargets = listOf(listOf(ChosenTarget.Permanent(theirs))),
        ) shouldNotBe null
        driver.findPermanent(opp, "Centaur Courser") shouldBe theirs
    }

    test("the card offers exactly its two printed modes, one or both") {
        val modal = SettleBeyondReality.script.spellEffect as ModalEffect
        modal.modes.size shouldBe 2
        modal.chooseCount shouldBe 2
        modal.minChooseCount shouldBe 1
    }
})
