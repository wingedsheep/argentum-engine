package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.trk.cards.DominionSaboteur
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Dominion Saboteur (TRK #54) — {4}{U} Creature — Shapeshifter 3/3.
 *
 *   You may have this creature enter as a copy of any artifact or creature on the battlefield,
 *   except it isn't legendary. If you do, it enters with additional counters on it equal to the
 *   same number and kinds of counters the copied permanent has on it.
 *
 * Pins the mirrored-counters copy rider: every kind the copied permanent carries comes over in the
 * same number, the copied permanent keeps its own, a counterless copy gets none, and declining the
 * copy enters a plain 3/3 with nothing.
 */
class DominionSaboteurScenarioTest : FunSpec({

    val legend = card("Saboteur Test Legend") {
        manaCost = "{1}{G}"
        typeLine = "Legendary Creature — Elf"
        power = 2
        toughness = 2
    }
    val relic = card("Saboteur Test Relic") {
        manaCost = "{2}"
        typeLine = "Artifact"
    }

    val returner = card("Saboteur Test Return") {
        manaCost = "{U}"
        typeLine = "Instant"
        spell { val t = target(TargetFilter.PermanentInYourGraveyard); effect = Effects.PutOntoBattlefield(t) }
    }

    fun setup(): Pair<GameTestDriver, EntityId> {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(DominionSaboteur)
        driver.registerCard(legend)
        driver.registerCard(relic)
        driver.registerCard(returner)
        driver.initMirrorMatch(deck = Deck.of("Island" to 40), startingLife = 20)
        val player = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver to player
    }

    fun GameTestDriver.giveCounters(id: EntityId, counters: Map<CounterType, Int>) {
        replaceState(state.updateEntity(id) { it.with(CountersComponent(counters)) })
    }

    fun GameTestDriver.countersOn(id: EntityId): Map<CounterType, Int> =
        state.getEntity(id)?.get<CountersComponent>()?.counters?.filterValues { it > 0 } ?: emptyMap()

    fun GameTestDriver.castSaboteur(player: EntityId): EntityId {
        val saboteur = putCardInHand(player, "Dominion Saboteur")
        giveMana(player, Color.BLUE, 5)
        castSpell(player, saboteur).error shouldBe null
        bothPass()
        return saboteur
    }

    test("copies a creature and enters with the same number and kinds of its counters") {
        val (driver, player) = setup()
        val target = driver.putCreatureOnBattlefield(driver.getOpponent(player), "Grizzly Bears")
        val mixed = mapOf(CounterType.PLUS_ONE_PLUS_ONE to 2, CounterType.LORE to 1, CounterType.SHIELD to 1)
        driver.giveCounters(target, mixed)

        val saboteur = driver.castSaboteur(player)
        val decision = driver.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
        decision.options shouldContainAll listOf(target)
        driver.submitCardSelection(player, listOf(target)).error shouldBe null

        driver.state.getEntity(saboteur)?.get<CardComponent>()?.name shouldBe "Grizzly Bears"
        driver.countersOn(saboteur) shouldBe mixed
        driver.countersOn(target) shouldBe mixed
        driver.state.projectedState.getPower(saboteur) shouldBe 4
    }

    test("copies a noncreature artifact with its charge counters") {
        val (driver, player) = setup()
        val target = driver.putPermanentOnBattlefield(player, "Saboteur Test Relic")
        driver.giveCounters(target, mapOf(CounterType.CHARGE to 3))

        val saboteur = driver.castSaboteur(player)
        driver.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
        driver.submitCardSelection(player, listOf(target)).error shouldBe null

        driver.state.getEntity(saboteur)?.get<CardComponent>()?.name shouldBe "Saboteur Test Relic"
        driver.state.getBattlefield().contains(saboteur) shouldBe true
        driver.countersOn(saboteur) shouldBe mapOf(CounterType.CHARGE to 3)
    }

    test("a copy of a legendary permanent isn't legendary, so both stay; no counters to mirror") {
        val (driver, player) = setup()
        val target = driver.putCreatureOnBattlefield(player, "Saboteur Test Legend")

        val saboteur = driver.castSaboteur(player)
        driver.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
        driver.submitCardSelection(player, listOf(target)).error shouldBe null

        driver.state.getBattlefield().contains(saboteur) shouldBe true
        driver.state.getBattlefield().contains(target) shouldBe true
        driver.state.projectedState.isLegendary(saboteur) shouldBe false
        driver.countersOn(saboteur) shouldBe emptyMap()
    }

    test("declining the copy enters a plain 3/3 with no counters") {
        val (driver, player) = setup()
        val target = driver.putCreatureOnBattlefield(driver.getOpponent(player), "Grizzly Bears")
        driver.giveCounters(target, mapOf(CounterType.PLUS_ONE_PLUS_ONE to 2))

        val saboteur = driver.castSaboteur(player)
        driver.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
        driver.submitCardSelection(player, emptyList()).error shouldBe null

        driver.state.getEntity(saboteur)?.get<CardComponent>()?.name shouldBe "Dominion Saboteur"
        driver.countersOn(saboteur) shouldBe emptyMap()
        driver.state.projectedState.getPower(saboteur) shouldBe 3
    }

    test("put onto the battlefield without being cast, it still mirrors the copied counters") {
        val (driver, player) = setup()
        val target = driver.putCreatureOnBattlefield(driver.getOpponent(player), "Grizzly Bears")
        driver.giveCounters(target, mapOf(CounterType.PLUS_ONE_PLUS_ONE to 1, CounterType.STUN to 2))
        val saboteur = driver.putCardInGraveyard(player, "Dominion Saboteur")
        val spell = driver.putCardInHand(player, "Saboteur Test Return")
        driver.giveMana(player, Color.BLUE, 1)
        driver.castSpellWithTargets(player, spell, listOf(ChosenTarget.Card(saboteur, player, Zone.GRAVEYARD)))
            .error shouldBe null
        driver.bothPass()

        driver.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
        driver.submitCardSelection(player, listOf(target)).error shouldBe null

        val entered = driver.state.getBattlefield().single {
            it != target && driver.state.getEntity(it)?.get<CardComponent>()?.name == "Grizzly Bears"
        }
        driver.countersOn(entered) shouldBe mapOf(CounterType.PLUS_ONE_PLUS_ONE to 1, CounterType.STUN to 2)
    }
})
