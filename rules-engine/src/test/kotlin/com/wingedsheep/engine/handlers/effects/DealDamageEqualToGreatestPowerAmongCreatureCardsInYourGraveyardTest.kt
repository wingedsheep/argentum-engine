package com.wingedsheep.engine.handlers.effects

import com.wingedsheep.engine.core.DamageDealtEvent
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.state.ComponentContainer
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.battlefield.DamageComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.identity.OwnerComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.sdk.core.CardType
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.TypeLine
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.CreatureStats
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.effects.DealDamageEqualToGreatestPowerAmongCreatureCardsInYourGraveyardEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe

class DealDamageEqualToGreatestPowerAmongCreatureCardsInYourGraveyardTest : FunSpec({

    val handler = DealDamageEqualToGreatestPowerAmongCreatureCardsInYourGraveyardHandler()

    val activePlayerId = EntityId.generate()
    val opponentId = EntityId.generate()

    fun creatureCard(power: Int, toughness: Int, ownerId: EntityId) = CardComponent(
        cardDefinitionId = "Test Creature $power/$toughness",
        name = "Test Creature $power/$toughness",
        manaCost = ManaCost(emptyList()),
        typeLine = TypeLine(cardTypes = setOf(CardType.CREATURE)),
        baseStats = CreatureStats(power, toughness),
        ownerId = ownerId
    )

    fun instantCard(ownerId: EntityId) = CardComponent(
        cardDefinitionId = "Test Instant",
        name = "Test Instant",
        manaCost = ManaCost(emptyList()),
        typeLine = TypeLine(cardTypes = setOf(CardType.INSTANT)),
        ownerId = ownerId
    )

    test("damage equals greatest power among creature cards in controller graveyard, ignoring non-creature cards and opponent graveyard") {
        // GIVEN: three creature cards (powers 2, 5, 3) and one instant in the active player's graveyard
        val creature2 = EntityId.generate()
        val creature5 = EntityId.generate()
        val creature3 = EntityId.generate()
        val instant = EntityId.generate()
        // AND: an opponent creature on the battlefield with toughness 6 and 0 marked damage
        val targetCreature = EntityId.generate()

        val state = GameState()
            .withEntity(activePlayerId, ComponentContainer())
            .withEntity(opponentId, ComponentContainer())
            .withEntity(creature2, ComponentContainer().with(creatureCard(2, 2, activePlayerId)))
            .withEntity(creature5, ComponentContainer().with(creatureCard(5, 5, activePlayerId)))
            .withEntity(creature3, ComponentContainer().with(creatureCard(3, 3, activePlayerId)))
            .withEntity(instant, ComponentContainer().with(instantCard(activePlayerId)))
            .withEntity(
                targetCreature,
                ComponentContainer()
                    .with(creatureCard(3, 6, opponentId))
                    .with(OwnerComponent(opponentId))
                    .with(ControllerComponent(opponentId))
            )
            .addToZone(ZoneKey(activePlayerId, Zone.GRAVEYARD), creature2)
            .addToZone(ZoneKey(activePlayerId, Zone.GRAVEYARD), creature5)
            .addToZone(ZoneKey(activePlayerId, Zone.GRAVEYARD), creature3)
            .addToZone(ZoneKey(activePlayerId, Zone.GRAVEYARD), instant)
            .addToZone(ZoneKey(opponentId, Zone.BATTLEFIELD), targetCreature)

        // AND: an effect on the stack reading "deal damage equal to the greatest power among creature cards in your graveyard"
        val effect = DealDamageEqualToGreatestPowerAmongCreatureCardsInYourGraveyardEffect(
            target = EffectTarget.ContextTarget(0)
        )
        val context = EffectContext(
            sourceId = null,
            controllerId = activePlayerId,
            opponentId = opponentId,
            targets = listOf(ChosenTarget.Permanent(targetCreature))
        )

        // WHEN: the effect resolves
        val result = handler.execute(state, effect, context)

        // THEN: the target creature has 5 marked damage (greatest power among only the creature cards)
        result.isSuccess shouldBe true
        result.state.getEntity(targetCreature)!!.get<DamageComponent>()!!.amount shouldBe 5

        // AND: a single damage event is emitted carrying amount=5 and the target creature's entity id
        val damageEvents = result.events.filterIsInstance<DamageDealtEvent>()
        damageEvents shouldHaveSize 1
        damageEvents.first().amount shouldBe 5
        damageEvents.first().targetId shouldBe targetCreature
    }
})
