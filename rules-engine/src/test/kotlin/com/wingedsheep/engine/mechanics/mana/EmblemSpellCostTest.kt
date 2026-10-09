package com.wingedsheep.engine.mechanics.mana

import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.handlers.effects.player.CreatePermanentEmblemExecutor
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.ComponentContainer
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.player.LifeLostAmountThisTurnComponent
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.CostModification
import com.wingedsheep.sdk.scripting.CostReductionSource
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifySpellCost
import com.wingedsheep.sdk.scripting.SpellCostTarget
import com.wingedsheep.sdk.scripting.effects.CreatePermanentEmblemEffect
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class EmblemSpellCostTest : FunSpec({
    val me = EntityId("player1")
    val opponent = EntityId("player2")
    val registry = CardRegistry()
    val calculator = CostCalculator(registry, PredicateEvaluator(cardRegistry = registry))
    val artifact = card("Test Colored Artifact") { manaCost = "{2}{U}{C}"; typeLine = "Artifact" }
    val creature = card("Test Nonartifact") { manaCost = "{2}{U}"; typeLine = "Creature — Bird"; power = 1; toughness = 1 }

    fun emblem(
        state: GameState = GameState(),
        controller: EntityId = me,
        target: SpellCostTarget = SpellCostTarget.YouCast(GameObjectFilter.Artifact),
        modification: CostModification = CostModification.ReduceGeneric(1),
    ): GameState = CreatePermanentEmblemExecutor().execute(
        state,
        Effects.CreatePermanentEmblem(
            ownedStaticAbilities = listOf(ModifySpellCost(target, modification)),
            emblemDescription = "Test cost modifier",
        ) as CreatePermanentEmblemEffect,
        EffectContext(sourceId = null, controllerId = controller),
    ).state

    test("emblem discounts only its controller's matching spells without a battlefield source") {
        val state = emblem()
        state.getBattlefield() shouldBe emptyList()
        calculator.calculateEffectiveCost(state, artifact, me) shouldBe ManaCost.parse("{1}{U}{C}")
        calculator.calculateEffectiveCost(state, artifact, opponent) shouldBe artifact.manaCost
        calculator.calculateEffectiveCost(state, creature, me) shouldBe creature.manaCost
    }

    test("each emblem stacks and reductions floor generic mana without touching colored or colorless pips") {
        val state = emblem(emblem(emblem()))
        calculator.calculateEffectiveCost(state, artifact, me) shouldBe ManaCost.parse("{U}{C}")
    }

    test("opponent's emblem is independent") {
        val state = emblem(emblem(), controller = opponent)
        calculator.calculateEffectiveCost(state, artifact, me) shouldBe ManaCost.parse("{1}{U}{C}")
        calculator.calculateEffectiveCost(state, artifact, opponent) shouldBe ManaCost.parse("{1}{U}{C}")
    }

    test("alternative costs receive emblem reductions") {
        calculator.calculateEffectiveCostWithAlternativeBase(emblem(), artifact, ManaCost.parse("{1}{R}"), me) shouldBe ManaCost.parse("{R}")
    }

    test("increases are applied before reductions even for an alternative base") {
        val state = emblem(emblem(), target = SpellCostTarget.AnyCaster(GameObjectFilter.Artifact),
            modification = CostModification.IncreaseGeneric(1))
        calculator.calculateEffectiveCostWithAlternativeBase(state, artifact, ManaCost.parse("{R}"), me) shouldBe ManaCost.parse("{R}")
    }

    test("opponent scoped and zone scoped emblem targets use emblem control") {
        val tax = emblem(target = SpellCostTarget.OpponentsCast(GameObjectFilter.Any),
            modification = CostModification.IncreaseGeneric(2))
        calculator.calculateEffectiveCost(tax, artifact, me) shouldBe artifact.manaCost
        calculator.calculateEffectiveCost(tax, artifact, opponent) shouldBe ManaCost.parse("{4}{U}{C}")
        val graveyard = emblem(target = SpellCostTarget.YouCastFromZones(setOf(Zone.GRAVEYARD), GameObjectFilter.Artifact))
        calculator.calculateEffectiveCost(graveyard, artifact, me, fromZone = Zone.GRAVEYARD) shouldBe ManaCost.parse("{1}{U}{C}")
        calculator.calculateEffectiveCost(graveyard, artifact, me, fromZone = Zone.HAND) shouldBe artifact.manaCost
    }

    test("dynamic amounts read the emblem controller even when discounting another player's spell") {
        val state = GameState()
            .withEntity(me, ComponentContainer.EMPTY.with(
                LifeLostAmountThisTurnComponent(1)))
            .withEntity(opponent, ComponentContainer.EMPTY.with(
                LifeLostAmountThisTurnComponent(2)))
        val discount = emblem(state, target = SpellCostTarget.AnyCaster(GameObjectFilter.Artifact),
            modification = CostModification.ReduceGenericBy(CostReductionSource.Dynamic(
                DynamicAmounts.lifeLostThisTurn())))
        calculator.calculateEffectiveCost(discount, artifact, opponent) shouldBe ManaCost.parse("{1}{U}{C}")
    }

    test("face-down discounts also use the emblem's fixed controller") {
        val state = emblem(target = SpellCostTarget.FaceDownYouCast)
        calculator.calculateFaceDownCost(state, me) shouldBe ManaCost.parse("{2}")
        calculator.calculateFaceDownCost(state, opponent) shouldBe ManaCost.parse("{3}")
    }

    test("emblem remains active across turn changes") {
        val state = emblem().copy(turnNumber = 5)
        calculator.calculateEffectiveCost(state, artifact, me) shouldBe ManaCost.parse("{1}{U}{C}")
    }
})
