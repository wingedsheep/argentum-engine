package com.wingedsheep.engine.stack

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.stack.ActivatedAbilityOnStackComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.state.components.stack.TargetsComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Engine coverage for `CopyForOtherPossibleTargetsEffect(selection = ONE_CHOSEN)` — Beamsplitter
 * Mage's "choose one of those creatures. Copy that spell. The copy targets the chosen creature."
 *
 * The candidate set is the CR 707.10d one shared with the Zada family (legal for every instance of
 * "target", other than what the original targets); only one copy is made. Several candidates pause
 * for a board choice, one candidate is used without a prompt, none makes no copy. The spell side is
 * covered by `BeamsplitterMageScenarioTest`; this drives the ability side.
 */
class CopyForChosenPossibleTargetTest : FunSpec({

    /** Agrus Kos's trigger with Beamsplitter Mage's choose-one copy. */
    val observer = card("Chosen Target Copier") {
        manaCost = "{3}{W}"
        typeLine = "Creature — Spirit Soldier"
        power = 3
        toughness = 4
        triggeredAbility {
            trigger = Triggers.self.becomesTarget(abilitiesOnly = true, targetsOnlyIt = true)
            effect = Effects.CopyForChosenOtherPossibleTarget(
                candidates = GameObjectFilter.Creature.youControl(),
                target = EffectTarget.TargetingSource
            )
        }
    }

    /** {T}: put a +1/+1 counter on target creature. */
    val pumper = card("Counter Wand") {
        manaCost = "{1}"
        typeLine = "Artifact"
        activatedAbility {
            cost = Costs.Tap
            val creature = target(TargetFilter.Creature)
            effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, creature)
        }
    }

    val shrouded = card("Shrouded Bear") {
        manaCost = "{1}{G}"
        typeLine = "Creature — Bear"
        power = 2
        toughness = 2
        keywords(Keyword.SHROUD)
    }

    val extras: List<CardDefinition> = listOf(observer, pumper, shrouded)

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + extras)
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40))
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun GameTestDriver.activateWand(player: EntityId, wand: EntityId, target: EntityId) =
        submit(
            ActivateAbility(
                playerId = player,
                sourceId = wand,
                abilityId = pumper.activatedAbilities.first().id,
                targets = listOf(ChosenTarget.Permanent(target))
            )
        )

    fun GameTestDriver.activatedOnStack() = state.stack.filter { id ->
        state.getEntity(id)?.has<ActivatedAbilityOnStackComponent>() == true
    }

    fun GameTestDriver.targetsOf(id: EntityId): List<ChosenTarget> =
        state.getEntity(id)?.get<TargetsComponent>()?.targets.orEmpty()

    fun GameTestDriver.counters(id: EntityId): Int =
        state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    fun GameTestDriver.resolveStack() {
        var guard = 0
        while (state.stack.isNotEmpty() && guard++ < 20) bothPass()
    }

    test("several candidates: the copier chooses one, and exactly one copy targets it") {
        val driver = createDriver()
        val p1 = driver.player1
        val copier = driver.putCreatureOnBattlefield(p1, "Chosen Target Copier")
        val bearA = driver.putCreatureOnBattlefield(p1, "Grizzly Bears")
        val bearB = driver.putCreatureOnBattlefield(p1, "Grizzly Bears")
        driver.putCreatureOnBattlefield(p1, "Shrouded Bear")
        val theirBear = driver.putCreatureOnBattlefield(driver.player2, "Grizzly Bears")
        val wand = driver.putPermanentOnBattlefield(p1, "Counter Wand")

        driver.activateWand(p1, wand, copier).error shouldBe null
        driver.bothPass() // resolve the trigger — it pauses for the choice

        val decision = driver.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
        decision.playerId shouldBe p1
        decision.useTargetingUI shouldBe true
        decision.minSelections shouldBe 1
        decision.maxSelections shouldBe 1
        withClue("only the copier's other creatures the ability could target: not itself, not shroud, not the opponent's") {
            decision.options shouldContainExactlyInAnyOrder listOf(bearA, bearB)
        }

        driver.submitCardSelection(p1, listOf(bearB)).error shouldBe null
        val abilities = driver.activatedOnStack()
        withClue("the original plus one copy") { abilities shouldHaveSize 2 }
        abilities.map { driver.targetsOf(it).single() } shouldContainExactlyInAnyOrder
            listOf(ChosenTarget.Permanent(copier), ChosenTarget.Permanent(bearB))

        driver.resolveStack()
        driver.counters(copier) shouldBe 1
        driver.counters(bearB) shouldBe 1
        driver.counters(bearA) shouldBe 0
        driver.counters(theirBear) shouldBe 0
    }

    test("a choice outside the offered creatures is rejected") {
        val driver = createDriver()
        val p1 = driver.player1
        val copier = driver.putCreatureOnBattlefield(p1, "Chosen Target Copier")
        driver.putCreatureOnBattlefield(p1, "Grizzly Bears")
        driver.putCreatureOnBattlefield(p1, "Grizzly Bears")
        val theirBear = driver.putCreatureOnBattlefield(driver.player2, "Grizzly Bears")
        val wand = driver.putPermanentOnBattlefield(p1, "Counter Wand")

        driver.activateWand(p1, wand, copier).error shouldBe null
        driver.bothPass()
        driver.pendingDecision.shouldNotBeNull()
        driver.submitCardSelection(p1, listOf(theirBear)).error.shouldNotBeNull()
    }

    test("a single candidate gets the copy without a prompt") {
        val driver = createDriver()
        val p1 = driver.player1
        val copier = driver.putCreatureOnBattlefield(p1, "Chosen Target Copier")
        val bear = driver.putCreatureOnBattlefield(p1, "Grizzly Bears")
        driver.putCreatureOnBattlefield(p1, "Shrouded Bear")
        val wand = driver.putPermanentOnBattlefield(p1, "Counter Wand")

        driver.activateWand(p1, wand, copier).error shouldBe null
        driver.bothPass()
        driver.pendingDecision shouldBe null
        driver.activatedOnStack() shouldHaveSize 2

        driver.resolveStack()
        driver.counters(copier) shouldBe 1
        driver.counters(bear) shouldBe 1
    }

    test("no other creature it could target: nothing is copied") {
        val driver = createDriver()
        val p1 = driver.player1
        val copier = driver.putCreatureOnBattlefield(p1, "Chosen Target Copier")
        driver.putCreatureOnBattlefield(p1, "Shrouded Bear")
        driver.putCreatureOnBattlefield(driver.player2, "Grizzly Bears")
        val wand = driver.putPermanentOnBattlefield(p1, "Counter Wand")

        driver.activateWand(p1, wand, copier).error shouldBe null
        driver.bothPass()
        driver.pendingDecision shouldBe null
        driver.activatedOnStack() shouldHaveSize 1

        driver.resolveStack()
        driver.counters(copier) shouldBe 1
    }
})
