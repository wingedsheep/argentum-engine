package com.wingedsheep.engine

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.CardsDrawnEvent
import com.wingedsheep.engine.core.CardsSelectedResponse
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.core.PermanentConnivedEvent
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifyKeywordAction
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Connive N (CR 701.50d/e): draw N, discard N, then one +1/+1 counter per nonland card discarded;
 * a connive 0 is no connive at all. Card-level coverage lives in `SpymastersVaultScenarioTest`.
 *
 *  - CR 701.50d — N cards drawn, exactly N discarded, counters equal to the *nonland* discards.
 *  - CR 701.50e — connive 0: nothing drawn or discarded, no replacement, no connive trigger.
 *  - CR 701.50f — a connive N is one connive: one event, one trigger, one replacement.
 *  - N is fixed before the draw, so an amount the connive itself changes (hand size) doesn't drift.
 */
class ConniveNTest : FunSpec({

    fun conniver(name: String, count: DynamicAmount) = card(name) {
        manaCost = "{2}{U}"
        typeLine = "Creature — Human"
        power = 1
        toughness = 1
        oracleText = "{T}: This creature connives N."
        activatedAbility {
            cost = com.wingedsheep.sdk.scripting.AbilityCost.Tap
            effect = Effects.Connive(EffectTarget.Self, count)
        }
    }

    val ConniveTwo = conniver("Connive Two", DynamicAmount.Fixed(2))
    val ConniveZero = conniver("Connive Zero", DynamicAmount.Fixed(0))
    val ConniveHand = conniver("Connive Hand", DynamicAmounts.cardsInYourHand())

    /** "If a creature you control would connive, instead you gain 3 life, then it connives." */
    val PrefixSource = card("Connive N Prefix Source") {
        manaCost = "{2}{U}"
        typeLine = "Enchantment"
        oracleText = "If a creature you control would connive, instead you gain 3 life, then that creature connives."
        replacementEffect(
            ModifyKeywordAction(
                prefixEffect = Effects.GainLife(3),
                appliesTo = EventPattern.ConnivedEvent(filter = GameObjectFilter.Creature.youControl()),
            )
        )
    }

    /** "Whenever a creature you control connives, you gain 7 life." */
    val Watcher = card("Connive N Watcher") {
        manaCost = "{1}{U}"
        typeLine = "Enchantment"
        oracleText = "Whenever a creature you control connives, you gain 7 life."
        triggeredAbility {
            trigger = Triggers.a(GameObjectFilter.Creature.youControl()).connives()
            effect = Effects.GainLife(7)
        }
    }

    fun newDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(ConniveTwo, ConniveZero, ConniveHand, PrefixSource, Watcher))
        driver.initMirrorMatch(deck = Deck.of("Island" to 30, "Forest" to 30), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    val byName = listOf(ConniveTwo, ConniveZero, ConniveHand).associateBy { it.name }

    fun activate(driver: GameTestDriver, player: EntityId, creatureName: String): EntityId {
        val creature = driver.putCreatureOnBattlefield(player, creatureName)
        driver.removeSummoningSickness(creature)
        val abilityId = byName.getValue(creatureName).activatedAbilities.single().id
        driver.submit(ActivateAbility(playerId = player, sourceId = creature, abilityId = abilityId))
            .outcome shouldBe Outcome.Done
        driver.bothPass()
        return creature
    }

    fun drain(driver: GameTestDriver) {
        var guard = 0
        while (guard++ < 30 && (driver.pendingDecision != null || driver.state.stack.isNotEmpty())) {
            if (driver.pendingDecision != null) driver.autoResolveDecision() else driver.bothPass()
        }
    }

    fun plusOnes(driver: GameTestDriver, creature: EntityId): Int =
        driver.state.getEntity(creature)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    test("connive 2 draws two, discards exactly two, and counts only the nonland discards") {
        val driver = newDriver()
        val player = driver.activePlayer!!
        driver.putCardOnTopOfLibrary(player, "Forest")
        driver.putCardOnTopOfLibrary(player, "Grizzly Bears")
        val bears = driver.putCardInHand(player, "Grizzly Bears")
        val forest = driver.putCardInHand(player, "Forest")
        val handBefore = driver.getHandSize(player)
        val eventsBefore = driver.events.size

        val creature = activate(driver, player, "Connive Two")

        driver.events.drop(eventsBefore).filterIsInstance<CardsDrawnEvent>().sumOf { it.cardIds.size } shouldBe 2
        val decision = driver.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
        decision.minSelections shouldBe 2
        decision.maxSelections shouldBe 2
        driver.submitDecision(player, CardsSelectedResponse(decision.id, listOf(bears, forest)))
        drain(driver)

        driver.getHandSize(player) shouldBe handBefore
        driver.getGraveyard(player).containsAll(listOf(bears, forest)) shouldBe true
        withClue("one nonland discarded -> one +1/+1 counter") { plusOnes(driver, creature) shouldBe 1 }
    }

    test("two nonland discards put two counters on the conniving creature") {
        val driver = newDriver()
        val player = driver.activePlayer!!
        val a = driver.putCardInHand(player, "Grizzly Bears")
        val b = driver.putCardInHand(player, "Grizzly Bears")

        val creature = activate(driver, player, "Connive Two")
        val decision = driver.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
        driver.submitDecision(player, CardsSelectedResponse(decision.id, listOf(a, b)))
        drain(driver)

        plusOnes(driver, creature) shouldBe 2
    }

    test("a connive N is one connive: the replacement and the connive trigger apply once") {
        val driver = newDriver()
        val player = driver.activePlayer!!
        driver.putPermanentOnBattlefield(player, "Connive N Prefix Source")
        driver.putPermanentOnBattlefield(player, "Connive N Watcher")

        activate(driver, player, "Connive Two")
        driver.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
        withClue("prefix resolves once, before the discard") { driver.getLifeTotal(player) shouldBe 23 }
        drain(driver)

        driver.events.filterIsInstance<PermanentConnivedEvent>().size shouldBe 1
        driver.getLifeTotal(player) shouldBe 30
    }

    test("CR 701.50e: connive 0 draws, discards, replaces and triggers nothing") {
        val driver = newDriver()
        val player = driver.activePlayer!!
        driver.putPermanentOnBattlefield(player, "Connive N Prefix Source")
        driver.putPermanentOnBattlefield(player, "Connive N Watcher")
        val handBefore = driver.getHandSize(player)
        val eventsBefore = driver.events.size

        activate(driver, player, "Connive Zero")

        driver.pendingDecision shouldBe null
        drain(driver)
        driver.events.drop(eventsBefore).filterIsInstance<CardsDrawnEvent>() shouldBe emptyList()
        driver.events.drop(eventsBefore).filterIsInstance<PermanentConnivedEvent>() shouldBe emptyList()
        driver.getHandSize(player) shouldBe handBefore
        driver.getLifeTotal(player) shouldBe 20
    }

    test("N is fixed before the draw — drawing doesn't raise the number discarded") {
        val driver = newDriver()
        val player = driver.activePlayer!!
        val creature = driver.putCreatureOnBattlefield(player, "Connive Hand")
        driver.removeSummoningSickness(creature)
        val n = driver.getHandSize(player)
        val abilityId = ConniveHand.activatedAbilities.single().id
        driver.submit(ActivateAbility(playerId = player, sourceId = creature, abilityId = abilityId))
            .outcome shouldBe Outcome.Done
        driver.bothPass()

        driver.getHandSize(player) shouldBe 2 * n
        val decision = driver.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
        decision.minSelections shouldBe n
        decision.maxSelections shouldBe n
        drain(driver)
        driver.getHandSize(player) shouldBe n
    }
})
