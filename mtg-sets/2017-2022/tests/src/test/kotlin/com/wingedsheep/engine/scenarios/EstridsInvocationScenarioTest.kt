package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.handlers.effects.permanent.attachments.AttachmentMover
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.c18.cards.EstridsInvocation
import com.wingedsheep.mtg.sets.definitions.lea.cards.HolyStrength
import com.wingedsheep.mtg.sets.definitions.ons.cards.SteelyResolve
import com.wingedsheep.engine.state.components.battlefield.chosenCreatureType
import com.wingedsheep.engine.state.components.battlefield.withCastChoice
import com.wingedsheep.sdk.core.*
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

class EstridsInvocationScenarioTest : FunSpec({
    fun driver() = GameTestDriver().also {
        it.registerCards(TestCards.all + listOf(EstridsInvocation, HolyStrength, SteelyResolve))
        it.initMirrorMatch(Deck.of("Island" to 40), skipMulligans = true, startingPlayer = 0)
        it.passPriorityUntil(Step.PRECOMBAT_MAIN)
    }
    fun GameTestDriver.aura(controller: EntityId, host: EntityId): EntityId {
        val id = putPermanentOnBattlefield(controller, "Holy Strength")
        replaceState(AttachmentMover.attach(state, id, host, controller).first)
        return id
    }
    fun GameTestDriver.castInvocation(): EntityId {
        val id = putCardInHand(player1, "Estrid's Invocation")
        giveMana(player1, Color.BLUE, 3)
        castSpell(player1, id).error shouldBe null
        bothPass()
        return id
    }
    fun GameTestDriver.nextOwnUpkeep() {
        passPriorityUntil(Step.UPKEEP)
        state.activePlayerId shouldBe player2
        passPriorityUntil(Step.PRECOMBAT_MAIN)
        passPriorityUntil(Step.UPKEEP)
        state.activePlayerId shouldBe player1
    }
    test("copying an enchantment with an as-enters choice makes the Invocation's own choice") {
        val d = driver()
        val resolve = d.putPermanentOnBattlefield(d.player1, "Steely Resolve")
        d.replaceState(d.state.updateEntity(resolve) {
            it.withCastChoice(com.wingedsheep.sdk.scripting.ChoiceSlot.CREATURE_TYPE,
                com.wingedsheep.engine.state.components.battlefield.ChoiceValue.TextChoice("Goblin"))
        })
        val bear = d.putPermanentOnBattlefield(d.player2, "Grizzly Bears")
        val id = d.castInvocation()
        d.submitCardSelection(d.player1, listOf(resolve)).error shouldBe null

        (id in d.state.getBattlefield()) shouldBe false
        val choice = d.state.pendingDecision.shouldBeInstanceOf<ChooseOptionDecision>()
        d.submitDecision(d.player1, OptionChosenResponse(choice.id, choice.options.indexOf("Bear"))).error shouldBe null

        (id in d.state.getBattlefield()) shouldBe true
        d.state.getEntity(id)!!.get<CardComponent>()!!.name shouldBe "Steely Resolve"
        d.state.getEntity(id)!!.chosenCreatureType() shouldBe "Bear"
        d.state.getEntity(resolve)!!.chosenCreatureType() shouldBe "Goblin"
        d.state.projectedState.hasKeyword(bear, Keyword.SHROUD) shouldBe true
    }
    test("copies only your enchantment and chooses a different creature for the copied Aura") {
        val d = driver()
        val bear = d.putPermanentOnBattlefield(d.player1, "Grizzly Bears")
        val other = d.putPermanentOnBattlefield(d.player2, "Grizzly Bears")
        val mine = d.aura(d.player1, bear)
        val theirs = d.aura(d.player2, other)
        val id = d.castInvocation()
        val choice = d.state.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
        choice.options shouldBe listOf(mine)
        (theirs in choice.options) shouldBe false
        d.submitCardSelection(d.player1, listOf(mine)).error shouldBe null
        d.submitTargetSelection(d.player1, listOf(other)).error shouldBe null
        d.state.getEntity(id)!!.get<AttachedToComponent>()!!.targetId shouldBe other
        d.state.projectedState.getPower(other) shouldBe 4
        d.state.getEntity(id)!!.get<CardComponent>()!!.copyTriggeredAbilities.size shouldBe 1
    }
    test("upkeep blink asks for a fresh copy and Aura host then enters under its owner control") {
        val d = driver()
        val firstHost = d.putPermanentOnBattlefield(d.player1, "Grizzly Bears")
        val secondHost = d.putPermanentOnBattlefield(d.player2, "Grizzly Bears")
        val source = d.aura(d.player1, firstHost)
        val id = d.castInvocation()
        d.submitCardSelection(d.player1, listOf(source)).error shouldBe null
        d.submitTargetSelection(d.player1, listOf(firstHost)).error shouldBe null
        d.nextOwnUpkeep()
        d.bothPass()
        d.state.pendingDecision.shouldBeInstanceOf<YesNoDecision>()
        d.submitYesNo(d.player1, true).error shouldBe null
        (id in d.state.getExile(d.player1)) shouldBe true
        d.state.getEntity(id)!!.get<CardComponent>()!!.name shouldBe "Estrid's Invocation"
        d.state.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
        d.submitCardSelection(d.player1, listOf(source)).error shouldBe null
        d.state.pendingDecision.shouldBeInstanceOf<ChooseTargetsDecision>()
        d.submitTargetSelection(d.player1, listOf(secondHost)).error shouldBe null
        d.state.getEntity(id)!!.get<AttachedToComponent>()!!.targetId shouldBe secondHost
        d.state.projectedState.getController(id) shouldBe d.player1
        d.state.getEntity(id)!!.get<CardComponent>()!!.copyTriggeredAbilities.size shouldBe 1
    }
    test("upkeep blink copying an enchantment with an as-enters choice asks that choice again") {
        val d = driver()
        val resolve = d.putPermanentOnBattlefield(d.player1, "Steely Resolve")
        val bear = d.putPermanentOnBattlefield(d.player2, "Grizzly Bears")
        val id = d.castInvocation()
        d.submitCardSelection(d.player1, listOf(resolve)).error shouldBe null
        val first = d.state.pendingDecision.shouldBeInstanceOf<ChooseOptionDecision>()
        d.submitDecision(d.player1, OptionChosenResponse(first.id, first.options.indexOf("Goblin"))).error shouldBe null
        d.state.getEntity(id)!!.chosenCreatureType() shouldBe "Goblin"

        d.nextOwnUpkeep()
        d.bothPass()
        d.submitYesNo(d.player1, true).error shouldBe null
        d.submitCardSelection(d.player1, listOf(resolve)).error shouldBe null
        (id in d.state.getBattlefield()) shouldBe false
        val again = d.state.pendingDecision.shouldBeInstanceOf<ChooseOptionDecision>()
        d.submitDecision(d.player1, OptionChosenResponse(again.id, again.options.indexOf("Bear"))).error shouldBe null

        (id in d.state.getBattlefield()) shouldBe true
        d.state.getEntity(id)!!.get<CardComponent>()!!.name shouldBe "Steely Resolve"
        d.state.getEntity(id)!!.chosenCreatureType() shouldBe "Bear"
        d.state.projectedState.hasKeyword(bear, Keyword.SHROUD) shouldBe true
    }
    test("declining upkeep exile keeps the copied Aura attached") {
        val d = driver()
        val host = d.putPermanentOnBattlefield(d.player1, "Grizzly Bears")
        val source = d.aura(d.player1, host)
        val id = d.castInvocation()
        d.submitCardSelection(d.player1, listOf(source)).error shouldBe null
        d.submitTargetSelection(d.player1, listOf(host)).error shouldBe null
        d.nextOwnUpkeep()
        d.bothPass()
        d.submitYesNo(d.player1, false).error shouldBe null
        d.state.getEntity(id)!!.get<AttachedToComponent>()!!.targetId shouldBe host
        d.state.getEntity(id)!!.get<CardComponent>()!!.name shouldBe "Holy Strength"
    }
    test("declining copying gives no upkeep trigger") {
        val d = driver()
        val host = d.putPermanentOnBattlefield(d.player1, "Grizzly Bears")
        d.aura(d.player1, host)
        val id = d.castInvocation()
        d.submitCardSelection(d.player1, emptyList()).error shouldBe null
        (id in d.state.getBattlefield()) shouldBe true
        d.state.getEntity(id)!!.get<CardComponent>()!!.copyTriggeredAbilities shouldBe emptyList()
        d.nextOwnUpkeep()
        d.state.stack shouldBe emptyList()
    }
    test("a departed Invocation cannot blink a later zone instance from its old upkeep trigger") {
        val d = driver()
        val host = d.putPermanentOnBattlefield(d.player1, "Grizzly Bears")
        val source = d.aura(d.player1, host)
        val id = d.castInvocation()
        d.submitCardSelection(d.player1, listOf(source)).error shouldBe null
        d.submitTargetSelection(d.player1, listOf(host)).error shouldBe null
        d.nextOwnUpkeep()
        d.replaceState(d.zones.moveToZone(d.state, id, Zone.HAND).state)
        d.bothPass()
        if (d.state.pendingDecision is YesNoDecision) d.submitYesNo(d.player1, true).error shouldBe null
        (id in d.state.getHand(d.player1)) shouldBe true
        d.state.getEntity(id)!!.get<CardComponent>()!!.name shouldBe "Estrid's Invocation"
    }
})
