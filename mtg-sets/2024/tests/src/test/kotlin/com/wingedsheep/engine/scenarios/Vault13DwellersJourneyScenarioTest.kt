package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.core.TargetsResponse
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.pip.cards.Vault13DwellersJourney
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe

/**
 * Vault 13: Dweller's Journey (PIP #26).
 *
 * Chapter I exiles up to one other enchantment or creature per player, linked to the Saga until it
 * leaves; chapter III returns two of them under their owners' control and bottoms the rest in their
 * owners' libraries. Rulings (2024-03-08): if the Saga leaves before chapter I resolves, nothing is
 * exiled.
 */
class Vault13DwellersJourneyScenarioTest : FunSpec({
    // Adds a lore counter to the Saga so chapters II and III trigger without walking turns.
    val loreProbe = card("Vault 13 Lore Probe") {
        manaCost = "{W}"
        typeLine = "Sorcery"
        spell {
            val saga = target(TargetFilter.Enchantment)
            effect = Effects.AddCounters(CounterType.LORE, 1, saga)
        }
    }
    val destroyProbe = card("Vault 13 Destroy Probe") {
        manaCost = "{W}"
        typeLine = "Instant"
        spell {
            val saga = target(TargetFilter.Enchantment)
            effect = Effects.Destroy(saga)
        }
    }
    val sagaName = Vault13DwellersJourney.name

    fun GameTestDriver.setUp() {
        registerCards(TestCards.all + listOf(Vault13DwellersJourney, loreProbe, destroyProbe))
    }

    fun GameTestDriver.resolveAll() {
        var guard = 0
        while (guard++ < 40 && state.stack.isNotEmpty() && !isPaused) {
            passPriority(state.priorityPlayerId!!)
        }
    }

    fun GameTestDriver.castSaga(me: EntityId) {
        val saga = putCardInHand(me, sagaName)
        giveMana(me, Color.WHITE, 4)
        castSpell(me, saga).error shouldBe null
        resolveAll()
    }

    fun GameTestDriver.chooseChapterOneTargets(targets: List<EntityId>): ChooseTargetsDecision {
        val decision = pendingDecision as ChooseTargetsDecision
        submitDecision(decision.playerId, TargetsResponse(decision.id, mapOf(0 to targets)))
            .error shouldBe null
        return decision
    }

    fun GameTestDriver.castProbe(me: EntityId, probe: String) {
        val spell = putCardInHand(me, probe)
        giveMana(me, Color.WHITE, 1)
        castSpell(me, spell, listOf(findPermanent(me, sagaName)!!)).error shouldBe null
    }

    fun GameTestDriver.isOnBattlefieldUnder(id: EntityId, controller: EntityId): Boolean =
        id in state.getBattlefield() && state.projectedState.getController(id) == controller

    test("three players: exile one per player, then return two and bottom the rest in its owner's library") {
        val d = GameTestDriver().apply { setUp() }
        val (me, p2, p3) = d.initMultiplayer(List(3) { Deck.of("Plains" to 40) }, startingPlayer = 0)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val myBears = d.putCreatureOnBattlefield(me, "Grizzly Bears")
        val p2Bears = d.putCreatureOnBattlefield(p2, "Grizzly Bears")
        val p3Giant = d.putCreatureOnBattlefield(p3, "Hill Giant")

        d.castSaga(me)
        val saga = d.findPermanent(me, sagaName)!!
        val decision = d.chooseChapterOneTargets(listOf(myBears, p2Bears, p3Giant))
        decision.legalTargets.getValue(0).contains(saga) shouldBe false
        d.resolveAll()
        listOf(myBears, p2Bears, p3Giant).forEach { (it in d.state.getBattlefield()) shouldBe false }

        // Chapter II: gain 2 life and scry 2.
        d.castProbe(me, loreProbe.name)
        d.resolveAll()
        while (d.isPaused) d.autoResolveDecision()
        d.resolveAll()
        d.getLifeTotal(me) shouldBe 22

        // Chapter III: choose the opponents' two cards; my Bears go to the bottom of my library.
        d.castProbe(me, loreProbe.name)
        d.resolveAll()
        val choice = d.pendingDecision as SelectCardsDecision
        choice.options shouldContainExactlyInAnyOrder listOf(myBears, p2Bears, p3Giant)
        choice.maxSelections shouldBe 2
        d.submitCardSelection(me, listOf(p2Bears, p3Giant)).error shouldBe null
        d.resolveAll()

        d.isOnBattlefieldUnder(p2Bears, p2) shouldBe true
        d.isOnBattlefieldUnder(p3Giant, p3) shouldBe true
        d.state.getZone(me, Zone.LIBRARY).last() shouldBe myBears
        (saga in d.state.getBattlefield()) shouldBe false
        d.state.getZone(me, Zone.GRAVEYARD).contains(saga) shouldBe true
    }

    test("the Saga leaving the battlefield returns the exiled cards under their owners' control") {
        val d = GameTestDriver().apply { setUp() }
        d.initMirrorMatch(Deck.of("Plains" to 40))
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val me = d.activePlayer!!
        val opp = d.getOpponent(me)
        val myBears = d.putCreatureOnBattlefield(me, "Grizzly Bears")
        val oppBears = d.putCreatureOnBattlefield(opp, "Grizzly Bears")

        d.castSaga(me)
        d.chooseChapterOneTargets(listOf(myBears, oppBears))
        d.resolveAll()
        (myBears in d.state.getBattlefield()) shouldBe false
        (oppBears in d.state.getBattlefield()) shouldBe false

        d.castProbe(me, destroyProbe.name)
        d.resolveAll()

        d.isOnBattlefieldUnder(myBears, me) shouldBe true
        d.isOnBattlefieldUnder(oppBears, opp) shouldBe true
    }

    test("if the Saga leaves before chapter I resolves, nothing is exiled") {
        val d = GameTestDriver().apply { setUp() }
        d.initMirrorMatch(Deck.of("Plains" to 40))
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val me = d.activePlayer!!
        val opp = d.getOpponent(me)
        val oppBears = d.putCreatureOnBattlefield(opp, "Grizzly Bears")

        d.castSaga(me)
        d.chooseChapterOneTargets(listOf(oppBears))
        d.castProbe(me, destroyProbe.name)
        d.resolveAll()

        d.findPermanent(me, sagaName) shouldBe null
        d.isOnBattlefieldUnder(oppBears, opp) shouldBe true
        d.state.getZone(opp, Zone.EXILE).isEmpty() shouldBe true
    }
})
