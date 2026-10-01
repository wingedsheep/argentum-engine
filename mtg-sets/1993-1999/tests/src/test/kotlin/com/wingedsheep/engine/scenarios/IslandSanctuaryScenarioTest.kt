package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain

class IslandSanctuaryScenarioTest : FunSpec({
    val flier = card("Sanctuary Test Bird") {
        typeLine = "Creature — Bird"; power = 2; toughness = 2; keywords(Keyword.FLYING)
    }
    val islandwalker = card("Sanctuary Test Walker") {
        typeLine = "Creature — Merfolk"; power = 2; toughness = 2; keywords(Keyword.ISLANDWALK)
    }
    fun driver() = GameTestDriver().apply {
        registerCards(TestCards.all + listOf(flier, islandwalker))
        initMirrorMatch(Deck.of("Forest" to 40))
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }
    fun GameTestDriver.draw(count: Int, player: EntityId, source: EntityId) {
        val result = services.effectExecutorRegistry.execute(state, Effects.DrawCards(count),
            EffectContext(sourceId = source, controllerId = player))
        result.error shouldBe null
        replaceState(result.state)
    }

    test("declining replaces nothing and grants no protection") {
        val d = driver(); val opp = d.getOpponent(d.activePlayer!!)
        d.putPermanentOnBattlefield(opp, "Island Sanctuary")
        val before = d.getHandSize(opp)
        d.passPriorityUntil(Step.DRAW)
        (d.pendingDecision as YesNoDecision).playerId shouldBe opp
        d.submitYesNo(opp, false).error shouldBe null
        d.getHandSize(opp) shouldBe before + 1
        d.state.grantedStaticAbilities shouldBe emptyList()
    }
    test("skipping the normal draw protects against ground attackers but allows flying and islandwalk without Islands") {
        val d = driver(); val me = d.activePlayer!!; val opp = d.getOpponent(me)
        d.putPermanentOnBattlefield(opp, "Island Sanctuary")
        val bear = d.putCreatureOnBattlefield(me, "Grizzly Bears").also(d::removeSummoningSickness)
        val bird = d.putCreatureOnBattlefield(me, flier.name).also(d::removeSummoningSickness)
        val walker = d.putCreatureOnBattlefield(me, islandwalker.name).also(d::removeSummoningSickness)
        val before = d.getHandSize(opp)
        d.passPriorityUntil(Step.DRAW)
        d.submitYesNo(opp, true).error shouldBe null
        d.getHandSize(opp) shouldBe before
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        d.activePlayer shouldBe me
        d.currentStep shouldBe Step.DECLARE_ATTACKERS
        d.state.grantedStaticAbilities.isNotEmpty() shouldBe true
        d.declareAttackers(me, listOf(bear), opp).error!! shouldContain "can't attack:"
        d.declareAttackers(me, listOf(bird, walker), opp).error shouldBe null
    }
    test("restriction survives Sanctuary leaving and ends at the controller's next turn") {
        val d = driver(); val me = d.activePlayer!!; val opp = d.getOpponent(me)
        val sanctuary = d.putPermanentOnBattlefield(opp, "Island Sanctuary")
        d.passPriorityUntil(Step.DRAW)
        d.submitYesNo(opp, true).error shouldBe null
        val destroyed = d.services.effectExecutorRegistry.execute(d.state,
            Effects.Destroy(EffectTarget.Self), EffectContext(sourceId = sanctuary, controllerId = opp))
        destroyed.error shouldBe null
        d.replaceState(destroyed.state)
        val bear = d.putCreatureOnBattlefield(me, "Grizzly Bears").also(d::removeSummoningSickness)
        d.putCreatureOnBattlefield(me, flier.name).also(d::removeSummoningSickness)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        d.activePlayer shouldBe me
        d.currentStep shouldBe Step.DECLARE_ATTACKERS
        d.state.grantedStaticAbilities.isNotEmpty() shouldBe true
        d.declareAttackers(me, listOf(bear), opp).error!! shouldContain "can't attack:"
        d.passPriorityUntil(Step.UPKEEP)
        d.activePlayer shouldBe opp
        d.state.grantedStaticAbilities shouldBe emptyList()
    }
    test("extra spell draws during the controller's draw step are independently optional") {
        val d = driver(); val opp = d.getOpponent(d.activePlayer!!)
        val sanctuary = d.putPermanentOnBattlefield(opp, "Island Sanctuary")
        d.passPriorityUntil(Step.DRAW)
        d.submitYesNo(opp, false).error shouldBe null
        val before = d.getHandSize(opp)
        d.draw(2, opp, sanctuary)
        d.submitYesNo(opp, true).error shouldBe null
        (d.pendingDecision as YesNoDecision).playerId shouldBe opp
        d.submitYesNo(opp, false).error shouldBe null
        d.getHandSize(opp) shouldBe before + 1
        d.state.grantedStaticAbilities.size shouldBe 1
    }
    test("draws outside your draw step do not offer Sanctuary") {
        val d = driver(); val me = d.activePlayer!!; val opp = d.getOpponent(me)
        val sanctuary = d.putPermanentOnBattlefield(opp, "Island Sanctuary")
        val before = d.getHandSize(opp)
        d.draw(1, opp, sanctuary)
        d.pendingDecision shouldBe null
        d.getHandSize(opp) shouldBe before + 1
        d.replaceState(d.state.copy(step = Step.DRAW))
        d.draw(1, opp, sanctuary)
        d.pendingDecision shouldBe null
        d.getHandSize(opp) shouldBe before + 2
        d.state.grantedStaticAbilities shouldBe emptyList()
    }
    test("replacing every draw adds no cards and duplicates do not strengthen the restriction") {
        val d = driver(); val opp = d.getOpponent(d.activePlayer!!)
        val sanctuary = d.putPermanentOnBattlefield(opp, "Island Sanctuary")
        d.passPriorityUntil(Step.DRAW)
        d.submitYesNo(opp, true).error shouldBe null
        val before = d.getHandSize(opp)
        d.draw(2, opp, sanctuary)
        repeat(2) { d.submitYesNo(opp, true).error shouldBe null }
        d.getHandSize(opp) shouldBe before
        d.state.grantedStaticAbilities.size shouldBe 3
        d.pendingDecision shouldBe null
    }
})
