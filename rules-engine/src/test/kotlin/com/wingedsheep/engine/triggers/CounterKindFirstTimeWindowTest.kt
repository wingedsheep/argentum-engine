package com.wingedsheep.engine.triggers

import com.wingedsheep.engine.core.CountersAddedEvent
import com.wingedsheep.engine.event.TriggerDetector
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.handlers.effects.DamageUtils
import com.wingedsheep.engine.state.ComponentContainer
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.identity.OwnerComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.CardType
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.TypeLine
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CreatureStats
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.GameObjectFilter
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe

/**
 * The per-kind "first time counters this turn" window — `getsCounters(type = X,
 * firstTimeEachTurn = true)`, "if it's the first time **+1/+1** counters have been put on that
 * permanent this turn" (Botanical Brawler).
 *
 * The window is scoped like the trigger: a kind-scoped trigger reads
 * [CountersAddedEvent.firstOfTypeThisTurn], so an earlier counter of another kind doesn't close it;
 * a kind-agnostic one ("counters", Stalwart Successor) keeps reading
 * [CountersAddedEvent.firstThisTurn]. The per-kind window is not creature-only.
 */
class CounterKindFirstTimeWindowTest : FunSpec({

    val you = EntityId.generate()

    fun cardOf(types: Set<CardType>) = CardComponent(
        cardDefinitionId = "Test",
        name = "Test",
        manaCost = ManaCost(emptyList()),
        typeLine = TypeLine(cardTypes = types),
        ownerId = you,
        baseStats = if (CardType.CREATURE in types) CreatureStats(2, 2) else null
    )

    fun base(): GameState = GameState().withEntity(you, ComponentContainer())

    fun GameState.onBattlefield(id: EntityId, types: Set<CardType>): GameState =
        withEntity(id, ComponentContainer().with(cardOf(types)).with(OwnerComponent(you)).with(ControllerComponent(you)))
            .addToZone(ZoneKey(you, Zone.BATTLEFIELD), id)

    context("DamageUtils per-kind window") {

        test("a counter of another kind leaves the window open; one of the same kind closes it") {
            val id = EntityId.generate()
            var state = base().onBattlefield(id, setOf(CardType.CREATURE))
            DamageUtils.isFirstCounterOfTypeThisTurn(state, id, CounterType.PLUS_ONE_PLUS_ONE) shouldBe true

            state = DamageUtils.markCounterPlacedOnCreature(state, you, id, CounterType.MINUS_ONE_MINUS_ONE)
            withClue("a -1/-1 counter closes the any-kind window") {
                DamageUtils.isFirstCounterThisTurn(state, id) shouldBe false
            }
            withClue("but not the +1/+1 window") {
                DamageUtils.isFirstCounterOfTypeThisTurn(state, id, CounterType.PLUS_ONE_PLUS_ONE) shouldBe true
            }

            state = DamageUtils.markCounterPlacedOnCreature(state, you, id, CounterType.PLUS_ONE_PLUS_ONE)
            DamageUtils.isFirstCounterOfTypeThisTurn(state, id, CounterType.PLUS_ONE_PLUS_ONE) shouldBe false
        }

        test("the window closes on a noncreature permanent too") {
            val artifact = EntityId.generate()
            val state = DamageUtils.markCounterPlacedOnCreature(
                base().onBattlefield(artifact, setOf(CardType.ARTIFACT)), you, artifact, CounterType.PLUS_ONE_PLUS_ONE
            )
            DamageUtils.isFirstCounterOfTypeThisTurn(state, artifact, CounterType.PLUS_ONE_PLUS_ONE) shouldBe false
        }

        test("recordCounterPlacement reports both windows, read before marking") {
            val id = EntityId.generate()
            val start = DamageUtils.markCounterPlacedOnCreature(
                base().onBattlefield(id, setOf(CardType.CREATURE)), you, id, CounterType.SHIELD
            )
            val (afterPlus, anyFirst, kindFirst) =
                DamageUtils.recordCounterPlacement(start, id, CounterType.PLUS_ONE_PLUS_ONE, placerId = you)
            anyFirst shouldBe false
            kindFirst shouldBe true
            val (_, _, kindFirstAgain) =
                DamageUtils.recordCounterPlacement(afterPlus, id, CounterType.PLUS_ONE_PLUS_ONE, placerId = you)
            kindFirstAgain shouldBe false
        }
    }

    context("trigger matching") {

        val kindObserver = card("Kind Window Observer") {
            manaCost = "{0}"
            typeLine = "Creature — Elemental"
            power = 0
            toughness = 1
            triggeredAbility {
                trigger = Triggers.another(GameObjectFilter.Permanent.youControl())
                    .getsCounters(type = CounterType.PLUS_ONE_PLUS_ONE, firstTimeEachTurn = true)
                effect = Effects.DrawCards(1)
            }
        }
        val anyObserver = card("Any Window Observer") {
            manaCost = "{0}"
            typeLine = "Creature — Elemental"
            power = 0
            toughness = 1
            triggeredAbility {
                trigger = Triggers.another(GameObjectFilter.Permanent.youControl())
                    .getsCounters(firstTimeEachTurn = true)
                effect = Effects.DrawCards(1)
            }
        }

        fun driver(): GameTestDriver = GameTestDriver().apply {
            registerCards(TestCards.all + listOf(kindObserver, anyObserver))
            initMirrorMatch(deck = Deck.of("Plains" to 40))
        }

        fun triggersOf(d: GameTestDriver, event: CountersAddedEvent, sourceId: EntityId) =
            TriggerDetector(
                d.cardRegistry,
                predicateEvaluator = PredicateEvaluator(cardRegistry = null),
                conditionEvaluator = PredicateEvaluator(cardRegistry = null).conditions
            ).detectTriggers(d.state, listOf(event))
                .filter { it.ability.trigger is EventPattern.CountersPlacedEvent && it.sourceId == sourceId }

        fun placed(id: EntityId, anyFirst: Boolean, kindFirst: Boolean, type: CounterType = CounterType.PLUS_ONE_PLUS_ONE) =
            CountersAddedEvent(id, type, 1, "", firstThisTurn = anyFirst, firstOfTypeThisTurn = kindFirst, placedBy = null)

        test("a kind-scoped trigger fires on the first +1/+1 placement after another kind") {
            val d = driver()
            val kind = d.putCreatureOnBattlefield(d.player1, "Kind Window Observer")
            val any = d.putCreatureOnBattlefield(d.player1, "Any Window Observer")
            val bears = d.putCreatureOnBattlefield(d.player1, "Grizzly Bears")
            val event = placed(bears, anyFirst = false, kindFirst = true)
            withClue("the +1/+1 window is still open") { triggersOf(d, event, kind) shouldHaveSize 1 }
            withClue("the any-kind window already closed") { triggersOf(d, event, any) shouldHaveSize 0 }
        }

        test("a kind-scoped trigger does not fire once a +1/+1 counter was already placed this turn") {
            val d = driver()
            val kind = d.putCreatureOnBattlefield(d.player1, "Kind Window Observer")
            val bears = d.putCreatureOnBattlefield(d.player1, "Grizzly Bears")
            triggersOf(d, placed(bears, anyFirst = false, kindFirst = false), kind) shouldHaveSize 0
        }

        test("the kind filter still applies: a first -1/-1 counter doesn't fire a +1/+1 trigger") {
            val d = driver()
            val kind = d.putCreatureOnBattlefield(d.player1, "Kind Window Observer")
            val bears = d.putCreatureOnBattlefield(d.player1, "Grizzly Bears")
            triggersOf(d, placed(bears, true, true, CounterType.MINUS_ONE_MINUS_ONE), kind) shouldHaveSize 0
        }
    }
})
