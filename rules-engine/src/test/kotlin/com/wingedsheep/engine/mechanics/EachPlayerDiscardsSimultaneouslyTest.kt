package com.wingedsheep.engine.mechanics

import com.wingedsheep.engine.core.CardsDiscardedEvent
import com.wingedsheep.engine.core.CardsSelectedResponse
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe

/**
 * "Each player discards N cards" ([Effects.EachPlayerDiscards]) follows the APNAP rule for
 * simultaneous choices (CR 101.4): the active player chooses first, then each nonactive player in
 * turn order, and only then are all the chosen cards discarded at the same time. Cards chosen
 * from a hand may stay face down while they are chosen (CR 101.4a), so a later chooser never sees
 * what an earlier player is discarding (Liliana of the Veil / Liliana, Defiant Necromancer
 * rulings, 2015-06-22).
 */
class EachPlayerDiscardsSimultaneouslyTest : FunSpec({

    val everyoneDiscards = card("Simultaneous Probe Everyone Discards") {
        manaCost = "{0}"
        typeLine = "Sorcery"
        oracleText = "Each player discards a card."
        spell { effect = Effects.EachPlayerDiscards(1) }
    }
    val everyoneDiscardsTwo = card("Simultaneous Probe Everyone Discards Two") {
        manaCost = "{0}"
        typeLine = "Sorcery"
        oracleText = "Each player discards two cards."
        spell { effect = Effects.EachPlayerDiscards(2) }
    }
    val discardWatcher = card("Simultaneous Probe Discard Watcher") {
        manaCost = "{0}"
        typeLine = "Enchantment"
        oracleText = "Whenever a player discards one or more cards, you gain 1 life."
        triggeredAbility {
            trigger = Triggers.anyPlayer.discards(batch = true)
            effect = Effects.GainLife(1)
        }
    }

    fun twoPlayerMain(): GameTestDriver = GameTestDriver().also {
        it.registerCards(TestCards.all + listOf(everyoneDiscards, everyoneDiscardsTwo, discardWatcher))
        it.initMirrorMatch(deck = Deck.of("Forest" to 40), startingPlayer = 0)
        it.passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    fun GameTestDriver.castAndResolve(name: String) {
        val active = activePlayer!!
        castSpell(active, putCardInHand(active, name)).error shouldBe null
        bothPass()
    }

    fun GameTestDriver.pendingSelection(): SelectCardsDecision =
        pendingDecision as? SelectCardsDecision ?: error("expected a discard choice, got $pendingDecision")

    test("the active player chooses first, and nothing is discarded until the last player has chosen") {
        val d = twoPlayerMain()
        val active = d.activePlayer!!
        val opponent = d.getOpponent(active)

        d.castAndResolve(everyoneDiscards.name)

        val first = d.pendingSelection()
        first.playerId shouldBe active
        val activeChoice = first.options.first()
        d.submitDecision(active, CardsSelectedResponse(first.id, listOf(activeChoice))).error shouldBe null

        val second = d.pendingSelection()
        withClue("the nonactive player chooses next") { second.playerId shouldBe opponent }
        withClue("while they choose, the active player's card hasn't been discarded (CR 101.4a)") {
            d.getHand(active) shouldContain activeChoice
            d.getGraveyard(active) shouldNotContain activeChoice
        }
        withClue("the nonactive player only ever chooses from their own hand") {
            second.options.forEach { d.getHand(opponent) shouldContain it }
        }

        val opponentChoice = second.options.first()
        val result = d.submitDecision(opponent, CardsSelectedResponse(second.id, listOf(opponentChoice)))
        result.error shouldBe null

        d.getGraveyard(active) shouldContain activeChoice
        d.getGraveyard(opponent) shouldContain opponentChoice
        d.getHand(active) shouldNotContain activeChoice
        d.getHand(opponent) shouldNotContain opponentChoice

        withClue("both discards happen in the same move, one discard event per player (CR 101.4)") {
            val discards = result.events.filterIsInstance<CardsDiscardedEvent>()
            discards.map { it.playerId } shouldContainExactlyInAnyOrder listOf(active, opponent)
            discards.first { it.playerId == active }.cardIds shouldBe listOf(activeChoice)
            discards.first { it.playerId == opponent }.cardIds shouldBe listOf(opponentChoice)
        }
    }

    test("each player discards N of their own cards into their own graveyard") {
        val d = twoPlayerMain()
        val active = d.activePlayer!!
        val opponent = d.getOpponent(active)

        d.castAndResolve(everyoneDiscardsTwo.name)

        val chosen = mutableMapOf<EntityId, List<EntityId>>()
        while (true) {
            val decision = d.pendingDecision as? SelectCardsDecision ?: break
            decision.minSelections shouldBe 2
            val pick = decision.options.take(2)
            chosen[decision.playerId] = pick
            d.submitDecision(decision.playerId, CardsSelectedResponse(decision.id, pick)).error shouldBe null
        }

        chosen.keys shouldBe setOf(active, opponent)
        withClue("the active player's graveyard also holds the resolved sorcery") {
            d.getGraveyard(active).filterNot { it in chosen.getValue(active) }
                .map { d.state.getEntity(it)?.get<CardComponent>()?.name } shouldBe listOf(everyoneDiscardsTwo.name)
        }
        d.getGraveyard(opponent).shouldContainExactlyInAnyOrder(chosen.getValue(opponent))
    }

    test("a player with an empty hand isn't asked, and the others still discard") {
        val d = twoPlayerMain()
        val active = d.activePlayer!!
        val opponent = d.getOpponent(active)
        d.getHand(opponent).forEach { d.moveToGraveyard(it) }
        val opponentGraveyard = d.getGraveyard(opponent).toList()

        d.castAndResolve(everyoneDiscards.name)

        val only = d.pendingSelection()
        only.playerId shouldBe active
        val pick = only.options.first()
        d.submitDecision(active, CardsSelectedResponse(only.id, listOf(pick))).error shouldBe null

        d.pendingDecision shouldBe null
        d.getGraveyard(active) shouldContain pick
        d.getGraveyard(opponent) shouldBe opponentGraveyard
    }

    test("discard triggers see every player's discard") {
        val d = twoPlayerMain()
        val active = d.activePlayer!!
        d.putPermanentOnBattlefield(active, discardWatcher.name)
        val lifeBefore = d.getLifeTotal(active)

        d.castAndResolve(everyoneDiscards.name)
        while (true) {
            val decision = d.pendingDecision as? SelectCardsDecision ?: break
            d.submitDecision(decision.playerId, CardsSelectedResponse(decision.id, decision.options.take(1)))
                .error shouldBe null
        }
        d.bothPass()
        d.bothPass()

        withClue("one batched trigger per discarding player") {
            d.getLifeTotal(active) shouldBe lifeBefore + 2
        }
    }
})
