package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.OrderObjectsDecision
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.combat.AttackingComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.TokenComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.blc.cards.EchoingAssault
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe

/**
 * Echoing Assault {4}{R} — Enchantment
 *   Creature tokens you control have menace.
 *   Whenever you attack a player, choose target nontoken creature that's attacking that player.
 *   Create a token that's a copy of that creature, except it's 1/1. The token enters tapped and
 *   attacking that player. Sacrifice it at the beginning of the next end step.
 *
 * Three players: attacking both opponents triggers once per player (the card's ruling), each
 * trigger may only target the creature attacking *its* player, and each 1/1 copy attacks that
 * player with menace, then is sacrificed at the end step.
 */
class EchoingAssaultScenarioTest : FunSpec({

    fun GameTestDriver.tokensOf(player: EntityId): List<EntityId> =
        getCreatures(player).filter { state.getEntity(it)?.has<TokenComponent>() == true }

    test("attacking two players makes one 1/1 menace copy attacking each, sacrificed at end step") {
        val d = GameTestDriver()
        d.registerCards(TestCards.all + EchoingAssault)
        val (p1, p2, p3) = d.initMultiplayer(List(3) { Deck.of("Mountain" to 40) })
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        d.putPermanentOnBattlefield(p1, "Echoing Assault")
        val bears = d.putCreatureOnBattlefield(p1, "Grizzly Bears").also { d.removeSummoningSickness(it) }
        val courser = d.putCreatureOnBattlefield(p1, "Centaur Courser").also { d.removeSummoningSickness(it) }

        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        d.declareAttackers(p1, mapOf(bears to p2, courser to p3)).error shouldBe null

        val offered = mutableListOf<List<EntityId>>()
        var guard = 0
        while (guard++ < 40 && d.tokensOf(p1).size < 2) {
            when (val decision = d.pendingDecision) {
                is OrderObjectsDecision -> d.submitOrderedResponse(decision.playerId, decision.objects)
                is ChooseTargetsDecision -> {
                    val legal = decision.legalTargets.getValue(0)
                    offered += legal
                    d.submitTargetSelection(decision.playerId, legal)
                }
                null -> if (d.stackSize > 0) d.bothPass() else break
                else -> error("unexpected decision $decision")
            }
        }

        withClue("each trigger offers only the nontoken creature attacking its own player") {
            offered.forEach { it shouldHaveSize 1 }
            offered.flatten().toSet() shouldBe setOf(bears, courser)
        }

        val tokens = d.tokensOf(p1)
        tokens shouldHaveSize 2
        val projected = d.state.projectedState
        val defenderByName = tokens.associate {
            d.state.getEntity(it)!!.get<CardComponent>()!!.name to
                d.state.getEntity(it)!!.get<AttackingComponent>()?.defenderId
        }
        withClue("each copy attacks the player its original attacks") {
            defenderByName shouldBe mapOf("Grizzly Bears" to p2, "Centaur Courser" to p3)
        }
        tokens.forEach {
            projected.getPower(it) shouldBe 1
            projected.getToughness(it) shouldBe 1
            d.state.getEntity(it)!!.has<TappedComponent>() shouldBe true
            withClue("creature tokens you control have menace") {
                projected.hasKeyword(it, Keyword.MENACE) shouldBe true
            }
        }
        withClue("the nontoken originals don't get menace") {
            projected.hasKeyword(bears, Keyword.MENACE) shouldBe false
        }

        d.passPriorityUntil(Step.END)
        var endGuard = 0
        while (endGuard++ < 20 && d.stackSize > 0) d.bothPass()
        d.tokensOf(p1).shouldBeEmpty()
        d.getCreatures(p1) shouldContainExactly listOf(bears, courser)
    }

    test("two-player: one trigger, offering every nontoken attacker") {
        val d = GameTestDriver()
        d.registerCards(TestCards.all + EchoingAssault)
        d.initMirrorMatch(deck = Deck.of("Mountain" to 40), skipMulligans = true)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val active = d.activePlayer!!
        val opp = d.getOpponent(active)
        d.putPermanentOnBattlefield(active, "Echoing Assault")
        val a = d.putCreatureOnBattlefield(active, "Grizzly Bears").also { d.removeSummoningSickness(it) }
        val b = d.putCreatureOnBattlefield(active, "Centaur Courser").also { d.removeSummoningSickness(it) }

        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        d.declareAttackers(active, listOf(a, b), opp).error shouldBe null
        val decision = d.pendingDecision as ChooseTargetsDecision
        withClue("one trigger for the one attacked player, offering both attackers") {
            decision.legalTargets.getValue(0).toSet() shouldBe setOf(a, b)
        }
        d.submitTargetSelection(decision.playerId, listOf(b))
        var guard = 0
        while (guard++ < 10 && d.stackSize > 0) d.bothPass()
        d.tokensOf(active) shouldHaveSize 1
    }
})
