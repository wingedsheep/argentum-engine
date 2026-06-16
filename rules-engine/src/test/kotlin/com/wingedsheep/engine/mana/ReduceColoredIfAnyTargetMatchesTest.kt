package com.wingedsheep.engine.mana

import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.mechanics.mana.CostCalculator
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.ComponentContainer
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.OwnerComponent
import com.wingedsheep.sdk.core.CardType
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.TypeLine
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Exercises [com.wingedsheep.sdk.scripting.CostModification.ReduceColoredIfAnyTargetMatches] — the
 * target-gated colored cost reduction added for Brush Off ("This spell costs {1}{U} less to cast if
 * it targets an instant or sorcery spell. Counter target spell.").
 *
 * Brush Off pairs the new colored `{U}` reduction with a generic `ReduceGenericBy(
 * FixedIfAnyTargetMatches(1, InstantOrSorcery))` for the `{1}`, both gated on the same filter, so
 * the combined reduction is exactly `{1}{U}` once a matching target is chosen.
 */
class ReduceColoredIfAnyTargetMatchesTest : FunSpec({

    val casterId = EntityId.generate()

    // Real registered Brush Off, so this test pins the card's own static abilities.
    val registry = CardRegistry().apply { register(TestCards.all) }
    val brushOff: CardDefinition = registry.getCard("Brush Off")
        ?: error("Brush Off not registered")
    val calculator = CostCalculator(registry, PredicateEvaluator())

    fun stackSpell(type: CardType, name: String): Pair<EntityId, ComponentContainer> {
        val id = EntityId.generate()
        val container = ComponentContainer.of(
            CardComponent(
                cardDefinitionId = name,
                name = name,
                manaCost = ManaCost.parse("{1}"),
                typeLine = TypeLine(cardTypes = setOf(type)),
                oracleText = "",
                colors = setOf(Color.BLUE),
                ownerId = casterId,
                spellEffect = null,
            ),
            OwnerComponent(casterId),
        )
        return id to container
    }

    fun baseState(spellId: EntityId, container: ComponentContainer): GameState =
        GameState()
            .withEntity(casterId, ComponentContainer.EMPTY)
            .withEntity(spellId, container)
            .addToZone(ZoneKey(casterId, Zone.STACK), spellId)
            .copy(turnOrder = listOf(casterId))

    test("Brush Off base cost is {2}{U}{U} with no chosen target") {
        val (spellId, container) = stackSpell(CardType.INSTANT, "Some Instant")
        val state = baseState(spellId, container)
        val cost = calculator.calculateEffectiveCost(state, brushOff, casterId, chosenTargets = emptyList())
        cost.toString() shouldBe ManaCost.parse("{2}{U}{U}").toString()
    }

    test("targeting an instant spell reduces Brush Off by {1}{U} to {1}{U}") {
        val (spellId, container) = stackSpell(CardType.INSTANT, "Some Instant")
        val state = baseState(spellId, container)
        val cost = calculator.calculateEffectiveCost(state, brushOff, casterId, chosenTargets = listOf(spellId))
        cost.toString() shouldBe ManaCost.parse("{1}{U}").toString()
    }

    test("targeting a sorcery spell also reduces Brush Off by {1}{U}") {
        val (spellId, container) = stackSpell(CardType.SORCERY, "Some Sorcery")
        val state = baseState(spellId, container)
        val cost = calculator.calculateEffectiveCost(state, brushOff, casterId, chosenTargets = listOf(spellId))
        cost.toString() shouldBe ManaCost.parse("{1}{U}").toString()
    }

    test("targeting a non-instant/sorcery spell (creature) leaves Brush Off at full cost") {
        val (spellId, container) = stackSpell(CardType.CREATURE, "Some Creature")
        val state = baseState(spellId, container)
        val cost = calculator.calculateEffectiveCost(state, brushOff, casterId, chosenTargets = listOf(spellId))
        cost.toString() shouldBe ManaCost.parse("{2}{U}{U}").toString()
    }
})
