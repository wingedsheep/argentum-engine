package com.wingedsheep.rulesengine.sets.ecl

import com.wingedsheep.rulesengine.ability.OnEnterBattlefield
import com.wingedsheep.rulesengine.card.CardDefinition
import com.wingedsheep.rulesengine.core.Keyword
import com.wingedsheep.rulesengine.core.ManaCost
import com.wingedsheep.rulesengine.ecs.EntityId
import com.wingedsheep.rulesengine.ecs.GameState
import com.wingedsheep.rulesengine.ecs.ZoneId
import com.wingedsheep.rulesengine.ecs.components.CardComponent
import com.wingedsheep.rulesengine.ecs.components.ControllerComponent
import com.wingedsheep.rulesengine.ecs.components.LifeComponent
import com.wingedsheep.rulesengine.ecs.event.ChosenTarget
import com.wingedsheep.rulesengine.ecs.script.EffectEvent
import com.wingedsheep.rulesengine.ecs.script.EffectExecutor
import com.wingedsheep.rulesengine.ecs.script.ExecutionContext
import com.wingedsheep.rulesengine.targeting.TargetCardInGraveyard
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

class RooftopPercherTest : FunSpec({

    val player1Id = EntityId.of("player1")
    val executor = EffectExecutor()

    val dummyCardDef = CardDefinition.creature(
        name = "Dummy",
        manaCost = ManaCost.parse("{1}"),
        subtypes = emptySet(),
        power = 1,
        toughness = 1
    )

    context("Rooftop Percher Script") {
        val script = LorwynEclipsedSet.getCardScript("Rooftop Percher")!!
        val definition = LorwynEclipsedSet.getCardDefinition("Rooftop Percher")!!

        test("has correct stats and keywords") {
            definition.creatureStats?.basePower shouldBe 3
            definition.creatureStats?.baseToughness shouldBe 3
            definition.keywords shouldContainExactlyInAnyOrder setOf(Keyword.FLYING, Keyword.CHANGELING)
        }

        test("defines correct target requirements") {
            script.targetRequirements shouldHaveSize 1
            val requirement = script.targetRequirements.first()

            requirement.shouldBeInstanceOf<TargetCardInGraveyard>()
            requirement.count shouldBe 2
            requirement.optional shouldBe true
        }

        test("executes ETB effect: Exile 2 targets + Gain 3 Life") {
            var state = GameState.newGame(listOf(player1Id to "Alice", EntityId.of("player2") to "Bob"))

            val (percherId, state1) = state.createEntity(
                EntityId.generate(),
                CardComponent(definition, player1Id),
                ControllerComponent(player1Id)
            )
            state = state1.addToZone(percherId, ZoneId.BATTLEFIELD)

            val graveyard = ZoneId.graveyard(player1Id)
            val (card1, state2) = state.createEntity(EntityId.generate(), CardComponent(dummyCardDef, player1Id))
            val (card2, state3) = state2.createEntity(EntityId.generate(), CardComponent(dummyCardDef, player1Id))
            state = state3.addToZone(card1, graveyard).addToZone(card2, graveyard)

            val chosenTargets = listOf(
                ChosenTarget.Card(card1, graveyard),
                ChosenTarget.Card(card2, graveyard)
            )

            val context = ExecutionContext(
                controllerId = player1Id,
                sourceId = percherId,
                targetsByIndex = mapOf(0 to chosenTargets)
            )

            val triggerAbility = script.triggeredAbilities.first { it.trigger is OnEnterBattlefield }

            val result = executor.execute(state, triggerAbility.effect, context)

            result.state.getZone(ZoneId.EXILE).contains(card1) shouldBe true
            result.state.getZone(ZoneId.EXILE).contains(card2) shouldBe true
            result.state.getZone(graveyard).isEmpty() shouldBe true
            result.state.getComponent<LifeComponent>(player1Id)?.life shouldBe 23

            result.events.any { it is EffectEvent.CardExiled && it.cardId == card1 } shouldBe true
            result.events.any { it is EffectEvent.CardExiled && it.cardId == card2 } shouldBe true
            result.events.any { it is EffectEvent.LifeGained && it.amount == 3 } shouldBe true
        }

        test("executes ETB effect: Exile 0 targets + Gain 3 Life") {
            var state = GameState.newGame(listOf(player1Id to "Alice", EntityId.of("player2") to "Bob"))
            val (percherId, state1) = state.createEntity(
                EntityId.generate(),
                CardComponent(definition, player1Id),
                ControllerComponent(player1Id)
            )
            state = state1.addToZone(percherId, ZoneId.BATTLEFIELD)

            val context = ExecutionContext(
                controllerId = player1Id,
                sourceId = percherId,
                targetsByIndex = mapOf(0 to emptyList())
            )

            val triggerAbility = script.triggeredAbilities.first { it.trigger is OnEnterBattlefield }

            val result = executor.execute(state, triggerAbility.effect, context)

            result.events.none { it is EffectEvent.CardExiled } shouldBe true
            result.state.getComponent<LifeComponent>(player1Id)?.life shouldBe 23
            result.events.any { it is EffectEvent.LifeGained && it.amount == 3 } shouldBe true
        }
    }
})
