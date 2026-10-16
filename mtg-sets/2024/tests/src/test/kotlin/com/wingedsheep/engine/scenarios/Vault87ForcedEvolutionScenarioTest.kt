package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.TargetsResponse
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.pip.cards.Vault87ForcedEvolution
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Vault 87: Forced Evolution (PIP #122).
 *
 * Chapter I steals a non-Mutant creature for as long as you control the Saga; chapter II grows a
 * creature you control and makes it a Mutant; chapter III draws cards equal to the greatest power
 * among your Mutants. Sacrificing the Saga after III ends the steal.
 */
class Vault87ForcedEvolutionScenarioTest : FunSpec({
    // Adds a lore counter to the Saga so chapters II and III trigger without walking turns.
    val loreProbe = card("Vault 87 Lore Probe") {
        manaCost = "{G}"
        typeLine = "Sorcery"
        spell {
            val saga = target(TargetFilter.Enchantment)
            effect = Effects.AddCounters(CounterType.LORE, 1, saga)
        }
    }
    val mutant = card("Vault 87 Test Mutant") {
        manaCost = "{G}"
        typeLine = "Creature — Mutant"
        power = 4
        toughness = 4
    }
    val destroyProbe = card("Vault 87 Destroy Probe") {
        manaCost = "{G}"
        typeLine = "Instant"
        spell {
            val saga = target(TargetFilter.Enchantment)
            effect = Effects.Destroy(saga)
        }
    }
    val sagaName = Vault87ForcedEvolution.name

    fun GameTestDriver.setUp() {
        registerCards(TestCards.all + listOf(Vault87ForcedEvolution, loreProbe, destroyProbe, mutant))
        initMirrorMatch(Deck.of("Forest" to 40))
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    fun GameTestDriver.resolveAll() {
        var guard = 0
        while (guard++ < 40 && state.stack.isNotEmpty() && !isPaused) {
            passPriority(state.priorityPlayerId!!)
        }
    }

    fun GameTestDriver.chooseTarget(target: EntityId): ChooseTargetsDecision {
        val decision = pendingDecision as ChooseTargetsDecision
        submitDecision(decision.playerId, TargetsResponse(decision.id, mapOf(0 to listOf(target))))
            .error shouldBe null
        return decision
    }

    fun GameTestDriver.castSaga(me: EntityId) {
        val saga = putCardInHand(me, sagaName)
        giveMana(me, Color.GREEN, 4)
        giveMana(me, Color.BLUE, 1)
        castSpell(me, saga).error shouldBe null
        resolveAll()
    }

    fun GameTestDriver.castLoreProbe(me: EntityId) {
        val spell = putCardInHand(me, loreProbe.name)
        giveMana(me, Color.GREEN, 1)
        castSpell(me, spell, listOf(findPermanent(me, sagaName)!!)).error shouldBe null
        resolveAll()
    }

    test("steal a non-Mutant, mutate it, draw for its power, and lose it when the Saga is sacrificed") {
        val d = GameTestDriver().apply { setUp() }
        val me = d.player1
        val opp = d.player2
        val oppBears = d.putCreatureOnBattlefield(opp, "Grizzly Bears")
        val oppMutant = d.putCreatureOnBattlefield(opp, mutant.name)

        // Chapter I: the opponent's Mutant isn't a legal target.
        d.castSaga(me)
        val decision = d.chooseTarget(oppBears)
        decision.legalTargets.getValue(0).contains(oppMutant) shouldBe false
        decision.legalTargets.getValue(0).contains(oppBears) shouldBe true
        d.resolveAll()
        d.state.projectedState.getController(oppBears) shouldBe me

        // Chapter II: +1/+1 counter, and the stolen Bears become a Mutant. Control doesn't end.
        d.castLoreProbe(me)
        d.chooseTarget(oppBears)
        d.resolveAll()
        d.state.getEntity(oppBears)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) shouldBe 1
        d.state.projectedState.hasSubtype(oppBears, "Mutant") shouldBe true
        d.state.projectedState.getPower(oppBears) shouldBe 3
        d.state.projectedState.getController(oppBears) shouldBe me

        // Chapter III: draw 3 (the opponent's 4-power Mutant isn't yours), then the Saga is
        // sacrificed and the Bears go home.
        val probe = d.putCardInHand(me, loreProbe.name)
        d.giveMana(me, Color.GREEN, 1)
        d.castSpell(me, probe, listOf(d.findPermanent(me, sagaName)!!)).error shouldBe null
        val handBefore = d.getHandSize(me)
        d.resolveAll()
        d.getHandSize(me) shouldBe handBefore + 3
        d.findPermanent(me, sagaName) shouldBe null
        d.state.projectedState.getController(oppBears) shouldBe opp
    }

    test("if the Saga leaves before chapter I resolves, no control is gained") {
        val d = GameTestDriver().apply { setUp() }
        val me = d.player1
        val opp = d.player2
        val oppBears = d.putCreatureOnBattlefield(opp, "Grizzly Bears")

        d.castSaga(me)
        d.chooseTarget(oppBears)
        // Chapter I is on the stack; remove the Saga before it resolves.
        val probe = d.putCardInHand(me, destroyProbe.name)
        d.giveMana(me, Color.GREEN, 1)
        d.castSpell(me, probe, listOf(d.findPermanent(me, sagaName)!!)).error shouldBe null
        d.resolveAll()

        d.findPermanent(me, sagaName) shouldBe null
        d.state.projectedState.getController(oppBears) shouldBe opp
    }
})
