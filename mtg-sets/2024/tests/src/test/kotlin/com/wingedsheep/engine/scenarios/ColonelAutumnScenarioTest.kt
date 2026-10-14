package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.pip.cards.ButchDeLoriaTunnelSnake
import com.wingedsheep.mtg.sets.definitions.pip.cards.ColonelAutumn
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Colonel Autumn — its own exploit, exploit granted to other legendary creatures you control, and the
 * "whenever a creature you control exploits a creature" payoff, including the rulings: it triggers
 * when Colonel Autumn is itself the creature sacrificed, and not when it left before the exploit
 * resolved.
 */
class ColonelAutumnScenarioTest : FunSpec({
    fun setup() = GameTestDriver().apply {
        registerCards(TestCards.all + ColonelAutumn + ButchDeLoriaTunnelSnake)
        initMirrorMatch(deck = Deck.of("Plains" to 40), skipMulligans = true, startingPlayer = 0)
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }
    fun plusOnes(d: GameTestDriver, id: EntityId) =
        d.state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0
    fun drain(d: GameTestDriver) {
        var guard = 0
        while (d.pendingDecision == null && d.state.stack.isNotEmpty() && guard++ < 20) d.bothPass()
    }

    /** Cast [name] and resolve it, leaving its exploit trigger on the stack. */
    fun castToExploitTrigger(d: GameTestDriver, name: String, colors: List<Color>, generic: Int): EntityId {
        val card = d.putCardInHand(d.player1, name)
        colors.forEach { d.giveMana(d.player1, it, 1) }
        if (generic > 0) d.giveColorlessMana(d.player1, generic)
        d.castSpell(d.player1, card).outcome shouldBe Outcome.Done
        d.bothPass() // resolve the creature spell
        return card
    }

    fun castColonel(d: GameTestDriver) =
        castToExploitTrigger(d, ColonelAutumn.name, listOf(Color.WHITE, Color.BLACK), 1)
    fun castButch(d: GameTestDriver) =
        castToExploitTrigger(d, ButchDeLoriaTunnelSnake.name, listOf(Color.BLACK), 1)

    test("Colonel Autumn exploiting another creature puts a +1/+1 counter on each creature you control") {
        val d = setup()
        val bear = d.putCreatureOnBattlefield(d.player1, "Grizzly Bears")
        val fodder = d.putCreatureOnBattlefield(d.player1, "Savannah Lions")
        val theirs = d.putCreatureOnBattlefield(d.player2, "Grizzly Bears")
        val colonel = castColonel(d)
        d.bothPass() // resolve the exploit trigger

        d.pendingDecision.shouldBeInstanceOf<YesNoDecision>()
        d.submitYesNo(d.player1, true)
        d.submitCardSelection(d.player1, listOf(fodder)).error shouldBe null
        drain(d)

        (fodder in d.getGraveyard(d.player1)) shouldBe true
        plusOnes(d, colonel) shouldBe 1
        plusOnes(d, bear) shouldBe 1
        plusOnes(d, theirs) shouldBe 0
    }

    test("Colonel Autumn exploiting itself still triggers its payoff") {
        val d = setup()
        val bear = d.putCreatureOnBattlefield(d.player1, "Grizzly Bears")
        val colonel = castColonel(d)
        d.bothPass()

        d.submitYesNo(d.player1, true)
        d.submitCardSelection(d.player1, listOf(colonel)).error shouldBe null
        drain(d)

        (colonel in d.getGraveyard(d.player1)) shouldBe true
        plusOnes(d, bear) shouldBe 1
    }

    test("Colonel Autumn that left before its exploit resolved doesn't trigger") {
        val d = setup()
        val bear = d.putCreatureOnBattlefield(d.player1, "Grizzly Bears")
        val fodder = d.putCreatureOnBattlefield(d.player1, "Savannah Lions")
        val colonel = castColonel(d)
        d.moveToGraveyard(colonel) // gone in response to its own exploit trigger
        d.bothPass()

        d.pendingDecision.shouldBeInstanceOf<YesNoDecision>()
        d.submitYesNo(d.player1, true)
        d.submitCardSelection(d.player1, listOf(fodder)).error shouldBe null
        drain(d)

        (fodder in d.getGraveyard(d.player1)) shouldBe true
        plusOnes(d, bear) shouldBe 0
    }

    test("other legendary creatures you control have exploit; nonlegendary and opposing ones don't") {
        val d = setup()
        val colonel = d.putCreatureOnBattlefield(d.player1, ColonelAutumn.name)
        val bear = d.putCreatureOnBattlefield(d.player1, "Grizzly Bears")
        val theirButch = d.putCreatureOnBattlefield(d.player2, ButchDeLoriaTunnelSnake.name)
        d.state.projectedState.hasKeyword(bear, Keyword.EXPLOIT) shouldBe false
        d.state.projectedState.hasKeyword(theirButch, Keyword.EXPLOIT) shouldBe false
        d.state.projectedState.hasKeyword(colonel, Keyword.EXPLOIT) shouldBe true

        val butch = castButch(d)
        d.state.projectedState.hasKeyword(butch, Keyword.EXPLOIT) shouldBe true
        d.bothPass() // resolve Butch's granted exploit trigger

        d.pendingDecision.shouldBeInstanceOf<YesNoDecision>()
        d.submitYesNo(d.player1, true)
        d.submitCardSelection(d.player1, listOf(bear)).error shouldBe null
        drain(d)

        (bear in d.getGraveyard(d.player1)) shouldBe true
        plusOnes(d, colonel) shouldBe 1
        plusOnes(d, butch) shouldBe 1
    }

    test("a legendary creature exploiting Colonel Autumn still triggers Colonel Autumn") {
        val d = setup()
        val colonel = d.putCreatureOnBattlefield(d.player1, ColonelAutumn.name)
        val bear = d.putCreatureOnBattlefield(d.player1, "Grizzly Bears")
        val butch = castButch(d)
        d.bothPass()

        d.submitYesNo(d.player1, true)
        d.submitCardSelection(d.player1, listOf(colonel)).error shouldBe null
        drain(d)

        (colonel in d.getGraveyard(d.player1)) shouldBe true
        plusOnes(d, butch) shouldBe 1
        plusOnes(d, bear) shouldBe 1
    }
})
