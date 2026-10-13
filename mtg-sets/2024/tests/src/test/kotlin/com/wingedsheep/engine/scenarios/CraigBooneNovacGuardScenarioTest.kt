package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.pip.cards.CraigBooneNovacGuard
import com.wingedsheep.mtg.sets.definitions.rav.cards.DoublingSeason
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

class CraigBooneNovacGuardScenarioTest : FunSpec({
    fun setup() = GameTestDriver().apply {
        registerCards(TestCards.all + CraigBooneNovacGuard + DoublingSeason)
        initMirrorMatch(deck = Deck.of("Plains" to 40), skipMulligans = true, startingPlayer = 0)
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }
    fun resolve(d: GameTestDriver) {
        var guard = 0
        while (d.pendingDecision == null && d.state.stack.isNotEmpty() && guard++ < 40)
            d.passPriority(d.state.priorityPlayerId!!).error shouldBe null
    }
    fun ready(d: GameTestDriver, name: String) =
        d.putCreatureOnBattlefield(d.player1, name).also(d::removeSummoningSickness)
    fun quest(d: GameTestDriver, id: EntityId) =
        d.state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.QUEST) ?: 0
    fun attackWithTwo(d: GameTestDriver): EntityId {
        val craig = ready(d, CraigBooneNovacGuard.name)
        val bears = ready(d, "Grizzly Bears")
        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        d.declareAttackers(d.player1, listOf(craig, bears), d.player2).error shouldBe null
        resolve(d)
        return craig
    }

    test("attacking with two creatures adds two quest counters, then the reflexive trigger targets") {
        val d = setup()
        val giant = d.putCreatureOnBattlefield(d.player2, "Hill Giant")
        val craig = attackWithTwo(d)
        quest(d, craig) shouldBe 2
        d.pendingDecision.shouldBeInstanceOf<ChooseTargetsDecision>()
        d.submitTargetSelection(d.player1, listOf(giant)).error shouldBe null
        resolve(d)
        val choice = d.pendingDecision.shouldBeInstanceOf<YesNoDecision>()
        choice.playerId shouldBe d.player2
        d.submitYesNo(d.player2, false).error shouldBe null
        resolve(d)
        // Two damage to the 3/3 — it survives, and nobody's life changes.
        (giant in d.state.getBattlefield()) shouldBe true
        d.getLifeTotal(d.player2) shouldBe 20
        d.getLifeTotal(d.player1) shouldBe 22
    }

    test("the creature's controller may take the damage instead; lifelink still applies") {
        val d = setup()
        val bears = d.putCreatureOnBattlefield(d.player2, "Grizzly Bears")
        attackWithTwo(d)
        d.submitTargetSelection(d.player1, listOf(bears)).error shouldBe null
        resolve(d)
        d.submitYesNo(d.player2, true).error shouldBe null
        resolve(d)
        (bears in d.state.getBattlefield()) shouldBe true
        d.getLifeTotal(d.player2) shouldBe 18
        d.getLifeTotal(d.player1) shouldBe 22
    }

    test("declining the damage sends it to the creature, counting every quest counter on Craig Boone") {
        val d = setup()
        val giant = d.putCreatureOnBattlefield(d.player2, "Hill Giant")
        val craig = ready(d, CraigBooneNovacGuard.name)
        d.addComponent(craig, CountersComponent().withAdded(CounterType.QUEST, 2))
        val bears = ready(d, "Grizzly Bears")
        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        d.declareAttackers(d.player1, listOf(craig, bears), d.player2).error shouldBe null
        resolve(d)
        quest(d, craig) shouldBe 4
        d.submitTargetSelection(d.player1, listOf(giant)).error shouldBe null
        resolve(d)
        d.submitYesNo(d.player2, false).error shouldBe null
        resolve(d)
        (giant in d.state.getZone(d.player2, Zone.GRAVEYARD)) shouldBe true
    }

    test("choosing no target deals no damage and asks nothing") {
        val d = setup()
        d.putCreatureOnBattlefield(d.player2, "Hill Giant")
        attackWithTwo(d)
        d.submitTargetSelection(d.player1, emptyList()).error shouldBe null
        resolve(d)
        d.pendingDecision.shouldBeNull()
        d.getLifeTotal(d.player2) shouldBe 20
        d.getLifeTotal(d.player1) shouldBe 20
    }

    test("with Doubling Season the four counters placed make the reflexive trigger fire twice (2024-06-07 ruling)") {
        val d = setup()
        d.putPermanentOnBattlefield(d.player1, DoublingSeason.name)
        val giant = d.putCreatureOnBattlefield(d.player2, "Hill Giant")
        val craig = attackWithTwo(d)
        quest(d, craig) shouldBe 4
        d.pendingDecision.shouldBeInstanceOf<ChooseTargetsDecision>()
        d.submitTargetSelection(d.player1, emptyList()).error shouldBe null
        resolve(d)
        d.pendingDecision.shouldBeInstanceOf<ChooseTargetsDecision>()
        d.submitTargetSelection(d.player1, listOf(giant)).error shouldBe null
        resolve(d)
        d.submitYesNo(d.player2, false).error shouldBe null
        resolve(d)
        d.pendingDecision.shouldBeNull()
        // Four damage — the quest counters on Craig Boone — kills the 3/3.
        (giant in d.state.getZone(d.player2, Zone.GRAVEYARD)) shouldBe true
    }

    test("attacking with a single creature doesn't trigger") {
        val d = setup()
        val craig = ready(d, CraigBooneNovacGuard.name)
        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        d.declareAttackers(d.player1, listOf(craig), d.player2).error shouldBe null
        d.state.stack.size shouldBe 0
        quest(d, craig) shouldBe 0
    }
})
