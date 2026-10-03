package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.lea.cards.CyclopeanTomb
import com.wingedsheep.sdk.core.*
import com.wingedsheep.sdk.dsl.*
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class CyclopeanTombScenarioTest : FunSpec({
    val mire = CounterType.of("mire")
    val destroy = card("Tomb Test Destroy") { manaCost = "{0}"; typeLine = "Instant"
        spell { val t = target(TargetFilter.Permanent); effect = Effects.Destroy(t) } }
    val remove = card("Tomb Test Remove") { manaCost = "{0}"; typeLine = "Instant"
        spell { val t = target(TargetFilter.Permanent); effect = Effects.RemoveAllCountersOfType(mire, t) } }
    val exile = card("Tomb Test Exile") { manaCost = "{0}"; typeLine = "Instant"
        spell { val t = target(TargetFilter.Permanent); effect = Effects.Exile(t) } }
    val bounce = card("Tomb Test Bounce") { manaCost = "{0}"; typeLine = "Instant"
        spell { val t = target(TargetFilter.Permanent); effect = Effects.ReturnToHand(t) } }
    val steal = card("Tomb Test Steal") { manaCost = "{0}"; typeLine = "Instant"
        spell { val t = target(TargetFilter.Permanent); effect = Effects.GainControl(t) } }
    val add = card("Tomb Test Add") { manaCost = "{0}"; typeLine = "Instant"
        spell { val t = target(TargetFilter.Permanent); effect = Effects.AddCounters(mire, 2, t) } }
    val noCounters = card("Tomb Test No Counters") { typeLine = "Enchantment"
        staticAbility { ability = com.wingedsheep.sdk.scripting.CantReceiveCounters(
            com.wingedsheep.sdk.scripting.filters.unified.GroupFilter(com.wingedsheep.sdk.scripting.GameObjectFilter.Land)) } }
    fun driver() = GameTestDriver().apply {
        registerCards(TestCards.all + listOf(destroy, remove, exile, bounce, steal, add, noCounters))
        initMirrorMatch(Deck.of("Forest" to 40))
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }
    fun GameTestDriver.myUpkeep() {
        val me = activePlayer!!
        passPriorityUntil(Step.PRECOMBAT_MAIN)
        passPriorityUntil(Step.UPKEEP)
        passPriorityUntil(Step.PRECOMBAT_MAIN)
        passPriorityUntil(Step.UPKEEP)
        activePlayer shouldBe me
    }
    fun GameTestDriver.mark(me: EntityId, tomb: EntityId, land: EntityId) {
        giveColorlessMana(me, 2)
        submit(ActivateAbility(me, tomb, CyclopeanTomb.activatedAbilities.single().id,
            targets = listOf(ChosenTarget.Permanent(land)))).error shouldBe null
        bothPass()
        state.getEntity(land)!!.get<CountersComponent>()!!.getCount(mire) shouldBe 1
        state.projectedState.getSubtypes(land) shouldBe setOf("Swamp")
    }
    fun GameTestDriver.spell(me: EntityId, name: String, target: EntityId) {
        if (state.priorityPlayerId != me) passPriority(state.priorityPlayerId!!).error shouldBe null
        castSpell(me, putCardInHand(me, name), listOf(target)).error shouldBe null
        bothPass()
    }
    fun GameTestDriver.kill(me: EntityId, tomb: EntityId) {
        spell(me, destroy.name, tomb)
        bothPass() // resolve the dies trigger that installs the recurring cleanup
        state.delayedTriggers.size shouldBe 1
    }
    fun GameTestDriver.cleanup(me: EntityId, land: EntityId? = null) {
        bothPass()
        if (pendingDecision != null) {
            val decision = pendingDecision as SelectCardsDecision
            decision.useTargetingUI shouldBe true
            submitCardSelection(me, listOf(land ?: decision.options.first())).error shouldBe null
        }
    }

    test("activation is restricted to its controller's upkeep and rejects Swamps") {
        val d = driver(); val me = d.activePlayer!!
        val tomb = d.putPermanentOnBattlefield(me, "Cyclopean Tomb")
        val forest = d.putPermanentOnBattlefield(me, "Forest")
        val swamp = d.putPermanentOnBattlefield(me, "Swamp")
        d.giveColorlessMana(me, 10)
        fun action(land: EntityId) = ActivateAbility(me, tomb, CyclopeanTomb.activatedAbilities.single().id,
            targets = listOf(ChosenTarget.Permanent(land)))
        d.submitExpectFailure(action(forest))
        d.myUpkeep()
        d.submitExpectFailure(action(swamp))
        d.mark(me, tomb, forest)
    }
    test("Swamp effect persists after exile but no cleanup is installed") {
        val d = driver(); val me = d.activePlayer!!
        val tomb = d.putPermanentOnBattlefield(me, "Cyclopean Tomb")
        val forest = d.putPermanentOnBattlefield(d.getOpponent(me), "Forest")
        d.myUpkeep(); d.mark(me, tomb, forest); d.spell(me, exile.name, tomb)
        d.state.delayedTriggers.size shouldBe 0
        d.state.projectedState.getSubtypes(forest) shouldBe setOf("Swamp")
    }
    test("dies schedules cleanup only on the original controller's upkeep") {
        val d = driver(); val me = d.activePlayer!!
        val tomb = d.putPermanentOnBattlefield(me, "Cyclopean Tomb")
        val forest = d.putPermanentOnBattlefield(d.getOpponent(me), "Forest")
        d.myUpkeep(); d.mark(me, tomb, forest); d.kill(me, tomb)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN); d.passPriorityUntil(Step.UPKEEP)
        d.activePlayer shouldBe d.getOpponent(me)
        d.state.stack.size shouldBe 0
        (d.state.getEntity(forest)!!.get<CountersComponent>()?.getCount(mire) ?: 0) shouldBe 1
        d.passPriorityUntil(Step.PRECOMBAT_MAIN); d.passPriorityUntil(Step.UPKEEP)
        d.cleanup(me, forest)
        (d.state.getEntity(forest)!!.get<CountersComponent>()?.getCount(mire) ?: 0) shouldBe 0
        d.state.projectedState.getSubtypes(forest) shouldBe setOf("Forest")
        d.state.delayedTriggers.size shouldBe 1
    }
    test("repeating cleanup chooses one marked land per upkeep and removes all counters") {
        val d = driver(); val me = d.activePlayer!!
        val tomb = d.putPermanentOnBattlefield(me, "Cyclopean Tomb")
        val forest = d.putPermanentOnBattlefield(me, "Forest")
        val mountain = d.putPermanentOnBattlefield(me, "Mountain")
        d.myUpkeep(); d.mark(me, tomb, forest)
        d.untapPermanent(tomb); d.mark(me, tomb, mountain)
        d.spell(me, add.name, forest)
        d.addComponent(forest, CountersComponent(mapOf(mire to 3, CounterType.CHARGE to 2)))
        d.kill(me, tomb)
        d.myUpkeep(); d.cleanup(me, forest)
        (d.state.getEntity(forest)!!.get<CountersComponent>()?.getCount(mire) ?: 0) shouldBe 0
        d.state.getEntity(forest)!!.get<CountersComponent>()!!.getCount(CounterType.CHARGE) shouldBe 2
        (d.state.getEntity(mountain)!!.get<CountersComponent>()?.getCount(mire) ?: 0) shouldBe 1
        d.myUpkeep(); d.cleanup(me, mountain)
        (d.state.getEntity(mountain)!!.get<CountersComponent>()?.getCount(mire) ?: 0) shouldBe 0
        d.myUpkeep(); d.cleanup(me)
        d.pendingDecision shouldBe null
    }
    test("removing no counters does not consume cleanup history") {
        val d = driver(); val me = d.activePlayer!!
        val tomb = d.putPermanentOnBattlefield(me, "Cyclopean Tomb")
        val forest = d.putPermanentOnBattlefield(me, "Forest")
        d.myUpkeep(); d.mark(me, tomb, forest); d.spell(me, remove.name, forest)
        d.kill(me, tomb); d.myUpkeep(); d.cleanup(me, forest)
        d.spell(me, add.name, forest)
        d.myUpkeep(); d.cleanup(me, forest)
        (d.state.getEntity(forest)!!.get<CountersComponent>()?.getCount(mire) ?: 0) shouldBe 0
    }
    test("a successfully cleaned land cannot be chosen again after new counters are added") {
        val d = driver(); val me = d.activePlayer!!
        val tomb = d.putPermanentOnBattlefield(me, "Cyclopean Tomb")
        val forest = d.putPermanentOnBattlefield(me, "Forest")
        d.myUpkeep(); d.mark(me, tomb, forest); d.kill(me, tomb)
        d.myUpkeep(); d.cleanup(me, forest); d.spell(me, add.name, forest)
        d.myUpkeep(); d.cleanup(me)
        (d.state.getEntity(forest)!!.get<CountersComponent>()?.getCount(mire) ?: 0) shouldBe 2
    }
    test("an ended Swamp duration does not restart when a new mire counter is added") {
        val d = driver(); val me = d.activePlayer!!
        val tomb = d.putPermanentOnBattlefield(me, "Cyclopean Tomb")
        val forest = d.putPermanentOnBattlefield(me, "Forest")
        d.myUpkeep(); d.mark(me, tomb, forest); d.spell(me, remove.name, forest)
        d.state.projectedState.getSubtypes(forest) shouldBe setOf("Forest")
        d.spell(me, add.name, forest)
        d.state.projectedState.getSubtypes(forest) shouldBe setOf("Forest")
    }
    test("activation on the stack still records the original Tomb after it dies") {
        val d = driver(); val me = d.activePlayer!!
        val tomb = d.putPermanentOnBattlefield(me, "Cyclopean Tomb")
        val forest = d.putPermanentOnBattlefield(me, "Forest")
        d.myUpkeep(); d.giveColorlessMana(me, 2)
        d.submit(ActivateAbility(me, tomb, CyclopeanTomb.activatedAbilities.single().id,
            targets = listOf(ChosenTarget.Permanent(forest)))).error shouldBe null
        d.spell(me, destroy.name, tomb); d.bothPass(); d.bothPass()
        d.state.projectedState.getSubtypes(forest) shouldBe setOf("Swamp")
        d.myUpkeep(); d.cleanup(me, forest)
        d.state.projectedState.getSubtypes(forest) shouldBe setOf("Forest")
    }
    test("a prevented counter placement does not mark the land for later cleanup") {
        val d = driver(); val me = d.activePlayer!!
        val tomb = d.putPermanentOnBattlefield(me, "Cyclopean Tomb")
        val forest = d.putPermanentOnBattlefield(me, "Forest")
        val prohibition = d.putPermanentOnBattlefield(me, noCounters.name)
        d.myUpkeep(); d.giveColorlessMana(me, 2)
        d.submit(ActivateAbility(me, tomb, CyclopeanTomb.activatedAbilities.single().id,
            targets = listOf(ChosenTarget.Permanent(forest)))).error shouldBe null
        d.bothPass()
        d.state.projectedState.getSubtypes(forest) shouldBe setOf("Forest")
        d.spell(me, destroy.name, prohibition); d.spell(me, add.name, forest)
        d.kill(me, tomb); d.myUpkeep(); d.cleanup(me)
        (d.state.getEntity(forest)!!.get<CountersComponent>()?.getCount(mire) ?: 0) shouldBe 2
    }
    test("a returned Tomb's cleanup uses only its new battlefield visit") {
        val d = driver(); val me = d.activePlayer!!
        val tomb = d.putPermanentOnBattlefield(me, "Cyclopean Tomb")
        val forest = d.putPermanentOnBattlefield(me, "Forest")
        val mountain = d.putPermanentOnBattlefield(me, "Mountain")
        d.myUpkeep(); d.mark(me, tomb, forest); d.spell(me, bounce.name, tomb)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        d.giveColorlessMana(me, 4)
        d.castSpell(me, tomb).error shouldBe null; d.bothPass()
        d.myUpkeep()
        d.mark(me, tomb, mountain); d.kill(me, tomb)
        d.myUpkeep(); d.cleanup(me, mountain)
        (d.state.getEntity(mountain)!!.get<CountersComponent>()?.getCount(mire) ?: 0) shouldBe 0
        (d.state.getEntity(forest)!!.get<CountersComponent>()?.getCount(mire) ?: 0) shouldBe 1
        d.state.projectedState.getSubtypes(forest) shouldBe setOf("Swamp")
    }
    test("cleanup follows the controller when Tomb dies without losing its earlier marks") {
        val d = driver(); val me = d.activePlayer!!; val other = d.getOpponent(me)
        val tomb = d.putPermanentOnBattlefield(me, "Cyclopean Tomb")
        val forest = d.putPermanentOnBattlefield(me, "Forest")
        d.myUpkeep(); d.mark(me, tomb, forest)
        d.passPriority(me).error shouldBe null
        d.spell(other, steal.name, tomb)
        d.kill(me, tomb)
        d.state.delayedTriggers.single().controllerId shouldBe other
        d.passPriorityUntil(Step.PRECOMBAT_MAIN); d.passPriorityUntil(Step.UPKEEP)
        d.activePlayer shouldBe other
        d.cleanup(other, forest)
        (d.state.getEntity(forest)!!.get<CountersComponent>()?.getCount(mire) ?: 0) shouldBe 0
    }
})
