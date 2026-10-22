package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.combat.AttackingComponent
import com.wingedsheep.engine.state.components.identity.TokenComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.mid.cards.AdelineResplendentCathar
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Adeline, Resplendent Cathar (MID #1): power equal to the creatures you control; whenever you
 * attack, for each opponent, a 1/1 Human token tapped and attacking that player or a planeswalker
 * they control.
 */
class AdelineResplendentCatharScenarioTest : FunSpec({

    val walker = card("Test Adeline Walker") {
        manaCost = "{3}"
        typeLine = "Legendary Planeswalker — Test"
        startingLoyalty = 3
        oracleText = ""
    }

    fun driver(players: Int): Pair<GameTestDriver, List<EntityId>> {
        val d = GameTestDriver()
        d.registerCards(TestCards.all + listOf(AdelineResplendentCathar, walker))
        val ids = if (players == 2) {
            d.initMirrorMatch(Deck.of("Plains" to 40), skipMulligans = true, startingPlayer = 0)
            listOf(d.player1, d.player2)
        } else d.initMultiplayer(List(players) { Deck.of("Plains" to 40) })
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return d to ids
    }

    fun GameTestDriver.resolveUntilDecision() {
        var guard = 0
        while (pendingDecision == null && stackSize > 0 && guard++ < 50) passPriority(priorityPlayer!!)
    }

    fun GameTestDriver.humans(player: EntityId) =
        getCreatures(player).filter { state.getEntity(it)?.has<TokenComponent>() == true }

    fun GameTestDriver.attackedBy(id: EntityId) = state.getEntity(id)?.get<AttackingComponent>()?.defenderId

    test("a tapped attacking Human for the opponent; Adeline's power counts it; vigilance") {
        val (d, p) = driver(2)
        val (p1, p2) = p
        val adeline = d.putCreatureOnBattlefield(p1, "Adeline, Resplendent Cathar").also { d.removeSummoningSickness(it) }
        d.state.projectedState.getPower(adeline) shouldBe 1

        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        d.declareAttackers(p1, listOf(adeline), p2).error shouldBe null
        d.resolveUntilDecision()

        d.pendingDecision shouldBe null
        val tokens = d.humans(p1)
        tokens shouldHaveSize 1
        d.attackedBy(tokens.single()) shouldBe p2
        d.state.getEntity(tokens.single())!!.has<TappedComponent>() shouldBe true
        withClue("vigilance: Adeline stays untapped") { d.state.getEntity(adeline)!!.has<TappedComponent>() shouldBe false }
        d.state.projectedState.getPower(adeline) shouldBe 2

        d.passPriorityUntil(Step.END_COMBAT)
        withClue("Adeline (2) and the Human (1) connect") { d.getLifeTotal(p2) shouldBe 17 }
    }

    test("whenever you attack: one trigger per combat, not one per attacker") {
        val (d, p) = driver(2)
        val (p1, p2) = p
        val adeline = d.putCreatureOnBattlefield(p1, "Adeline, Resplendent Cathar").also { d.removeSummoningSickness(it) }
        val bears = d.putCreatureOnBattlefield(p1, "Centaur Courser").also { d.removeSummoningSickness(it) }

        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        d.declareAttackers(p1, listOf(adeline, bears), p2).error shouldBe null
        d.resolveUntilDecision()

        d.humans(p1) shouldHaveSize 1
        withClue("Adeline, Centaur Courser and the Human") { d.state.projectedState.getPower(adeline) shouldBe 3 }
    }

    test("multiplayer: one Human per opponent; an opponent's planeswalker can be chosen instead of them") {
        val (d, p) = driver(3)
        val (p1, p2, p3) = p
        val p3Walker = d.putPermanentOnBattlefield(p3, "Test Adeline Walker").also { pw ->
            d.replaceState(d.state.updateEntity(pw) { it.with(CountersComponent().withAdded(CounterType.LOYALTY, 3)) })
        }
        val adeline = d.putCreatureOnBattlefield(p1, "Adeline, Resplendent Cathar").also { d.removeSummoningSickness(it) }

        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        d.declareAttackers(p1, listOf(adeline), p2).error shouldBe null
        d.resolveUntilDecision()

        withClue("p2 controls no planeswalker: their Human attacks them without a question; p3 is asked about") {
            val decision = d.pendingDecision.shouldBeInstanceOf<ChooseTargetsDecision>()
            decision.playerId shouldBe p1
            decision.legalTargets[0]!! shouldContainExactly listOf(p3, p3Walker)
        }
        d.submitTargetSelection(p1, listOf(p3Walker)).error shouldBe null

        d.pendingDecision shouldBe null
        d.humans(p1).map { d.attackedBy(it) } shouldContainExactlyInAnyOrder listOf(p2, p3Walker)
        withClue("no token goes to an opponent") { d.humans(p2) + d.humans(p3) shouldHaveSize 0 }
    }
})
