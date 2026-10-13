package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CardsRevealedEvent
import com.wingedsheep.engine.view.ClientEvent
import com.wingedsheep.engine.view.ClientEventTransformer
import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.ChooseNumberDecision
import com.wingedsheep.engine.core.NumberChosenResponse
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.state.components.battlefield.LinkedExileComponent
import com.wingedsheep.engine.state.components.identity.FaceDownComponent
import com.wingedsheep.engine.state.components.identity.MayLookAtInExileComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.pip.cards.ExpertLevelSafe
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class ExpertLevelSafeScenarioTest : FunSpec({
    val removal = card("Safe Removal Probe") {
        manaCost = "{W}"
        typeLine = "Instant"
        spell { effect = Effects.Destroy(target(TargetFilter.Artifact)) }
    }
    fun driver(): GameTestDriver = GameTestDriver().apply {
        registerCards(TestCards.all + listOf(ExpertLevelSafe, removal))
        initMirrorMatch(Deck.of("Plains" to 40))
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }
    fun GameTestDriver.castSafe(): EntityId {
        val me = activePlayer!!
        val safe = putCardInHand(me, ExpertLevelSafe.name)
        giveMana(me, Color.WHITE, 2)
        castSpell(me, safe).error shouldBe null
        bothPass()
        bothPass()
        return safe
    }
    fun GameTestDriver.activate(safe: EntityId, resolve: Boolean = true) {
        val me = activePlayer!!
        val opponent = if (me == player1) player2 else player1
        giveMana(me, Color.WHITE, 1)
        submit(ActivateAbility(
            playerId = me, sourceId = safe, abilityId = ExpertLevelSafe.activatedAbilities.single().id,
            targets = listOf(ChosenTarget.Player(opponent)), paymentStrategy = PaymentStrategy.FromPool,
        )).error shouldBe null
        if (resolve) bothPass()
    }
    fun GameTestDriver.choose(value: Int) {
        val decision = state.pendingDecision as ChooseNumberDecision
        submitDecision(decision.playerId, NumberChosenResponse(decision.id, value)).error shouldBe null
    }
    test("entering exiles two linked cards face down without granting permission to look") {
        val d = driver()
        val safe = d.castSafe()
        val linked = d.state.getEntity(safe)!!.get<LinkedExileComponent>()!!.exiledIds
        linked.size shouldBe 2
        linked.forEach {
            d.state.getEntity(it)!!.has<FaceDownComponent>() shouldBe true
            d.state.getEntity(it)!!.has<MayLookAtInExileComponent>() shouldBe false
        }
    }
    test("matching numbers sacrifice Safe and return its whole linked pile") {
        val d = driver()
        val me = d.activePlayer!!
        val safe = d.castSafe()
        val linked = d.state.getEntity(safe)!!.get<LinkedExileComponent>()!!.exiledIds
        val hand = d.getHandSize(me)
        d.activate(safe)
        d.choose(2)
        d.getPermanents(me).contains(safe) shouldBe true
        val beforeReturn = d.events.size
        d.choose(2)
        val returnEvents = d.events.drop(beforeReturn)
        returnEvents.filterIsInstance<CardsRevealedEvent>() shouldBe emptyList()
        val opponent = if (me == d.player1) d.player2 else d.player1
        val opponentEvents = ClientEventTransformer.transform(returnEvents, opponent, d.state)
        opponentEvents.filterIsInstance<ClientEvent.CardsRevealed>() shouldBe emptyList()
        val hiddenReturns = opponentEvents.filterIsInstance<ClientEvent.PermanentLeft>()
            .filter { it.destination == "hand" }
        hiddenReturns.size shouldBe 2
        hiddenReturns.forEach {
            it.cardName shouldBe "card"
            it.cardId shouldBe null
        }
        val ownerReturns = ClientEventTransformer.transform(returnEvents, me, d.state)
            .filterIsInstance<ClientEvent.PermanentLeft>().filter { it.destination == "hand" }
        ownerReturns.map { it.cardName } shouldBe listOf("Plains", "Plains")
        d.getGraveyard(me).contains(safe) shouldBe true
        d.getHandSize(me) shouldBe hand + 2
        linked.forEach { d.getHand(me).contains(it) shouldBe true }
    }
    test("different numbers exile one more card using the original controller context") {
        val d = driver()
        val me = d.activePlayer!!
        val opponent = if (me == d.player1) d.player2 else d.player1
        val safe = d.castSafe()
        d.activate(safe)
        d.choose(1)
        d.choose(3)
        d.getExile(me).size shouldBe 3
        d.getExile(opponent).size shouldBe 0
        d.state.getEntity(safe)!!.get<LinkedExileComponent>()!!.exiledIds.size shouldBe 3
        d.getPermanents(me).contains(safe) shouldBe true
    }
    test("matching numbers still return linked cards after Safe leaves before resolution") {
        val d = driver()
        val me = d.activePlayer!!
        val opponent = if (me == d.player1) d.player2 else d.player1
        val safe = d.castSafe()
        val pile = d.state.getEntity(safe)!!.get<LinkedExileComponent>()!!.exiledIds
        d.activate(safe, resolve = false)
        d.passPriority(me)
        val destroy = d.putCardInHand(opponent, removal.name)
        d.giveMana(opponent, Color.WHITE, 1)
        d.castSpell(opponent, destroy, listOf(safe)).error shouldBe null
        d.bothPass()
        d.getGraveyard(me).contains(safe) shouldBe true
        d.bothPass()
        d.choose(1)
        d.choose(1)
        pile.forEach { d.getHand(me).contains(it) shouldBe true }
    }
    test("matching numbers return cards to their distinct owners") {
        val d = driver()
        val me = d.activePlayer!!
        val opponent = if (me == d.player1) d.player2 else d.player1
        val safe = d.putPermanentOnBattlefield(me, ExpertLevelSafe.name)
        val mine = d.putCardInExile(me, "Plains")
        val theirs = d.putCardInExile(opponent, "Plains")
        d.replaceState(d.state.updateEntity(safe) { it.with(LinkedExileComponent(listOf(mine, theirs))) })
        d.activate(safe)
        d.choose(3)
        val beforeReturn = d.events.size
        d.choose(3)
        d.events.drop(beforeReturn).filterIsInstance<CardsRevealedEvent>().size shouldBe 2
        d.getHand(me).contains(mine) shouldBe true
        d.getHand(opponent).contains(theirs) shouldBe true
        d.getHand(me).contains(theirs) shouldBe false
    }
})
