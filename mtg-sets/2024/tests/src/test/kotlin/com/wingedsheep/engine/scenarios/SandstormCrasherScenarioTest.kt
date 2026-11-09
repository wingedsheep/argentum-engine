package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.battlefield.ExertedComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.combat.AttackingComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.TokenComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.j25.cards.SandstormCrasher
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Sandstorm Crasher {3}{R} — Creature — Minotaur Berserker Wizard 3/4
 *   Trample
 *   You may exert this creature as it attacks. When you do, create a tapped and attacking token
 *   that's a copy of target creature you control. Sacrifice the token at the beginning of the next
 *   end step.
 *
 * Exerting fires the linked "when you do" trigger, which may target any creature you control —
 * here one that didn't attack. The copy enters tapped and attacking the only defender and is
 * sacrificed at the end step. Declining the exert makes no trigger and no token.
 */
class SandstormCrasherScenarioTest : FunSpec({

    fun GameTestDriver.tokensOf(player: EntityId): List<EntityId> =
        getCreatures(player).filter { state.getEntity(it)?.has<TokenComponent>() == true }

    fun setup(): Triple<GameTestDriver, EntityId, EntityId> {
        val d = GameTestDriver()
        d.registerCards(TestCards.all + SandstormCrasher)
        d.initMirrorMatch(deck = Deck.of("Mountain" to 40), skipMulligans = true)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val active = d.activePlayer!!
        return Triple(d, active, d.getOpponent(active))
    }

    test("exerting copies a creature you control, tapped and attacking, sacrificed at the end step") {
        val (d, active, opp) = setup()
        val crasher = d.putCreatureOnBattlefield(active, "Sandstorm Crasher").also { d.removeSummoningSickness(it) }
        val bears = d.putCreatureOnBattlefield(active, "Grizzly Bears")

        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        d.declareAttackers(active, listOf(crasher), opp).error shouldBe null
        val exert = d.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
        exert.options shouldBe listOf(crasher)
        d.submitCardSelection(active, listOf(crasher)).error shouldBe null
        d.state.getEntity(crasher)!!.has<ExertedComponent>() shouldBe true

        val targeting = d.pendingDecision.shouldBeInstanceOf<ChooseTargetsDecision>()
        withClue("any creature you control is a legal target, attacking or not") {
            targeting.legalTargets.getValue(0) shouldContainExactlyInAnyOrder listOf(crasher, bears)
        }
        d.submitTargetSelection(active, listOf(bears)).error shouldBe null
        var guard = 0
        while (guard++ < 10 && d.stackSize > 0) d.bothPass()

        val token = d.tokensOf(active).single()
        val entity = d.state.getEntity(token)!!
        entity.get<CardComponent>()!!.name shouldBe "Grizzly Bears"
        entity.has<TappedComponent>() shouldBe true
        entity.get<AttackingComponent>()?.defenderId shouldBe opp
        withClue("the original stays home, untapped") {
            d.state.getEntity(bears)!!.has<AttackingComponent>() shouldBe false
            d.isTapped(bears) shouldBe false
        }

        d.passPriorityUntil(Step.END)
        var endGuard = 0
        while (endGuard++ < 10 && d.stackSize > 0) d.bothPass()
        d.tokensOf(active).shouldBeEmpty()
        d.getCreatures(active) shouldContainExactlyInAnyOrder listOf(crasher, bears)
    }

    test("declining the exert attacks without a trigger or token") {
        val (d, active, opp) = setup()
        val crasher = d.putCreatureOnBattlefield(active, "Sandstorm Crasher").also { d.removeSummoningSickness(it) }
        d.putCreatureOnBattlefield(active, "Grizzly Bears")

        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        d.declareAttackers(active, listOf(crasher), opp).error shouldBe null
        d.submitCardSelection(active, emptyList()).error shouldBe null

        d.state.getEntity(crasher)!!.has<AttackingComponent>() shouldBe true
        d.state.getEntity(crasher)!!.has<ExertedComponent>() shouldBe false
        d.state.stack.isEmpty() shouldBe true
        d.tokensOf(active) shouldHaveSize 0
    }
})
