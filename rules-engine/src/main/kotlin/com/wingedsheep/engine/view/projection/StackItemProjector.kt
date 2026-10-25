package com.wingedsheep.engine.view.projection

import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.ActivatedAbilityOnStackComponent
import com.wingedsheep.engine.state.components.stack.TargetsComponent
import com.wingedsheep.engine.state.components.stack.TriggeredAbilityOnStackComponent
import com.wingedsheep.engine.state.nameVisibleToAll
import com.wingedsheep.engine.view.ClientAbilityIdentity
import com.wingedsheep.engine.view.ClientCard
import com.wingedsheep.engine.view.ClientPerModeTargetGroup
import com.wingedsheep.engine.view.Visibility
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.effects.ModalEffect

/**
 * Projects an activated or triggered ability on the stack into a [ClientCard]. These have no
 * [CardComponent] of their own, so the card is synthetic: the source's name, colours and art, the
 * ability's rendered text, and its targets.
 */
internal class StackItemProjector(
    private val cardRegistry: CardRegistry,
    private val visibility: Visibility,
    private val stackText: StackTextRenderer,
) {

    /** The ability on the stack at [entityId], or null when it is neither kind of ability. */
    fun project(
        state: GameState,
        entityId: EntityId,
        viewingPlayerId: EntityId,
        isSpectator: Boolean
    ): ClientCard? {
        val container = state.getEntity(entityId) ?: return null
        container.get<ActivatedAbilityOnStackComponent>()?.let { activated ->
            return projectActivated(state, entityId, activated, container.get<TargetsComponent>(), viewingPlayerId, isSpectator)
        }
        container.get<TriggeredAbilityOnStackComponent>()?.let { triggered ->
            return projectTriggered(state, entityId, triggered, container.get<TargetsComponent>(), viewingPlayerId, isSpectator)
        }
        return null
    }

    private fun projectActivated(
        state: GameState,
        entityId: EntityId,
        activatedAbility: ActivatedAbilityOnStackComponent,
        targetsComponent: TargetsComponent?,
        viewingPlayerId: EntityId,
        isSpectator: Boolean
    ): ClientCard {
        // Get the source card's info to display
        val identityVisible = sourceIdentityVisible(state, activatedAbility.sourceId, viewingPlayerId, isSpectator)
        val sourceCard = state.getEntity(activatedAbility.sourceId)?.get<CardComponent>()
            ?.takeIf { identityVisible }
        val sourceName = nameVisibleToAll(state, activatedAbility.sourceId, activatedAbility.sourceName)
        val cardDef = cardRegistry.getCard(sourceName).takeIf { identityVisible }

        return ClientCard(
            id = entityId,
            name = "$sourceName ability",
            manaCost = "",
            manaValue = 0,
            typeLine = "Ability",
            cardTypes = setOf("Ability"),
            subtypes = emptySet(),
            colors = sourceColors(state, activatedAbility.sourceId, sourceCard),
            oracleText = activatedAbility.descriptionOverride
                ?: stackText.runtimeAbilityText(state, entityId, activatedAbility)
                ?: StackTextRenderer.effectDisplayText(
                    activatedAbility.effect, StackTextRenderer.selfNounFor(state, activatedAbility.sourceId)
                ),
            power = null,
            toughness = null,
            basePower = null,
            baseToughness = null,
            damage = null,
            keywords = emptySet(),
            counters = emptyMap(),
            isTapped = false,
            hasSummoningSickness = false,
            isTransformed = false,
            isAttacking = false,
            isBlocking = false,
            attackingTarget = null,
            blockingTarget = null,
            controllerId = activatedAbility.controllerId,
            ownerId = activatedAbility.controllerId,
            isToken = false,
            zone = ZoneKey(activatedAbility.controllerId, Zone.STACK),
            attachedTo = null,
            attachments = emptyList(),
            isFaceDown = false,
            targets = StackTextRenderer.toClientTargets(targetsComponent),
            imageUri = sourceCard?.imageUri ?: cardDef?.metadata?.imageUri,
            chosenX = activatedAbility.xValue,
            chosenModeDescriptions = (activatedAbility.effect as? com.wingedsheep.sdk.scripting.effects.ModalEffect)?.let { modal ->
                activatedAbility.chosenModes.map { modal.modes[it].description }
            } ?: emptyList(),
            perModeTargets = (activatedAbility.effect as? com.wingedsheep.sdk.scripting.effects.ModalEffect)?.let { modal ->
                stackText.perModeTargetGroups(state, activatedAbility.chosenModes, activatedAbility.modeTargetsOrdered,
                    activatedAbility.chosenModes.map { modal.modes[it].description }, viewingPlayerId, isSpectator)
            } ?: emptyList(),
            abilityIdentity = activatedAbility.abilityIdentity?.takeIf { identityVisible }?.let {
                ClientAbilityIdentity(it.cardDefinitionId, it.abilityId.value)
            }
        )
    }

    /** A room ability's source is a dungeon card (CR 309.4c), which has no [CardComponent]. */
    private fun dungeonImage(state: GameState, sourceId: EntityId): String? =
        state.getEntity(sourceId)
            ?.get<com.wingedsheep.engine.state.components.identity.DungeonCardComponent>()
            ?.let { com.wingedsheep.sdk.dungeons.Dungeons.byName(it.dungeon)?.imageUri }

    private fun projectTriggered(
        state: GameState,
        entityId: EntityId,
        triggeredAbility: TriggeredAbilityOnStackComponent,
        targetsComponent: TargetsComponent?,
        viewingPlayerId: EntityId,
        isSpectator: Boolean
    ): ClientCard {
        val identityVisible = sourceIdentityVisible(state, triggeredAbility.sourceId, viewingPlayerId, isSpectator)
        val sourceCard = state.getEntity(triggeredAbility.sourceId)?.get<CardComponent>()
            ?.takeIf { identityVisible }
        val sourceName = nameVisibleToAll(state, triggeredAbility.sourceId, triggeredAbility.sourceName)
        val cardDef = cardRegistry.getCard(sourceName).takeIf { identityVisible }

        // Triggering entity ID for visual source arrow (separate from targeting arrows)
        val triggeringId = triggeredAbility.triggerContext?.triggeringEntityId?.takeIf { id ->
            state.getBattlefield().contains(id)
        }

        // Find the source entity's current zone (for graveyard trigger styling)
        val sourceZone = findEntityZone(state, triggeredAbility.sourceId)

        // Modal-copy breakdown: spell copies carry the original's chosenModes (700.2g) so the
        // opponent can see the same per-mode text and target names on the copy.
        val triggeredModal = triggeredAbility.effect as? ModalEffect
        val triggeredModeDescriptions: List<String> =
            if (triggeredModal != null && triggeredAbility.chosenModes.isNotEmpty()) {
                stackText.chosenModeDescriptions(state, entityId, triggeredAbility, triggeredModal)
            } else emptyList()
        val triggeredPerModeTargets: List<ClientPerModeTargetGroup> =
            if (triggeredAbility.chosenModes.isNotEmpty()) {
                stackText.perModeTargetGroups(
                    state,
                    triggeredAbility.chosenModes,
                    triggeredAbility.modeTargetsOrdered,
                    triggeredModeDescriptions,
                    viewingPlayerId,
                    isSpectator
                )
            } else emptyList()

        return ClientCard(
            id = entityId,
            name = "$sourceName trigger",
            manaCost = "",
            manaValue = 0,
            typeLine = "Triggered Ability",
            cardTypes = setOf("Ability"),
            subtypes = emptySet(),
            colors = sourceColors(state, triggeredAbility.sourceId, sourceCard),
            oracleText = triggeredAbility.descriptionOverride
                ?: stackText.runtimeAbilityText(state, entityId, triggeredAbility)
                ?: StackTextRenderer.effectDisplayText(
                    triggeredAbility.effect, StackTextRenderer.selfNounFor(state, triggeredAbility.sourceId)
                ),
            power = null,
            toughness = null,
            basePower = null,
            baseToughness = null,
            damage = null,
            keywords = emptySet(),
            counters = emptyMap(),
            isTapped = false,
            hasSummoningSickness = false,
            isTransformed = false,
            isAttacking = false,
            isBlocking = false,
            attackingTarget = null,
            blockingTarget = null,
            controllerId = triggeredAbility.controllerId,
            ownerId = triggeredAbility.controllerId,
            isToken = false,
            zone = ZoneKey(triggeredAbility.controllerId, Zone.STACK),
            attachedTo = null,
            attachments = emptyList(),
            isFaceDown = false,
            targets = StackTextRenderer.toClientTargets(targetsComponent),
            triggeringEntityId = triggeringId,
            imageUri = sourceCard?.imageUri ?: cardDef?.metadata?.imageUri
                ?: dungeonImage(state, triggeredAbility.sourceId),
            sourceZone = sourceZone,
            chosenX = triggeredAbility.xValue,
            abilityIdentity = triggeredAbility.abilityIdentity?.takeIf { identityVisible }?.let {
                ClientAbilityIdentity(it.cardDefinitionId, it.abilityId.value)
            },
            copyIndex = triggeredAbility.copyIndex,
            copyTotal = triggeredAbility.copyTotal,
            chosenModeDescriptions = triggeredModeDescriptions,
            perModeTargets = triggeredPerModeTargets
        )
    }

    /** A public ability must not expose the hidden card underneath its face-down source. */
    private fun sourceIdentityVisible(
        state: GameState,
        sourceId: EntityId,
        viewingPlayerId: EntityId,
        isSpectator: Boolean
    ): Boolean {
        val zone = if (sourceId in state.stack) Zone.STACK
            else findEntityZone(state, sourceId)?.let(Zone::valueOf) ?: return true
        return visibility.isCardIdentityVisibleTo(state, zone, sourceId, viewingPlayerId, isSpectator)
    }

    /** The source's colours — projected on the battlefield (Rule 613), printed anywhere else. */
    private fun sourceColors(state: GameState, sourceId: EntityId, visibleCard: CardComponent?): Set<Color> =
        if (sourceId in state.getBattlefield()) {
            state.projectedState.getColors(sourceId).mapNotNull { name ->
                Color.entries.firstOrNull { it.name == name }
            }.toSet()
        } else visibleCard?.let { card ->
            // An off-battlefield color effect (Mycosynth Lattice) overrides the shown printed colors.
            state.projectedState.offBattlefieldColors(sourceId)
                ?.mapNotNullTo(linkedSetOf()) { name -> Color.entries.firstOrNull { it.name == name } }
                ?: card.colors
        }.orEmpty()

    /**
     * Find which zone an entity is currently in.
     * Returns the zone type name (e.g., "GRAVEYARD") or null if not found.
     */
    private fun findEntityZone(state: GameState, entityId: EntityId): String? {
        for ((zoneKey, entities) in state.zones) {
            if (entityId in entities) {
                return zoneKey.zoneType.name
            }
        }
        return null
    }
}
