package com.wingedsheep.ai.engine

import com.wingedsheep.ai.engine.knowledge.GraveyardSynergy
import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.TargetsResponse
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.tsp.cards.ThinkTwice
import com.wingedsheep.mtg.sets.definitions.vow.cards.DiregrafScavenger
import com.wingedsheep.mtg.sets.definitions.vow.cards.ScreamingSwarm
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Who a "target player mills N" trigger and an "exile target card from a graveyard" trigger aim at.
 *
 * The board evaluator has no library or graveyard term, so both answers simulate to the same score
 * and the pick used to fall to the first legal target — the AI's own seat. The 2026-10-12
 * VOW-vs-LRW engine log: Screaming Swarm milled its own controller ten cards over four attacks, and
 * Diregraf Scavenger exiled its controller's own Mindleech Ghoul. Self-mill is a deliberate choice
 * now — taken when the deck feeds on its graveyard ([GraveyardSynergy]), not by list order.
 */
class MillAndGraveyardTargetChoiceTest : FunSpec({

    val cards = TestCards.all + listOf(ScreamingSwarm, ThinkTwice, DiregrafScavenger)
    val registry = CardRegistry().apply { register(cards) }

    fun newGame(): GameTestDriver = GameTestDriver().apply {
        registerCards(cards)
        initMirrorMatch(deck = Deck.of("Island" to 20, "Swamp" to 20), skipMulligans = true, startingPlayer = 0)
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    fun awaitTargets(d: GameTestDriver): ChooseTargetsDecision {
        repeat(20) {
            val pending = d.state.pendingDecision
            if (pending is ChooseTargetsDecision && pending.playerId == d.player1) return pending
            if (pending != null) d.autoResolveDecision() else d.bothPass()
        }
        error("the target decision never arrived")
    }

    /** Attack with Screaming Swarm and stop at its mill trigger's target decision. */
    fun swarmTrigger(setup: GameTestDriver.() -> Unit = {}): Pair<GameTestDriver, ChooseTargetsDecision> {
        val d = newGame()
        d.setup()
        val swarm = d.putCreatureOnBattlefield(d.player1, "Screaming Swarm")
        d.removeSummoningSickness(swarm)
        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        d.declareAttackers(d.player1, listOf(swarm), d.player2)
        return d to awaitTargets(d)
    }

    fun answer(d: GameTestDriver, decision: ChooseTargetsDecision): List<EntityId> =
        AIPlayer.create(registry, d.player1).respondToDecision(d.state, decision)
            .shouldBeInstanceOf<TargetsResponse>().selectedTargets.getValue(0)

    test("with no graveyard payoffs, the mill trigger targets the opponent") {
        val (d, decision) = swarmTrigger()
        decision.legalTargets.getValue(0).toSet() shouldBe setOf(d.player1, d.player2)
        GraveyardSynergy.benefitsFromGraveyard(d.state, d.player1, registry) shouldBe false
        answer(d, decision) shouldBe listOf(d.player2)
    }

    test("a deck full of flashback mills itself") {
        val (d, decision) = swarmTrigger { repeat(5) { putCardInHand(player1, "Think Twice") } }
        GraveyardSynergy.benefitsFromGraveyard(d.state, d.player1, registry) shouldBe true
        answer(d, decision) shouldBe listOf(d.player1)
    }

    test("a graveyard deck still mills an opponent who is about to deck") {
        val (d, decision) = swarmTrigger {
            repeat(5) { putCardInHand(player1, "Think Twice") }
            // Leave the opponent one card: milling them is the win.
            val library = state.getZone(player2, Zone.LIBRARY)
            library.drop(1).forEach { moveToGraveyard(it) }
        }
        answer(d, decision) shouldBe listOf(d.player2)
    }

    test("Diregraf Scavenger exiles the opponent's creature card, not its controller's") {
        val d = newGame()
        val own = d.putCardInGraveyard(d.player1, "Grizzly Bears")
        val theirs = d.putCardInGraveyard(d.player2, "Grizzly Bears")
        repeat(4) { d.putLandOnBattlefield(d.player1, "Swamp") }
        val scavenger = d.putCardInHand(d.player1, "Diregraf Scavenger")
        d.castSpell(d.player1, scavenger)
        val decision = awaitTargets(d)
        decision.legalTargets.getValue(0).toSet().containsAll(listOf(own, theirs)) shouldBe true
        answer(d, decision) shouldBe listOf(theirs)
    }
})
