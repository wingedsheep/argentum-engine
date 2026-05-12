package com.wingedsheep.engine.filters

import com.wingedsheep.engine.handlers.filters.UntappedCreaturesAndOrTreasuresFilter
import com.wingedsheep.engine.state.Component
import com.wingedsheep.engine.state.ComponentContainer
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.identity.OwnerComponent
import com.wingedsheep.sdk.core.CardType
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.TypeLine
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.CreatureStats
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder

/**
 * BDD specification for the untapped-creatures-and/or-Treasures filter primitive.
 *
 * The filter selects battlefield permanents that are:
 *   - untapped, AND
 *   - (a creature OR a permanent with the Treasure subtype), AND
 *   - controlled by the querying player
 */
class FilterSpanningMultipleTypesCreaturesAndOrTreasuresTest : FunSpec({

    val filter = UntappedCreaturesAndOrTreasuresFilter()
    val activePlayer = EntityId.generate()
    val opponent = EntityId.generate()

    fun creature(owner: EntityId): CardComponent = CardComponent(
        cardDefinitionId = "Test Creature",
        name = "Test Creature",
        manaCost = ManaCost(emptyList()),
        typeLine = TypeLine(cardTypes = setOf(CardType.CREATURE)),
        ownerId = owner,
        baseStats = CreatureStats(2, 2)
    )

    fun treasureToken(owner: EntityId): CardComponent = CardComponent(
        cardDefinitionId = "Treasure",
        name = "Treasure",
        manaCost = ManaCost(emptyList()),
        typeLine = TypeLine(
            cardTypes = setOf(CardType.ARTIFACT),
            subtypes = setOf(Subtype("Treasure"))
        ),
        ownerId = owner
    )

    fun plains(owner: EntityId): CardComponent = CardComponent(
        cardDefinitionId = "Plains",
        name = "Plains",
        manaCost = ManaCost(emptyList()),
        typeLine = TypeLine(cardTypes = setOf(CardType.LAND)),
        ownerId = owner
    )

    fun container(
        controller: EntityId,
        card: CardComponent,
        vararg extras: Component
    ): ComponentContainer {
        var c = ComponentContainer()
            .with(card)
            .with(OwnerComponent(controller))
            .with(ControllerComponent(controller))
        extras.forEach { component ->
            @Suppress("UNCHECKED_CAST")
            c = c.copy(components = c.components + (component::class.qualifiedName!! to component))
        }
        return c
    }

    fun buildState(entities: List<Pair<EntityId, ComponentContainer>>): GameState {
        var state = GameState()
            .withEntity(activePlayer, ComponentContainer())
            .withEntity(opponent, ComponentContainer())
        entities.forEach { (id, cont) ->
            state = state.withEntity(id, cont)
            val controller = cont.get<ControllerComponent>()?.playerId ?: activePlayer
            state = state.addToZone(ZoneKey(controller, Zone.BATTLEFIELD), id)
        }
        return state
    }

    test(
        "filter returns exactly the active player's untapped creature and untapped Treasure, " +
        "excluding tapped permanents, opponent permanents, and non-creature non-Treasure permanents"
    ) {
        // GIVEN
        val yourUntappedCreature = EntityId.generate()
        val yourTappedCreature = EntityId.generate()
        val yourUntappedTreasure = EntityId.generate()
        val yourTappedTreasure = EntityId.generate()
        val yourUntappedPlains = EntityId.generate()
        val opponentUntappedCreature = EntityId.generate()
        val opponentUntappedTreasure = EntityId.generate()

        val state = buildState(
            listOf(
                yourUntappedCreature to container(activePlayer, creature(activePlayer)),
                yourTappedCreature to container(activePlayer, creature(activePlayer), TappedComponent),
                yourUntappedTreasure to container(activePlayer, treasureToken(activePlayer)),
                yourTappedTreasure to container(activePlayer, treasureToken(activePlayer), TappedComponent),
                yourUntappedPlains to container(activePlayer, plains(activePlayer)),
                opponentUntappedCreature to container(opponent, creature(opponent)),
                opponentUntappedTreasure to container(opponent, treasureToken(opponent))
            )
        )

        // WHEN
        val result = filter.findMatching(state, activePlayer)

        // THEN
        result shouldContainExactlyInAnyOrder listOf(yourUntappedCreature, yourUntappedTreasure)
    }
})
