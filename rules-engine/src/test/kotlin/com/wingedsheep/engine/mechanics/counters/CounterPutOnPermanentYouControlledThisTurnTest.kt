package com.wingedsheep.engine.mechanics.counters

import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.handlers.effects.DamageUtils
import com.wingedsheep.engine.state.ComponentContainer
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.identity.OwnerComponent
import com.wingedsheep.engine.state.components.player.CountersPutOnYourPermanentsThisTurnComponent
import com.wingedsheep.sdk.core.CardType
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.TypeLine
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.CreatureStats
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.conditions.CounterPutOnPermanentYouControlledThisTurn
import com.wingedsheep.sdk.scripting.references.Player
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * The recipient-controller counter history behind `CounterPutOnPermanentYouControlledThisTurn`
 * ("if a +1/+1 counter was put on a permanent under your control this turn" — Fairgrounds
 * Trumpeter). Its ruling pins the axes tested here: the permanent had to be yours *as the counter
 * was placed*; who placed it is irrelevant; and it doesn't matter whether you still control the
 * permanent or whether it still has the counter.
 */
class CounterPutOnPermanentYouControlledThisTurnTest : FunSpec({

    val evaluator = PredicateEvaluator(cardRegistry = null)
    val you = EntityId.generate()
    val opponent = EntityId.generate()

    fun card(owner: EntityId, types: Set<CardType>): CardComponent = CardComponent(
        cardDefinitionId = "Test",
        name = "Test",
        manaCost = ManaCost(emptyList()),
        typeLine = TypeLine(cardTypes = types),
        ownerId = owner,
        baseStats = if (CardType.CREATURE in types) CreatureStats(2, 2) else null
    )

    fun container(controller: EntityId, types: Set<CardType>) = ComponentContainer()
        .with(card(controller, types))
        .with(OwnerComponent(controller))
        .with(ControllerComponent(controller))

    fun base(): GameState = GameState()
        .withEntity(you, ComponentContainer())
        .withEntity(opponent, ComponentContainer())

    fun GameState.onBattlefield(id: EntityId, controller: EntityId, types: Set<CardType>): GameState =
        withEntity(id, container(controller, types)).addToZone(ZoneKey(controller, Zone.BATTLEFIELD), id)

    fun GameState.holds(player: EntityId, kind: CounterType? = CounterType.PLUS_ONE_PLUS_ONE): Boolean =
        evaluator.conditions.evaluate(
            this,
            CounterPutOnPermanentYouControlledThisTurn(kind, Player.You),
            EffectContext(sourceId = null, controllerId = player)
        )

    test("false before any counter is placed") {
        base().holds(you) shouldBe false
    }

    test("keyed on the permanent's controller, not on who placed the counter") {
        val yours = EntityId.generate()
        val state = DamageUtils.markCounterPlacedOnCreature(
            base().onBattlefield(yours, you, setOf(CardType.CREATURE)),
            placerId = opponent,
            targetId = yours,
            counterType = CounterType.PLUS_ONE_PLUS_ONE
        )
        withClue("the opponent put a counter on your creature — it counts for you") {
            state.holds(you) shouldBe true
        }
        withClue("and not for the opponent, who placed it on a permanent they don't control") {
            state.holds(opponent) shouldBe false
        }
    }

    test("any permanent counts, not only creatures") {
        val artifact = EntityId.generate()
        val state = DamageUtils.markCounterPlacedOnCreature(
            base().onBattlefield(artifact, you, setOf(CardType.ARTIFACT)),
            placerId = you,
            targetId = artifact,
            counterType = CounterType.PLUS_ONE_PLUS_ONE
        )
        state.holds(you) shouldBe true
    }

    test("the recordCounterPlacement funnel feeds the same record") {
        val planeswalker = EntityId.generate()
        val (state, _) = DamageUtils.recordCounterPlacement(
            base().onBattlefield(planeswalker, you, setOf(CardType.PLANESWALKER)),
            planeswalker,
            CounterType.LOYALTY,
            placerId = you
        )
        state.holds(you, CounterType.LOYALTY) shouldBe true
    }

    test("the kind is scoped — a -1/-1 counter is not a +1/+1 counter, but satisfies the kind-agnostic reading") {
        val yours = EntityId.generate()
        val state = DamageUtils.markCounterPlacedOnCreature(
            base().onBattlefield(yours, you, setOf(CardType.CREATURE)),
            placerId = you,
            targetId = yours,
            counterType = CounterType.MINUS_ONE_MINUS_ONE
        )
        state.holds(you) shouldBe false
        state.holds(you, kind = null) shouldBe true
    }

    test("a permanent entering with counters (CR 122.6) counts for its base controller") {
        val entering = EntityId.generate()
        val (state, _) = DamageUtils.recordCounterPlacement(
            base().withEntity(entering, container(you, setOf(CardType.CREATURE))),
            entering,
            CounterType.PLUS_ONE_PLUS_ONE,
            byController = true
        )
        state.holds(you) shouldBe true
    }

    test("a card off the battlefield that is not entering is not a permanent and records nothing") {
        val suspended = EntityId.generate()
        val state = DamageUtils.markCounterOnControlledPermanent(
            base().withEntity(suspended, container(you, setOf(CardType.CREATURE))),
            suspended,
            CounterType.TIME
        )
        state.holds(you, kind = null) shouldBe false
    }

    test("survives the permanent leaving the battlefield — turn history, not a board scan") {
        val yours = EntityId.generate()
        val marked = DamageUtils.markCounterPlacedOnCreature(
            base().onBattlefield(yours, you, setOf(CardType.CREATURE)),
            placerId = you,
            targetId = yours,
            counterType = CounterType.PLUS_ONE_PLUS_ONE
        )
        val gone = marked.removeFromZone(ZoneKey(you, Zone.BATTLEFIELD), yours)
        gone.holds(you) shouldBe true
    }

    test("recording the same kind twice is idempotent") {
        val record = CountersPutOnYourPermanentsThisTurnComponent()
            .with(CounterType.PLUS_ONE_PLUS_ONE)
        record.with(CounterType.PLUS_ONE_PLUS_ONE) shouldBe record
    }
})
