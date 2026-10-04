package com.wingedsheep.engine.stack

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.BecomesTargetEvent
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.stack.ActivatedAbilityOnStackComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.state.components.stack.TargetsComponent
import com.wingedsheep.engine.state.components.stack.TriggeredAbilityOnStackComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.ProtectionScope
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe

/**
 * Engine coverage for the two halves of the Agrus Kos shape:
 *
 *  - the `targetsOnlyIt` axis on the becomes-target trigger — "becomes the target of an ability that
 *    targets only it": every chosen target of the ability (players included) must be the observer;
 *  - [com.wingedsheep.sdk.scripting.effects.CopyForEachOtherPossibleTargetEffect] on an **ability**
 *    (CR 707.10d): one copy per other creature the copier controls that the ability could target,
 *    each copy filling every target slot with its one creature, all copies controlled by the copier
 *    whoever controlled the original.
 *
 * Plus the emission fix that makes the multi-slot ruling hold: an object chosen for several
 * instances of "target" becomes the target once, so its trigger fires once.
 */
class CopyAbilityForEachPossibleTargetTest : FunSpec({

    /** Agrus Kos without the payment — the trigger and the copy, nothing else. */
    val observer = card("Sole Target Copier") {
        manaCost = "{3}{W}"
        typeLine = "Creature — Spirit Soldier"
        power = 3
        toughness = 4
        triggeredAbility {
            trigger = Triggers.self.becomesTarget(abilitiesOnly = true, targetsOnlyIt = true)
            effect = Effects.CopyForEachOtherPossibleTarget(
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

    /** {T}: two target slots, each getting a +1/+1 counter. */
    val doublePumper = card("Twin Counter Wand") {
        manaCost = "{1}"
        typeLine = "Artifact"
        activatedAbility {
            cost = Costs.Tap
            val first = target(TargetFilter.Creature)
            val second = target(TargetFilter.Creature)
            effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, first) then
                Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, second)
        }
    }

    /** {T}: target creature gets a counter and target player loses 1 life. */
    val creatureAndPlayer = card("Creature And Player Wand") {
        manaCost = "{1}"
        typeLine = "Artifact"
        activatedAbility {
            cost = Costs.Tap
            val creature = target(TargetFilter.Creature)
            val victim = target(Targets.Player)
            effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, creature) then
                Effects.LoseLife(1, victim)
        }
    }

    /** A spell aimed at a single creature — Agrus Kos's wording is abilities only. */
    val pumpSpell = card("Counter Spell Gift") {
        manaCost = "{G}"
        typeLine = "Instant"
        spell {
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

    /** Protection reads the targeting object's kind — a copy of an activated ability is one. */
    val wardedFromAbilities = card("Ability Warded Bear") {
        manaCost = "{1}{G}"
        typeLine = "Creature — Bear"
        power = 2
        toughness = 2
        keywordAbility(KeywordAbility.Protection(ProtectionScope.ActivatedAbilities))
    }

    val extras: List<CardDefinition> =
        listOf(observer, pumper, doublePumper, creatureAndPlayer, pumpSpell, shrouded, wardedFromAbilities)

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + extras)
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40))
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun GameTestDriver.activate(player: EntityId, source: EntityId, def: CardDefinition, targets: List<ChosenTarget>) =
        submit(
            ActivateAbility(
                playerId = player,
                sourceId = source,
                abilityId = def.activatedAbilities.first().id,
                targets = targets
            )
        )

    fun GameTestDriver.observerTriggersOnStack() = state.stack.count { id ->
        state.getEntity(id)?.get<TriggeredAbilityOnStackComponent>()?.sourceName == "Sole Target Copier"
    }

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

    test("an ability targeting only the observer is copied once for each other creature its controller controls") {
        val driver = createDriver()
        val p1 = driver.player1
        val agrus = driver.putCreatureOnBattlefield(p1, "Sole Target Copier")
        val bearA = driver.putCreatureOnBattlefield(p1, "Grizzly Bears")
        val bearB = driver.putCreatureOnBattlefield(p1, "Grizzly Bears")
        val theirBear = driver.putCreatureOnBattlefield(driver.player2, "Grizzly Bears")
        val wand = driver.putPermanentOnBattlefield(p1, "Counter Wand")

        driver.activate(p1, wand, pumper, listOf(ChosenTarget.Permanent(agrus))).error shouldBe null
        driver.observerTriggersOnStack() shouldBe 1

        driver.bothPass() // resolve the trigger
        val abilities = driver.activatedOnStack()
        withClue("the original plus one copy per other creature p1 controls") { abilities shouldHaveSize 3 }
        val copies = abilities.filter { driver.targetsOf(it) != listOf(ChosenTarget.Permanent(agrus)) }
        copies.map { driver.targetsOf(it).single() } shouldContainExactlyInAnyOrder
            listOf(ChosenTarget.Permanent(bearA), ChosenTarget.Permanent(bearB))
        copies.forEach { driver.state.getEntity(it)!!.get<ActivatedAbilityOnStackComponent>()!!.controllerId shouldBe p1 }

        driver.resolveStack()
        driver.counters(agrus) shouldBe 1
        driver.counters(bearA) shouldBe 1
        driver.counters(bearB) shouldBe 1
        withClue("the opponent's creature is not a candidate") { driver.counters(theirBear) shouldBe 0 }
    }

    test("an ability that also targets another creature doesn't trigger it") {
        val driver = createDriver()
        val p1 = driver.player1
        val agrus = driver.putCreatureOnBattlefield(p1, "Sole Target Copier")
        val bear = driver.putCreatureOnBattlefield(p1, "Grizzly Bears")
        val wand = driver.putPermanentOnBattlefield(p1, "Twin Counter Wand")

        driver.activate(p1, wand, doublePumper, listOf(ChosenTarget.Permanent(agrus), ChosenTarget.Permanent(bear)))
            .error shouldBe null
        driver.observerTriggersOnStack() shouldBe 0
    }

    test("an ability that also targets a player doesn't trigger it") {
        val driver = createDriver()
        val p1 = driver.player1
        val agrus = driver.putCreatureOnBattlefield(p1, "Sole Target Copier")
        driver.putCreatureOnBattlefield(p1, "Grizzly Bears")
        val wand = driver.putPermanentOnBattlefield(p1, "Creature And Player Wand")

        driver.activate(p1, wand, creatureAndPlayer, listOf(ChosenTarget.Permanent(agrus), ChosenTarget.Player(driver.player2)))
            .error shouldBe null
        driver.observerTriggersOnStack() shouldBe 0
    }

    test("every slot aimed at the observer triggers it once, and each copy fills every slot with its creature") {
        val driver = createDriver()
        val p1 = driver.player1
        val agrus = driver.putCreatureOnBattlefield(p1, "Sole Target Copier")
        val bear = driver.putCreatureOnBattlefield(p1, "Grizzly Bears")
        val wand = driver.putPermanentOnBattlefield(p1, "Twin Counter Wand")

        val result = driver.activate(p1, wand, doublePumper, listOf(ChosenTarget.Permanent(agrus), ChosenTarget.Permanent(agrus)))
        result.error shouldBe null
        withClue("one object chosen twice becomes the target once") {
            result.events.filterIsInstance<BecomesTargetEvent>().count { it.targetEntityId == agrus } shouldBe 1
        }
        driver.observerTriggersOnStack() shouldBe 1

        driver.bothPass()
        val copy = driver.activatedOnStack().single { driver.targetsOf(it).none { t -> t == ChosenTarget.Permanent(agrus) } }
        driver.targetsOf(copy) shouldBe listOf(ChosenTarget.Permanent(bear), ChosenTarget.Permanent(bear))

        driver.resolveStack()
        driver.counters(bear) shouldBe 2
        driver.counters(agrus) shouldBe 2
    }

    test("a spell targeting only the observer doesn't trigger it") {
        val driver = createDriver()
        val p1 = driver.player1
        val agrus = driver.putCreatureOnBattlefield(p1, "Sole Target Copier")
        driver.putCreatureOnBattlefield(p1, "Grizzly Bears")
        val gift = driver.putCardInHand(p1, "Counter Spell Gift")
        driver.giveMana(p1, Color.GREEN, 1)

        driver.castSpellWithTargets(p1, gift, listOf(ChosenTarget.Permanent(agrus))).error shouldBe null
        driver.observerTriggersOnStack() shouldBe 0
    }

    test("a creature the ability couldn't target gets no copy") {
        val driver = createDriver()
        val p1 = driver.player1
        val agrus = driver.putCreatureOnBattlefield(p1, "Sole Target Copier")
        val bear = driver.putCreatureOnBattlefield(p1, "Grizzly Bears")
        val shroudBear = driver.putCreatureOnBattlefield(p1, "Shrouded Bear")
        val wand = driver.putPermanentOnBattlefield(p1, "Counter Wand")

        driver.activate(p1, wand, pumper, listOf(ChosenTarget.Permanent(agrus))).error shouldBe null
        driver.bothPass()
        driver.activatedOnStack() shouldHaveSize 2

        driver.resolveStack()
        driver.counters(bear) shouldBe 1
        driver.counters(shroudBear) shouldBe 0
    }

    test("a creature with protection from activated abilities gets no copy of one") {
        val driver = createDriver()
        val p1 = driver.player1
        val agrus = driver.putCreatureOnBattlefield(p1, "Sole Target Copier")
        val bear = driver.putCreatureOnBattlefield(p1, "Grizzly Bears")
        val warded = driver.putCreatureOnBattlefield(p1, "Ability Warded Bear")
        val wand = driver.putPermanentOnBattlefield(p1, "Counter Wand")

        driver.activate(p1, wand, pumper, listOf(ChosenTarget.Permanent(agrus))).error shouldBe null
        driver.bothPass()
        driver.activatedOnStack() shouldHaveSize 2

        driver.resolveStack()
        driver.counters(bear) shouldBe 1
        driver.counters(warded) shouldBe 0
    }

    test("an opponent's ability is copied for the observer's controller, onto that player's creatures") {
        val driver = createDriver()
        val p1 = driver.player1
        val p2 = driver.player2
        val agrus = driver.putCreatureOnBattlefield(p1, "Sole Target Copier")
        val myBear = driver.putCreatureOnBattlefield(p1, "Grizzly Bears")
        val theirBear = driver.putCreatureOnBattlefield(p2, "Grizzly Bears")
        val theirWand = driver.putPermanentOnBattlefield(p2, "Counter Wand")

        driver.passPriority(p1)
        driver.activate(p2, theirWand, pumper, listOf(ChosenTarget.Permanent(agrus))).error shouldBe null
        driver.observerTriggersOnStack() shouldBe 1

        driver.bothPass()
        val copy = driver.activatedOnStack().single { driver.targetsOf(it) != listOf(ChosenTarget.Permanent(agrus)) }
        driver.targetsOf(copy) shouldBe listOf(ChosenTarget.Permanent(myBear))
        driver.state.getEntity(copy)!!.get<ActivatedAbilityOnStackComponent>()!!.controllerId shouldBe p1

        driver.resolveStack()
        driver.counters(myBear) shouldBe 1
        driver.counters(theirBear) shouldBe 0
    }
})
