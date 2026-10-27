package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.state.components.battlefield.PhasedOutComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.player.AdditionalUpkeepStepsComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.echo
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Echo (CR 702.30), through the `echo(cost)` builder.
 *
 *  - 702.30a: "At the beginning of your upkeep, if this permanent came under your control since the
 *    beginning of your last upkeep, sacrifice it unless you pay [cost]." — so it is owed once, on
 *    the first upkeep after it entered or changed control, and again whenever it comes under your
 *    control anew (a new object, or control regained).
 */
class EchoTest : FunSpec({

    val echoBear = card("Test Echo Bear") {
        manaCost = "{1}{R}"
        typeLine = "Creature — Bear"
        power = 2
        toughness = 2
        oracleText = "Echo {1}{R}"
        echo(ManaCost.parse("{1}{R}"))
    }

    fun driver(): GameTestDriver = GameTestDriver().apply {
        registerCards(TestCards.all + listOf(echoBear))
        initMirrorMatch(deck = Deck.of("Mountain" to 40), startingLife = 20)
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    fun GameTestDriver.onBattlefield(id: EntityId): Boolean = id in state.getBattlefield()

    /** Advance to [player]'s next upkeep, with any echo trigger on the stack. */
    fun GameTestDriver.toNextUpkeepOf(player: EntityId) {
        if (state.activePlayerId == player) passPriorityUntil(Step.PRECOMBAT_MAIN, getOpponent(player))
        passPriorityUntil(Step.UPKEEP, player)
    }

    fun GameTestDriver.giveControl(id: EntityId, to: EntityId) =
        replaceState(state.updateEntity(id) { it.with(ControllerComponent(to)) })

    test("the builder adds the display keyword") {
        echoBear.keywords shouldContain Keyword.ECHO
    }

    test("owed on the first upkeep after it entered: pay and it stays, and it isn't asked again") {
        val d = driver()
        val me = d.activePlayer!!
        val bear = d.putPermanentOnBattlefield(me, "Test Echo Bear")

        d.toNextUpkeepOf(me)
        d.stackSize shouldBe 1
        d.giveMana(me, Color.RED, 2)
        d.bothPass()
        d.pendingDecision.shouldBeInstanceOf<YesNoDecision>().playerId shouldBe me
        d.submitYesNo(me, true)
        d.onBattlefield(bear) shouldBe true

        d.toNextUpkeepOf(me)
        d.stackSize shouldBe 0
        d.toNextUpkeepOf(me)
        d.stackSize shouldBe 0
        d.onBattlefield(bear) shouldBe true
    }

    test("declining to pay sacrifices it") {
        val d = driver()
        val me = d.activePlayer!!
        val bear = d.putPermanentOnBattlefield(me, "Test Echo Bear")

        d.toNextUpkeepOf(me)
        d.giveMana(me, Color.RED, 2)
        d.bothPass()
        d.submitYesNo(me, false)
        d.onBattlefield(bear) shouldBe false
        d.getGraveyard(me) shouldContain bear
    }

    test("an unaffordable echo cost sacrifices it without a prompt") {
        val d = driver()
        val me = d.activePlayer!!
        val bear = d.putPermanentOnBattlefield(me, "Test Echo Bear")

        d.toNextUpkeepOf(me)
        d.giveColorlessMana(me, 2)
        d.bothPass()
        d.pendingDecision shouldBe null
        d.onBattlefield(bear) shouldBe false
    }

    test("it doesn't trigger on an opponent's upkeep") {
        val d = driver()
        val me = d.activePlayer!!
        d.putPermanentOnBattlefield(me, "Test Echo Bear")

        d.toNextUpkeepOf(d.getOpponent(me))
        d.stackSize shouldBe 0
    }

    test("one that came under your control during your upkeep is owed at your next upkeep") {
        val d = driver()
        val me = d.activePlayer!!
        d.toNextUpkeepOf(me)
        // After the beginning of this upkeep, so it "came under your control since" it.
        val bear = d.putPermanentOnBattlefield(me, "Test Echo Bear")
        d.stackSize shouldBe 0

        d.toNextUpkeepOf(me)
        d.stackSize shouldBe 1
        d.bothPass()
        d.onBattlefield(bear) shouldBe false
    }

    test("a player who gains control of it owes the echo on their own next upkeep") {
        val d = driver()
        val me = d.activePlayer!!
        val opp = d.getOpponent(me)
        val bear = d.putPermanentOnBattlefield(me, "Test Echo Bear")
        d.toNextUpkeepOf(me)
        d.giveMana(me, Color.RED, 2)
        d.bothPass()
        d.submitYesNo(me, true)

        d.giveControl(bear, opp)
        d.toNextUpkeepOf(opp)
        d.stackSize shouldBe 1
        d.giveMana(opp, Color.RED, 2)
        d.bothPass()
        d.pendingDecision.shouldBeInstanceOf<YesNoDecision>().playerId shouldBe opp
        d.submitYesNo(opp, false)
        d.onBattlefield(bear) shouldBe false
        d.getGraveyard(me) shouldContain bear
    }

    test("regaining control after losing it makes the echo owed again") {
        val d = driver()
        val me = d.activePlayer!!
        val opp = d.getOpponent(me)
        val bear = d.putPermanentOnBattlefield(me, "Test Echo Bear")
        d.toNextUpkeepOf(me)
        d.giveMana(me, Color.RED, 2)
        d.bothPass()
        d.submitYesNo(me, true)

        // Lose control, let the game see it, then take it back — all before my next upkeep.
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        d.giveControl(bear, opp)
        d.passPriorityUntil(Step.END)
        d.giveControl(bear, me)

        d.toNextUpkeepOf(me)
        d.stackSize shouldBe 1
    }

    test("phasing out and back in doesn't make it owed again (CR 702.26d)") {
        val d = driver()
        val me = d.activePlayer!!
        val bear = d.putPermanentOnBattlefield(me, "Test Echo Bear")
        d.toNextUpkeepOf(me)
        d.giveMana(me, Color.RED, 2)
        d.bothPass()
        d.submitYesNo(me, true)

        // Phased out (Oubliette-style, so it stays out) across my next upkeep, then phased back in.
        d.replaceState(d.state.updateEntity(bear) { it.with(PhasedOutComponent(me, phaseInOnSourceLeaves = me)) })
        d.toNextUpkeepOf(me)
        d.stackSize shouldBe 0
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        d.replaceState(d.state.updateEntity(bear) { it.without<PhasedOutComponent>() })

        d.toNextUpkeepOf(me)
        d.stackSize shouldBe 0
        d.onBattlefield(bear) shouldBe true
    }

    test("an additional upkeep step counts as your last upkeep") {
        val d = driver()
        val me = d.activePlayer!!
        val paid = d.putPermanentOnBattlefield(me, "Test Echo Bear")
        d.toNextUpkeepOf(me)
        d.giveMana(me, Color.RED, 2)
        d.bothPass()
        d.submitYesNo(me, true)

        // Arrives after this turn's upkeep began, so the inserted upkeep step asks for it; the
        // one already paid has been controlled since this turn's upkeep and isn't asked again.
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val fresh = d.putPermanentOnBattlefield(me, "Test Echo Bear")
        d.replaceState(d.state.updateEntity(me) { it.with(AdditionalUpkeepStepsComponent(1)) })
        d.passPriorityUntil(Step.UPKEEP)
        d.stackSize shouldBe 1
        d.bothPass()
        d.onBattlefield(fresh) shouldBe false
        d.onBattlefield(paid) shouldBe true
    }

    test("if it left the battlefield before the trigger resolves, nothing happens (intervening if)") {
        val d = driver()
        val me = d.activePlayer!!
        val bear = d.putPermanentOnBattlefield(me, "Test Echo Bear")

        d.toNextUpkeepOf(me)
        d.stackSize shouldBe 1
        d.moveToGraveyard(bear)
        d.giveMana(me, Color.RED, 2)
        d.bothPass()
        d.pendingDecision shouldBe null
    }
})
