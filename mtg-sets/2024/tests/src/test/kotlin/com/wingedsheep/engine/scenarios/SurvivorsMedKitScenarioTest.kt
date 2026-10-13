package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.state.components.battlefield.ChosenModesEverComponent
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.stack.ActivatedAbilityOnStackComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.pip.cards.SurvivorsMedKit
import com.wingedsheep.sdk.core.CounterType
import io.kotest.matchers.shouldBe

class SurvivorsMedKitScenarioTest : ScenarioTestBase() {
    init {
        fun game() = scenario().withPlayers().withCardOnBattlefield(1, "Survivor's Med Kit")
            .withLandsOnBattlefield(1, "Forest", 4)
            .withCardInLibrary(1, "Forest").withCardInLibrary(1, "Forest")
            .withCardInLibrary(2, "Forest").build()
        fun TestGame.activate(): ExecutionResult {
            val id = findPermanent("Survivor's Med Kit")!!
            return execute(ActivateAbility(player1Id, id, SurvivorsMedKit.activatedAbilities.single().id))
        }
        fun TestGame.pick(index: Int): ExecutionResult {
            val decision = getPendingDecision() as ChooseOptionDecision
            return submitDecision(OptionChosenResponse(decision.id, index))
        }
        test("Stimpak is announced before payment and draws only on resolution") {
            val g = game(); val kit = g.findPermanent("Survivor's Med Kit")!!
            g.activate().error shouldBe null
            (g.getPendingDecision() as ChooseOptionDecision).context.phase shouldBe DecisionPhase.CASTING
            g.state.getEntity(kit)!!.has<TappedComponent>() shouldBe false
            g.pick(0).error shouldBe null
            g.state.getEntity(kit)!!.has<TappedComponent>() shouldBe true
            g.state.getEntity(kit)!!.get<ChosenModesEverComponent>()!!.modeIndices shouldBe setOf(0)
            g.handSize(1) shouldBe 0
            val stack = g.state.getEntity(g.state.stack.single())!!.get<ActivatedAbilityOnStackComponent>()!!
            stack.chosenModes shouldBe listOf(0)
            g.resolveStack(); g.handSize(1) shouldBe 1
            g.state = g.state.updateEntity(kit) { it.without<TappedComponent>() }
            g.activate().error shouldBe null
            (g.getPendingDecision() as ChooseOptionDecision).options.size shouldBe 2
            g.pick(0).error shouldBe null
            g.resolveStack()
            val food = g.findPermanent("Food")!!
            g.state.getEntity(food)!!.get<com.wingedsheep.engine.state.components.identity.CardComponent>()!!.imageUri shouldBe "https://cards.scryfall.io/normal/front/e/5/e5d0801a-9565-419f-bb0b-23547c9dd59d.jpg?1783912042"
            food shouldBe g.state.getBattlefield().first { id ->
                g.state.getEntity(id)?.get<com.wingedsheep.engine.state.components.identity.CardComponent>()?.name == "Food"
            }
        }
        test("RadAway targets before tapping, removes only rad counters and sacrifices the kit") {
            val g = game(); val kit = g.findPermanent("Survivor's Med Kit")!!
            g.state = g.state.updateEntity(g.player2Id) { it.with(CountersComponent(mapOf(CounterType.RAD to 5, CounterType.ENERGY to 2))) }
            g.activate(); g.pick(2).error shouldBe null
            (g.getPendingDecision() is ChooseTargetsDecision) shouldBe true
            g.state.getEntity(kit)!!.has<TappedComponent>() shouldBe false
            g.selectTargets(listOf(g.player2Id)).error shouldBe null
            g.state.getEntity(kit)!!.has<TappedComponent>() shouldBe true
            g.state.getEntity(g.player2Id)!!.get<CountersComponent>()!!.getCount(CounterType.RAD) shouldBe 5
            g.resolveStack()
            (g.state.getEntity(g.player2Id)?.get<CountersComponent>()?.getCount(CounterType.RAD) ?: 0) shouldBe 0
            g.state.getEntity(g.player2Id)!!.get<CountersComponent>()!!.getCount(CounterType.ENERGY) shouldBe 2
            g.isInGraveyard(1, "Survivor's Med Kit") shouldBe true
        }
        test("cancelling mode or target selection pays nothing and remembers no mode") {
            for (targetPause in listOf(false, true)) {
                val g = game(); val kit = g.findPermanent("Survivor's Med Kit")!!
                g.activate(); if (targetPause) g.pick(2)
                g.submitDecision(CancelDecisionResponse(g.getPendingDecision()!!.id)).error shouldBe null
                g.state.stack.size shouldBe 0
                g.state.getEntity(kit)!!.has<TappedComponent>() shouldBe false
                g.state.getEntity(kit)!!.get<ChosenModesEverComponent>() shouldBe null
            }
        }
        test("countered Stimpak stays chosen but a new object starts fresh") {
            val g = game(); val kit = g.findPermanent("Survivor's Med Kit")!!
            g.activate(); g.pick(0)
            g.state = g.state.removeEntity(g.state.stack.single()).updateEntity(kit) { it.without<TappedComponent>() }.withPriority(g.player1Id)
            g.activate(); (g.getPendingDecision() as ChooseOptionDecision).options.size shouldBe 2
            g.submitDecision(CancelDecisionResponse(g.getPendingDecision()!!.id))
            val moved = zones.moveToZone(g.state, kit, com.wingedsheep.sdk.core.Zone.HAND)
            g.state = moved.state.withPriority(g.player1Id)
            g.castSpell(1, "Survivor's Med Kit").error shouldBe null; g.resolveStack()
            g.activate(); (g.getPendingDecision() as ChooseOptionDecision).options.size shouldBe 3
        }
        test("two kits remember their chosen modes independently") {
            val g = scenario().withPlayers().withCardOnBattlefield(1, "Survivor's Med Kit")
                .withCardOnBattlefield(1, "Survivor's Med Kit").withLandsOnBattlefield(1, "Forest", 3)
                .withCardInLibrary(1, "Forest").withCardInLibrary(2, "Forest").build()
            val kits = g.state.getBattlefield().filter { id ->
                g.state.getEntity(id)?.get<com.wingedsheep.engine.state.components.identity.CardComponent>()?.name == "Survivor's Med Kit"
            }
            g.execute(ActivateAbility(g.player1Id, kits[0], SurvivorsMedKit.activatedAbilities.single().id))
            g.pick(0); g.resolveStack()
            g.execute(ActivateAbility(g.player1Id, kits[1], SurvivorsMedKit.activatedAbilities.single().id)).error shouldBe null
            (g.getPendingDecision() as ChooseOptionDecision).options.size shouldBe 3
        }
        test("a departed RadAway source cannot sacrifice its returned object") {
            val g = game(); val kit = g.findPermanent("Survivor's Med Kit")!!
            g.state = g.state.updateEntity(g.player2Id) { it.with(CountersComponent(mapOf(CounterType.RAD to 3))) }
            g.activate(); g.pick(2); g.selectTargets(listOf(g.player2Id))
            g.state = zones.moveToZone(g.state, kit, com.wingedsheep.sdk.core.Zone.HAND).state.withPriority(g.player1Id)
            g.state = zones.moveToZone(g.state, kit, com.wingedsheep.sdk.core.Zone.BATTLEFIELD).state.withPriority(g.player1Id)
            g.resolveStack()
            g.findPermanent("Survivor's Med Kit") shouldBe kit
            (g.state.getEntity(g.player2Id)?.get<CountersComponent>()?.getCount(CounterType.RAD) ?: 0) shouldBe 0
            g.state.getEntity(kit)!!.get<ChosenModesEverComponent>() shouldBe null
        }
    }
}
