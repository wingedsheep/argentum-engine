package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.mh3.cards.ConduitGoblin
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

class ConduitGoblinScenarioTest : FunSpec({

    fun driver() = GameTestDriver().also {
        it.registerCards(TestCards.all + listOf(ConduitGoblin))
        it.initMirrorMatch(Deck.of("Mountain" to 40), skipMulligans = true, startingPlayer = 0)
        it.passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    fun GameTestDriver.energy() =
        state.getEntity(player1)?.get<CountersComponent>()?.getCount(CounterType.ENERGY) ?: 0

    fun GameTestDriver.drainEnergy() {
        replaceState(state.updateEntity(player1) { c ->
            c.with((c.get<CountersComponent>() ?: CountersComponent()).withRemoved(CounterType.ENERGY, energy()))
        })
    }

    /** Cast Conduit Goblin from hand and resolve it plus its enters trigger. */
    fun GameTestDriver.castGoblin(): EntityId {
        val spell = putCardInHand(player1, ConduitGoblin.name)
        giveMana(player1, Color.RED, 1)
        giveMana(player1, Color.WHITE, 1)
        castSpell(player1, spell).error shouldBe null
        bothPass() // resolve the creature spell
        bothPass() // resolve the enters trigger
        return findPermanent(player1, ConduitGoblin.name)!!
    }

    /** Move to beginning of combat and aim the trigger at [target]; the trigger is left on the stack. */
    fun GameTestDriver.toCombatTargeting(target: EntityId) {
        passPriorityUntil(Step.BEGIN_COMBAT)
        pendingDecision.shouldBeInstanceOf<ChooseTargetsDecision>()
        submitTargetSelection(player1, listOf(target)).error shouldBe null
    }

    test("enters: you get two energy") {
        val d = driver()
        d.castGoblin()
        d.energy() shouldBe 2
    }

    test("paying {E} gives another creature you control +1/+0 and haste until end of turn") {
        val d = driver()
        val goblin = d.castGoblin()
        val bear = d.putCreatureOnBattlefield(d.player1, "Grizzly Bears")
        d.toCombatTargeting(bear)
        d.bothPass()
        d.pendingDecision.shouldBeInstanceOf<YesNoDecision>()
        d.submitYesNo(d.player1, true).error shouldBe null
        d.energy() shouldBe 1
        val projected = d.state.projectedState
        projected.getPower(bear) shouldBe 3
        projected.getToughness(bear) shouldBe 2
        projected.hasKeyword(bear, Keyword.HASTE) shouldBe true
        projected.getPower(goblin) shouldBe 2
    }

    test("declining the payment does nothing and keeps the energy") {
        val d = driver()
        d.castGoblin()
        val bear = d.putCreatureOnBattlefield(d.player1, "Grizzly Bears")
        d.toCombatTargeting(bear)
        d.bothPass()
        d.submitYesNo(d.player1, false).error shouldBe null
        d.energy() shouldBe 2
        d.state.projectedState.getPower(bear) shouldBe 2
        d.state.projectedState.hasKeyword(bear, Keyword.HASTE) shouldBe false
    }

    test("with no energy, there is no payment prompt and no pump") {
        val d = driver()
        d.castGoblin()
        d.drainEnergy()
        val bear = d.putCreatureOnBattlefield(d.player1, "Grizzly Bears")
        d.toCombatTargeting(bear)
        d.bothPass()
        (d.pendingDecision is YesNoDecision) shouldBe false
        d.state.projectedState.getPower(bear) shouldBe 2
        d.state.projectedState.hasKeyword(bear, Keyword.HASTE) shouldBe false
    }

    test("the trigger can't target Conduit Goblin itself") {
        val d = driver()
        val goblin = d.castGoblin()
        d.putCreatureOnBattlefield(d.player1, "Grizzly Bears")
        d.passPriorityUntil(Step.BEGIN_COMBAT)
        val decision = d.pendingDecision.shouldBeInstanceOf<ChooseTargetsDecision>()
        (goblin in decision.legalTargets.getValue(0)) shouldBe false
        decision.legalTargets.getValue(0).size shouldBe 1
    }
})
