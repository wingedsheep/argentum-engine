package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.pip.cards.BattleOfHooverDam
import com.wingedsheep.sdk.core.*
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.*
import com.wingedsheep.sdk.scripting.events.Recipient
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

class BattleOfHooverDamScenarioTest : FunSpec({
    val doubler = card("Hoover Counter Doubler") {
        typeLine = "Enchantment"
        replacementEffect(ScaleCounterPlacement(appliesTo = EventPattern.CounterPlacementEvent(
            recipient = Recipient.AnyPermanent)))
    }
    val blocker = card("Hoover Counter Prevention") {
        typeLine = "Enchantment"
        staticAbility { ability = CantReceiveCounters(GroupFilter(GameObjectFilter.Creature)) }
    }
    fun setup() = GameTestDriver().apply {
        registerCards(TestCards.all + listOf(BattleOfHooverDam, doubler, blocker))
        initMirrorMatch(Deck.of("Plains" to 40), skipMulligans = true, startingPlayer = 0)
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }
    fun GameTestDriver.chooseMode(mode: String): EntityId {
        repeat(4) { putLandOnBattlefield(player1, "Plains") }
        val id = putCardInHand(player1, BattleOfHooverDam.name)
        castSpell(player1, id).error shouldBe null
        bothPass()
        val decision = pendingDecision.shouldBeInstanceOf<ChooseOptionDecision>()
        submitDecision(player1, OptionChosenResponse(decision.id,
            decision.options.indexOfFirst { it.startsWith(mode) })).error shouldBe null
        return id
    }
    fun GameTestDriver.returnAtEnd(id: EntityId) {
        passPriorityUntil(Step.END)
        val decision = pendingDecision.shouldBeInstanceOf<ChooseTargetsDecision>()
        (id in decision.legalTargets.getValue(0)) shouldBe true
        submitTargetSelection(player1, listOf(id)).error shouldBe null
        bothPass()
    }
    fun GameTestDriver.bolt(id: EntityId) {
        val spell = putCardInHand(player1, "Lightning Bolt")
        giveMana(player1, Color.RED)
        castSpell(player1, spell, listOf(id)).error shouldBe null
        bothPass()
    }
    test("NCR returns a small creature with finality and its later death is exiled") {
        val d = setup()
        val creature = d.putCardInGraveyard(d.player1, "Grizzly Bears")
        val large = d.putCardInGraveyard(d.player1, "Serra Angel")
        d.putCardInGraveyard(d.player2, "Grizzly Bears")
        d.chooseMode("NCR")
        d.passPriorityUntil(Step.END)
        val targets = d.pendingDecision.shouldBeInstanceOf<ChooseTargetsDecision>().legalTargets.getValue(0)
        targets shouldBe listOf(creature)
        d.submitTargetSelection(d.player1, listOf(creature)).error shouldBe null
        d.bothPass()
        (creature in d.getPermanents(d.player1)) shouldBe true
        d.state.getEntity(creature)!!.get<CountersComponent>()!!.getCount(CounterType.FINALITY) shouldBe 1
        (large in d.getGraveyard(d.player1)) shouldBe true
        d.bolt(creature)
        (creature in d.state.getExile(d.player1)) shouldBe true
        d.state.stack.isEmpty() shouldBe true
    }
    test("NCR's finality counters honor doubling and counter prevention") {
        for (prevent in listOf(false, true)) {
            val d = setup()
            d.putPermanentOnBattlefield(d.player1, if (prevent) blocker.name else doubler.name)
            val creature = d.putCardInGraveyard(d.player1, "Grizzly Bears")
            d.chooseMode("NCR")
            d.returnAtEnd(creature)
            (d.state.getEntity(creature)?.get<CountersComponent>()?.getCount(CounterType.FINALITY) ?: 0) shouldBe
                if (prevent) 0 else 2
            d.bolt(creature)
            (creature in d.getGraveyard(d.player1)) shouldBe prevent
            (creature in d.state.getExile(d.player1)) shouldBe !prevent
        }
    }
    test("Legion gives two counters to your target when your creature dies and has no NCR end-step trigger") {
        val d = setup()
        d.chooseMode("Legion")
        val dead = d.putCreatureOnBattlefield(d.player1, "Grizzly Bears")
        val survivor = d.putCreatureOnBattlefield(d.player1, "Centaur Courser")
        val opponent = d.putCreatureOnBattlefield(d.player2, "Grizzly Bears")
        d.bolt(dead)
        val choices = d.pendingDecision.shouldBeInstanceOf<ChooseTargetsDecision>()
        (survivor in choices.legalTargets.getValue(0)) shouldBe true
        (opponent in choices.legalTargets.getValue(0)) shouldBe false
        d.submitTargetSelection(d.player1, listOf(survivor)).error shouldBe null
        d.bothPass()
        d.state.getEntity(survivor)!!.get<CountersComponent>()!!.getCount(CounterType.PLUS_ONE_PLUS_ONE) shouldBe 2
        d.bolt(opponent)
        d.pendingDecision shouldBe null
        d.state.stack.isEmpty() shouldBe true
        d.passPriorityUntil(Step.END)
        d.pendingDecision shouldBe null
        d.state.stack.isEmpty() shouldBe true
    }
    test("without an entry choice neither anchor-word trigger is active") {
        val d = setup()
        // Like a permanent becoming a copy after entry: it has the text but no recorded mode.
        d.putPermanentOnBattlefield(d.player1, BattleOfHooverDam.name)
        val dead = d.putCreatureOnBattlefield(d.player1, "Grizzly Bears")
        d.putCreatureOnBattlefield(d.player1, "Centaur Courser")
        d.bolt(dead)
        d.pendingDecision shouldBe null
        d.state.stack.isEmpty() shouldBe true
        d.passPriorityUntil(Step.END)
        d.pendingDecision shouldBe null
        d.state.stack.isEmpty() shouldBe true
    }
})
