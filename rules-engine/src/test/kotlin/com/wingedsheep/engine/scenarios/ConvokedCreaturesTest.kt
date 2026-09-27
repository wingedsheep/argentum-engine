package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.state.components.battlefield.BattlefieldEntryTimestampComponent
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.times
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AlternativePaymentChoice
import com.wingedsheep.sdk.scripting.ConvokePayment
import com.wingedsheep.sdk.scripting.EntersWithDynamicCounters
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * "The creatures that convoked it" (CR 702.51c — a creature tapped to pay for mana in a spell's
 * total cost through convoke is said to have "convoked" that spell).
 *
 * The convoke taps are recorded on the spell, carried onto the resolving permanent's cast-choices
 * bag under `ChoiceSlot.CONVOKED_CREATURES`, and read two ways:
 *  - `DynamicAmounts.convokedCreatureCount()` — the number that convoked it, counting a creature
 *    that has since left the battlefield;
 *  - `CardPredicate.ConvokedSource` — each creature that convoked it and is still that same
 *    object (one that left and came back is a new object, CR 400.7).
 */
class ConvokedCreaturesTest : FunSpec({

    // Enters with two +1/+1 counters for each creature that convoked it (Ancient Imperiosaur shape).
    val convokeCounterBeast = card("Convoke Counter Beast") {
        manaCost = "{3}{G}"
        typeLine = "Creature — Beast"
        power = 1
        toughness = 1
        oracleText = "Convoke\nThis creature enters with two +1/+1 counters on it for each creature that convoked it."
        keywords(Keyword.CONVOKE)
        replacementEffect(EntersWithDynamicCounters(count = DynamicAmounts.convokedCreatureCount() * 2))
    }

    // ETB: a +1/+1 counter on each creature that convoked it (Zephyr Singer shape).
    val convokeRewarder = card("Convoke Rewarder") {
        manaCost = "{3}{G}"
        typeLine = "Creature — Elf"
        power = 2
        toughness = 2
        oracleText = "Convoke\nWhen this creature enters, put a +1/+1 counter on each creature that convoked it."
        keywords(Keyword.CONVOKE)
        triggeredAbility {
            trigger = Triggers.self.enters()
            effect = Effects.ForEachInGroup(
                GroupFilter(GameObjectFilter.Creature.thatConvokedSource()),
                Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.IterationEntity)
            )
        }
    }

    val greenElf = card("Green Elf") {
        manaCost = "{G}"
        typeLine = "Creature — Elf"
        power = 1
        toughness = 1
    }

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(convokeCounterBeast, convokeRewarder, greenElf))
        driver.initMirrorMatch(deck = Deck.of("Forest" to 20), skipMulligans = true)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun GameTestDriver.plusOnes(id: EntityId): Int =
        state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    fun GameTestDriver.castConvoked(player: EntityId, name: String, convokers: List<EntityId>, mana: Int): EntityId {
        if (mana > 0) giveMana(player, Color.GREEN, mana)
        val spellId = putCardInHand(player, name)
        val result = submit(
            CastSpell(
                playerId = player,
                cardId = spellId,
                paymentStrategy = PaymentStrategy.FromPool,
                alternativePayment = AlternativePaymentChoice(
                    convokedCreatures = convokers.associateWith { ConvokePayment(color = null) }
                )
            )
        )
        result.outcome shouldBe Outcome.Done
        return spellId
    }

    test("the count is the number of creatures tapped for convoke") {
        val driver = createDriver()
        val player = driver.activePlayer!!
        val elves = (1..3).map { driver.putCreatureOnBattlefield(player, "Green Elf") }

        val beast = driver.castConvoked(player, "Convoke Counter Beast", elves, mana = 1)
        driver.bothPass()

        driver.plusOnes(beast) shouldBe 6
    }

    test("cast without convoke, nothing convoked it") {
        val driver = createDriver()
        val player = driver.activePlayer!!
        driver.putCreatureOnBattlefield(player, "Green Elf")

        val beast = driver.castConvoked(player, "Convoke Counter Beast", emptyList(), mana = 4)
        driver.bothPass()

        driver.plusOnes(beast) shouldBe 0
    }

    test("a creature that convoked it and then left still counts") {
        val driver = createDriver()
        val player = driver.activePlayer!!
        val elves = (1..2).map { driver.putCreatureOnBattlefield(player, "Green Elf") }

        val beast = driver.castConvoked(player, "Convoke Counter Beast", elves, mana = 2)
        driver.moveToGraveyard(elves[0])
        driver.bothPass()

        driver.plusOnes(beast) shouldBe 4
    }

    test("each creature that convoked it gets the counter; an untapped bystander doesn't") {
        val driver = createDriver()
        val player = driver.activePlayer!!
        val convokers = (1..2).map { driver.putCreatureOnBattlefield(player, "Green Elf") }
        val bystander = driver.putCreatureOnBattlefield(player, "Green Elf")

        val rewarder = driver.castConvoked(player, "Convoke Rewarder", convokers, mana = 2)
        driver.bothPass() // resolve the creature spell
        driver.bothPass() // resolve its enters trigger

        convokers.forEach { driver.plusOnes(it) shouldBe 1 }
        driver.plusOnes(bystander) shouldBe 0
        driver.plusOnes(rewarder) shouldBe 0
    }

    test("a convoker that left and returned is a new object and gets nothing (CR 400.7)") {
        val driver = createDriver()
        val player = driver.activePlayer!!
        val (stayed, blinked) = (1..2).map { driver.putCreatureOnBattlefield(player, "Green Elf") }

        driver.castConvoked(player, "Convoke Rewarder", listOf(stayed, blinked), mana = 2)
        // Simulate a blink: same entity id, new battlefield-entry stamp.
        driver.replaceState(
            driver.state.updateEntity(blinked) { it.with(BattlefieldEntryTimestampComponent(timestamp = 9_999L)) }
        )
        driver.bothPass()
        driver.bothPass()

        driver.plusOnes(stayed) shouldBe 1
        driver.plusOnes(blinked) shouldBe 0
    }
})
