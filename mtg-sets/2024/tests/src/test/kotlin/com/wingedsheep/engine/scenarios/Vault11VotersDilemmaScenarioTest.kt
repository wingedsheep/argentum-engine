package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.pip.cards.Vault11VotersDilemma
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class Vault11VotersDilemmaScenarioTest : FunSpec({
    // Isolate the real chapter's effect without advancing turns through unrelated draw steps.
    val ballot = card("Vault 11 Chapter Probe") {
        manaCost = "{W}"
        typeLine = "Sorcery"
        spell { effect = Vault11VotersDilemma.sagaChapters.single { it.chapter == 2 }.effect }
    }
    val immortal = card("Vault Indestructible Probe") {
        manaCost = "{W}"
        typeLine = "Creature — Human"
        power = 1
        toughness = 1
        keywords(Keyword.INDESTRUCTIBLE)
    }
    fun driver(): GameTestDriver = GameTestDriver().apply {
        registerCards(TestCards.all + listOf(Vault11VotersDilemma, ballot, immortal))
        initMirrorMatch(Deck.of("Plains" to 40))
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }
    fun GameTestDriver.startBallot() {
        val me = activePlayer!!
        val spell = putCardInHand(me, ballot.name)
        giveMana(me, Color.WHITE, 1)
        castSpell(me, spell).error shouldBe null
        bothPass()
    }
    fun GameTestDriver.vote(choice: EntityId?) {
        val decision = state.pendingDecision!!
        submitCardSelection(decision.playerId, listOfNotNull(choice)).error shouldBe null
    }
    test("real chapters II and III ask only at resolution and the final vote sacrifices the saga") {
        val d = driver()
        val me = d.activePlayer!!
        val creature = d.putCreatureOnBattlefield(me, "Grizzly Bears")
        val saga = d.putCardInHand(me, Vault11VotersDilemma.name)
        d.giveMana(me, Color.WHITE, 3)
        d.giveMana(me, Color.BLACK, 1)
        d.castSpell(me, saga).error shouldBe null
        d.bothPass()
        d.bothPass()
        d.passPriorityUntil(Step.END)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN, me)
        d.state.pendingDecision shouldBe null
        d.state.stack.isNotEmpty() shouldBe true
        d.bothPass()
        d.state.pendingDecision!!.context.sourceId shouldBe saga
        d.vote(creature)
        d.getPermanents(me).contains(creature) shouldBe true
        d.vote(creature)
        d.getGraveyard(me).contains(creature) shouldBe true
        d.getPermanents(me).contains(saga) shouldBe true
        d.passPriorityUntil(Step.END)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN, me)
        d.state.pendingDecision shouldBe null
        d.bothPass()
        d.state.pendingDecision!!.context.sourceId shouldBe saga
        d.getPermanents(me).contains(saga) shouldBe true
        val h1 = d.getHandSize(d.player1)
        val h2 = d.getHandSize(d.player2)
        d.vote(null)
        d.vote(null)
        d.getHandSize(d.player1) shouldBe h1 + 1
        d.getHandSize(d.player2) shouldBe h2 + 1
        d.getGraveyard(me).contains(saga) shouldBe true
    }
    test("different votes destroy both tied creatures after all ballots are collected") {
        val d = driver()
        val a = d.putCreatureOnBattlefield(d.player1, "Grizzly Bears")
        val b = d.putCreatureOnBattlefield(d.player2, "Grizzly Bears")
        d.startBallot()
        d.vote(a)
        d.getPermanents(d.player1).contains(a) shouldBe true
        d.getPermanents(d.player2).contains(b) shouldBe true
        d.vote(b)
        d.getGraveyard(d.player1).contains(a) shouldBe true
        d.getGraveyard(d.player2).contains(b) shouldBe true
    }
    test("a unanimous vote destroys only the selected creature") {
        val d = driver()
        val a = d.putCreatureOnBattlefield(d.player1, "Grizzly Bears")
        val b = d.putCreatureOnBattlefield(d.player2, "Grizzly Bears")
        d.startBallot()
        d.vote(a)
        d.vote(a)
        d.getGraveyard(d.player1).contains(a) shouldBe true
        d.getPermanents(d.player2).contains(b) shouldBe true
    }
    test("a tied indestructible winner survives without protecting the other winner") {
        val d = driver()
        val a = d.putCreatureOnBattlefield(d.player1, immortal.name)
        val b = d.putCreatureOnBattlefield(d.player2, "Grizzly Bears")
        d.startBallot()
        d.vote(a)
        d.vote(b)
        d.getPermanents(d.player1).contains(a) shouldBe true
        d.getGraveyard(d.player2).contains(b) shouldBe true
    }
    test("all players abstaining draws once for each player") {
        val d = driver()
        val a = d.putCreatureOnBattlefield(d.player1, "Grizzly Bears")
        val h1 = d.getHandSize(d.player1)
        val h2 = d.getHandSize(d.player2)
        d.startBallot()
        d.vote(null)
        d.vote(null)
        d.getHandSize(d.player1) shouldBe h1 + 1
        d.getHandSize(d.player2) shouldBe h2 + 1
        d.getPermanents(d.player1).contains(a) shouldBe true
    }
    test("empty battlefield needs no ballots and each player draws") {
        val d = driver()
        val h1 = d.getHandSize(d.player1)
        val h2 = d.getHandSize(d.player2)
        d.startBallot()
        d.state.pendingDecision shouldBe null
        d.getHandSize(d.player1) shouldBe h1 + 1
        d.getHandSize(d.player2) shouldBe h2 + 1
    }
    test("chapter I creates its Human Soldier for the saga controller") {
        val d = driver()
        val me = d.activePlayer!!
        val enemy = if (me == d.player1) d.player2 else d.player1
        val saga = d.putCardInHand(me, Vault11VotersDilemma.name)
        d.giveMana(me, Color.WHITE, 3)
        d.giveMana(me, Color.BLACK, 1)
        d.castSpell(me, saga).error shouldBe null
        d.bothPass()
        d.bothPass()
        d.getPermanents(me).size shouldBe 2
        d.getPermanents(enemy).size shouldBe 0
    }
})
