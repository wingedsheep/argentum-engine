package com.wingedsheep.engine.handlers.effects

import com.wingedsheep.engine.core.ZoneChangeEvent
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.state.ComponentContainer
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.OwnerComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.sdk.core.CardType
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.TypeLine
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.CreatureStats
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.effects.ReturnTargetCreatureCardFromGraveyardToHandEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe

class ReturnTargetCreatureCardFromGraveyardToHandTest : FunSpec({

    val handler = ReturnTargetCreatureCardFromGraveyardToHandHandler()

    val controllerId = EntityId.generate()
    val opponentId = EntityId.generate()

    fun creatureCard(ownerId: EntityId) = CardComponent(
        cardDefinitionId = "Test Creature",
        name = "Test Creature",
        manaCost = ManaCost(emptyList()),
        typeLine = TypeLine(cardTypes = setOf(CardType.CREATURE)),
        baseStats = CreatureStats(2, 2),
        ownerId = ownerId
    )

    fun sorceryCard(ownerId: EntityId) = CardComponent(
        cardDefinitionId = "Test Sorcery",
        name = "Test Sorcery",
        manaCost = ManaCost(emptyList()),
        typeLine = TypeLine(cardTypes = setOf(CardType.SORCERY)),
        ownerId = ownerId
    )

    test("targeted creature card moves from controller's graveyard to their hand") {
        // GIVEN: a creature card and a non-creature card in the controller's graveyard
        val creatureId = EntityId.generate()
        val sorceryId = EntityId.generate()

        val state = GameState()
            .withEntity(controllerId, ComponentContainer())
            .withEntity(opponentId, ComponentContainer())
            .withEntity(creatureId, ComponentContainer()
                .with(creatureCard(controllerId))
                .with(OwnerComponent(controllerId)))
            .withEntity(sorceryId, ComponentContainer()
                .with(sorceryCard(controllerId))
                .with(OwnerComponent(controllerId)))
            .addToZone(ZoneKey(controllerId, Zone.GRAVEYARD), creatureId)
            .addToZone(ZoneKey(controllerId, Zone.GRAVEYARD), sorceryId)

        // AND: the chosen target is the creature card (a legal target — creature card in controller's graveyard)
        val effect = ReturnTargetCreatureCardFromGraveyardToHandEffect(
            target = EffectTarget.ContextTarget(0)
        )
        val context = EffectContext(
            sourceId = null,
            controllerId = controllerId,
            opponentId = opponentId,
            targets = listOf(ChosenTarget.Card(creatureId, controllerId, Zone.GRAVEYARD))
        )

        // WHEN: the effect resolves
        val result = handler.execute(state, effect, context)

        // THEN: effect succeeds
        result.isSuccess shouldBe true

        // AND: the targeted creature card is now in the controller's hand
        result.state.getZone(ZoneKey(controllerId, Zone.HAND)) shouldContain creatureId

        // AND: the targeted creature card is no longer in the controller's graveyard
        result.state.getZone(ZoneKey(controllerId, Zone.GRAVEYARD)) shouldNotContain creatureId

        // AND: the non-creature card remains in the graveyard (it was never a legal target)
        result.state.getZone(ZoneKey(controllerId, Zone.GRAVEYARD)) shouldContain sorceryId

        // AND: a single zone-change event is emitted for the creature card from GRAVEYARD to HAND
        val zoneChangeEvents = result.events.filterIsInstance<ZoneChangeEvent>()
        zoneChangeEvents shouldHaveSize 1
        zoneChangeEvents.first().entityId shouldBe creatureId
        zoneChangeEvents.first().fromZone shouldBe Zone.GRAVEYARD
        zoneChangeEvents.first().toZone shouldBe Zone.HAND
        zoneChangeEvents.first().ownerId shouldBe controllerId
    }
})
