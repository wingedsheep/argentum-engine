package com.wingedsheep.engine.handlers.effects
import com.wingedsheep.engine.state.components.battlefield.chosenCreatureRef
import com.wingedsheep.engine.state.components.battlefield.chosenOpponent

import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.identity.OwnerComponent
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.state.components.battlefield.LastKnownPermanentComponent
import com.wingedsheep.engine.state.components.combat.AttackingComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.state.components.stack.SpellOnStackComponent
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.values.EntityReference

/**
 * Utility functions for resolving effect targets from symbolic references to concrete entity IDs.
 *
 * Targets in MTG are late-bound: effects reference targets symbolically
 * (e.g., ContextTarget(0) = "the first target chosen at cast time") and these
 * are resolved at execution time against the current game state.
 */
object TargetResolutionUtils {

    /**
     * Resolve a target from the effect target definition and context.
     */
    fun resolveTarget(effectTarget: EffectTarget, context: EffectContext): EntityId? {
        return when (effectTarget) {
            is EffectTarget.Self -> context.pipeline.iterationTarget ?: context.sourceId
            is EffectTarget.GrantingSource -> context.granterId
            is EffectTarget.Controller -> context.controllerId
            is EffectTarget.ContextTarget -> context.positionalTarget(effectTarget.index)?.toEntityId()
            is EffectTarget.BoundVariable -> context.pipeline.namedTargets[effectTarget.name]?.toEntityId()
            is EffectTarget.SpecificEntity -> effectTarget.entityId
            is EffectTarget.TriggeringEntity -> context.triggeringEntityId
            is EffectTarget.DiscardedAsCost ->
                context.discardedAsCostCards.getOrNull(effectTarget.index)
            is EffectTarget.PipelineTarget ->
                context.pipeline.storedCollections[effectTarget.collectionName]?.getOrNull(effectTarget.index)
            else -> null
        }
    }

    /**
     * Resolve a target with access to game state (for targets like EnchantedCreature
     * that need to look up attachment relationships).
     */
    fun resolveTarget(effectTarget: EffectTarget, context: EffectContext, state: GameState): EntityId? {
        if (effectTarget is EffectTarget.EnchantedCreature ||
            effectTarget is EffectTarget.EquippedCreature ||
            effectTarget is EffectTarget.EnchantedPermanent
        ) {
            val sourceId = context.sourceId ?: return null
            return state.getEntity(sourceId)?.get<AttachedToComponent>()?.targetId
        }
        if (effectTarget is EffectTarget.ChosenCreature) {
            val sourceId = context.sourceId ?: return null
            return state.getEntity(sourceId)?.chosenCreatureRef()
        }
        if (effectTarget is EffectTarget.TargetController) {
            val targetEntity = context.targets.firstOrNull()?.toEntityId() ?: return null
            return controllerOf(state, targetEntity)
        }
        if (effectTarget is EffectTarget.ControllerOfTriggeringEntity) {
            val triggerId = context.triggeringEntityId ?: return null
            val entity = state.getEntity(triggerId) ?: return null
            state.projectedState.getController(triggerId)?.let { return it }
            entity.get<ControllerComponent>()?.playerId?.let { return it }
            // An activated ability's stack entity is a bare container with no
            // ControllerComponent. "That artifact's controller" (Haunting Wind, Artifact
            // Possession) means the controller of the ability's SOURCE permanent — fall
            // through to it, or to the ability's own controller as last-known information
            // if the source has left the battlefield.
            entity.get<com.wingedsheep.engine.state.components.stack.ActivatedAbilityOnStackComponent>()?.let { ability ->
                return controllerOf(state, ability.sourceId) ?: ability.controllerId
            }
            // The triggering permanent may itself have left the battlefield: last-known
            // controller (CR 608.2h) before the owner.
            entity.get<LastKnownPermanentComponent>()?.snapshot?.controllerId?.let { return it }
            return entity.get<CardComponent>()?.ownerId
        }
        if (effectTarget is EffectTarget.AttachedToTriggeringPermanent) {
            // "Becomes unattached": the host recorded when the trigger fired is the only right
            // answer — the live link is by now either gone or, if the unattach was caused by
            // equipping the attachment elsewhere, pointing at the *new* host. Scoped to the
            // battlefield so a former host that has itself left resolves to nothing, which is
            // Stitcher's Graft's "the triggered ability won't do anything in that case".
            context.triggerUnattachedFromEntityId?.let {
                return it.takeIf { id -> id in state.getBattlefield() }
            }
            // "Becomes attached": the triggering entity is the attachment, and the host is its
            // current attachment target. Reading it live means a "for as long as attached" payoff
            // does nothing if the attachment has already moved or left (CR 611.2b) — what Eriette
            // and Assimilation Aegis want.
            val attachmentId = context.triggeringEntityId ?: return null
            return state.getEntity(attachmentId)?.get<AttachedToComponent>()?.targetId
        }
        if (effectTarget is EffectTarget.ControllerOfPipelineTarget) {
            val targetEntityId = context.pipeline.storedCollections[effectTarget.collectionName]?.getOrNull(effectTarget.index) ?: return null
            return controllerOf(state, targetEntityId)
        }
        if (effectTarget is EffectTarget.PipelineTarget) {
            return context.pipeline.storedCollections[effectTarget.collectionName]?.getOrNull(effectTarget.index)
        }
        return resolveTarget(effectTarget, context)
    }

    /**
     * The first chosen target that is a player. "Target player" / "target opponent"
     * references resolve through the bound targets — never through turn order.
     */
    private fun firstPlayerTarget(context: EffectContext): EntityId? =
        context.targets.firstOrNull { it is ChosenTarget.Player }?.toEntityId()
            ?: context.targets.firstOrNull()?.toEntityId()

    /**
     * The defending player for the ability's source, per CR 802.2a: an explicitly bound
     * defender (attack-declaration legality checks bind [EffectContext.defendingPlayerId]
     * before the attacker has an `AttackingComponent`), else read from the source's attack
     * assignment (a creature attacking a planeswalker defends against that planeswalker's
     * controller). When the source has already left combat (e.g. it died dealing combat
     * damage), the trigger event's player is last-known information for "deals combat
     * damage to a player" triggers.
     */
    fun resolveDefendingPlayer(context: EffectContext, state: GameState): EntityId? {
        context.defendingPlayerId?.let { return it }
        val defenderId = context.sourceId
            ?.let { state.getEntity(it)?.get<AttackingComponent>()?.defenderId }
        if (defenderId != null) {
            return if (defenderId in state.turnOrder) defenderId
            else state.getEntity(defenderId)?.get<ControllerComponent>()?.playerId
        }
        return (context.triggeringPlayerId ?: context.triggeringEntityId)
            ?.takeIf { it in state.turnOrder }
    }

    /**
     * Central single-player resolution for a [Player] reference. Every executor that
     * maps a `Player` to one concrete player id goes through here — per-executor copies
     * of this switch are what made `EffectContext.opponentId` so hard to kill.
     *
     * Multi-player references ([Player.Each], [Player.EachOpponent],
     * [Player.ActivePlayerFirst]) return `null`: they have no single-player meaning and
     * must be resolved through [resolvePlayerTargets] / an iteration.
     */
    fun resolvePlayerRef(player: Player, context: EffectContext, state: GameState): EntityId? {
        return when (player) {
            Player.You -> context.controllerId
            Player.TargetPlayer, Player.TargetOpponent, Player.Any -> firstPlayerTarget(context)
            is Player.ContextPlayer -> context.positionalTarget(player.index)?.toEntityId()
            Player.TriggeringPlayer -> context.triggeringPlayerId ?: context.triggeringEntityId
            Player.Candidate -> context.candidatePlayerId
            Player.AnOpponent -> state.getOpponents(context.controllerId).firstOrNull()
            Player.DefendingPlayer -> resolveDefendingPlayer(context, state)
            Player.ChosenOpponent -> context.sourceId?.let { state.getEntity(it)?.chosenOpponent() }
            Player.EnchantedPlayer -> enchantedPlayer(context, state)
            is Player.OwnerOf -> context.targets.firstOrNull()?.toEntityId()
                ?.let { state.getEntity(it)?.get<CardComponent>()?.ownerId }
            // The owner of the ability's own source, which is NOT context.controllerId once the
            // permanent has been stolen — "its owner shuffles it into their library and draws"
            // still acts on the owner (Gandalf, Wandering Wizard).
            Player.OwnerOfSource -> context.sourceId
                ?.let { state.getEntity(it)?.get<CardComponent>()?.ownerId }
            // "You", read off the source instead of the context — the one reference that survives
            // a per-player rebind of controllerId (CountPlayersWith / ForEach-over-players).
            // Falls back to the context controller for sources that aren't on the battlefield
            // (a resolving spell), where the two always agree anyway.
            Player.ControllerOfSource -> context.sourceId
                ?.let { controllerOf(state, it) }
                ?: context.controllerId
            is Player.ControllerOf -> context.targets.firstOrNull()?.toEntityId()
                ?.let { controllerOf(state, it) }
            // Multi-player / list-only references have no single resolution here.
            // OwnersOfLinkedExile is resolved by ForEachExecutor.resolvePlayers (a player loop).
            Player.Each, Player.EachOpponent, Player.ActivePlayerFirst,
            Player.OwnersOfLinkedExile -> null
        }
    }

    /**
     * Distinct owners of the cards still in the effect source's linked-exile pile
     * ([com.wingedsheep.engine.state.components.battlefield.LinkedExileComponent], populated by
     * `Effects.ExileUntilLeaves`). Backs [Player.OwnersOfLinkedExile]. The component persists across
     * the source's own zone change, so this resolves correctly from a leaves-the-battlefield
     * trigger. Only cards still in an exile zone count (a token that ceased to exist, or a card that
     * has since left exile, drops out); owners are deduplicated so a player owning several exiled
     * cards is listed once. Empty — never "all players" — when nothing qualifies.
     */
    fun linkedExileOwners(state: GameState, context: EffectContext): List<EntityId> {
        val sourceId = context.sourceId ?: return emptyList()
        val linked = state.getEntity(sourceId)
            ?.get<com.wingedsheep.engine.state.components.battlefield.LinkedExileComponent>()
            ?: return emptyList()
        return linked.exiledIds
            .filter { id ->
                state.zones.any { (zone, cards) ->
                    zone.zoneType == com.wingedsheep.sdk.core.Zone.EXILE && id in cards
                }
            }
            .mapNotNull { id ->
                val container = state.getEntity(id)
                container?.get<OwnerComponent>()?.playerId
                    ?: container?.get<CardComponent>()?.ownerId
            }
            .distinct()
    }

    /**
     * Resolve a player target from the effect target definition and context.
     */
    fun resolvePlayerTarget(effectTarget: EffectTarget, context: EffectContext): EntityId? {
        return when (effectTarget) {
            is EffectTarget.Controller -> context.controllerId
            is EffectTarget.ContextTarget -> context.positionalTarget(effectTarget.index)?.toEntityId()
            is EffectTarget.BoundVariable -> context.pipeline.namedTargets[effectTarget.name]?.toEntityId()
            is EffectTarget.PipelineTarget ->
                context.pipeline.storedCollections[effectTarget.collectionName]?.getOrNull(effectTarget.index)
            is EffectTarget.PlayerRef -> when (effectTarget.player) {
                Player.You -> context.controllerId
                Player.TargetPlayer, Player.TargetOpponent, Player.Any -> firstPlayerTarget(context)
                Player.TriggeringPlayer -> context.triggeringPlayerId ?: context.triggeringEntityId
                else -> null
            }
            else -> null
        }
    }

    /**
     * The player the source Aura is attached to (CR 303 enchant player), or `null` when the source
     * isn't attached to a player. Reads the source's [AttachedToComponent] target and confirms it
     * is a player (in [GameState.turnOrder]). Used to resolve [Player.EnchantedPlayer].
     */
    fun enchantedPlayer(context: EffectContext, state: GameState): EntityId? {
        val sourceId = context.sourceId ?: return null
        val targetId = state.getEntity(sourceId)?.get<AttachedToComponent>()?.targetId ?: return null
        return targetId.takeIf { it in state.turnOrder }
    }

    /**
     * The controller of [entityId] wherever the entity is. A spell on the stack is controlled by
     * its caster ([SpellOnStackComponent.casterId] — the stack object's [ControllerComponent]
     * still reflects the owner when a player casts a card they don't own). A battlefield
     * permanent reads the *projected* controller: control-changing effects (Threaten, Empress
     * Galina) live in Layer 2 and never touch the base [ControllerComponent]. An entity that has
     * left the battlefield reads its last-known controller (CR 608.2h,
     * [LastKnownPermanentComponent]) — so "Destroy target creature. Its controller creates two
     * Map tokens." credits the controller-at-death, not the owner. Finally falls back to the
     * owner (cards that never were permanents, e.g. a discarded card).
     */
    private fun controllerOf(state: GameState, entityId: EntityId): EntityId? {
        val entity = state.getEntity(entityId) ?: return null
        return entity.get<SpellOnStackComponent>()?.casterId
            ?: state.projectedState.getController(entityId)
            ?: entity.get<ControllerComponent>()?.playerId
            ?: entity.get<LastKnownPermanentComponent>()?.snapshot?.controllerId
            ?: entity.get<CardComponent>()?.ownerId
    }

    /**
     * Resolve a player target with access to game state (for relational references like OwnerOf/ControllerOf).
     */
    fun resolvePlayerTarget(effectTarget: EffectTarget, context: EffectContext, state: GameState): EntityId? {
        // Player references get the full state-aware resolution (combat derivation,
        // chosen-opponent slots, relational owner/controller lookups).
        if (effectTarget is EffectTarget.PlayerRef) {
            return resolvePlayerRef(effectTarget.player, context, state)
        }

        // Try stateless resolution first
        resolvePlayerTarget(effectTarget, context)?.let { return it }

        // Handle TargetController: resolve the first target, then look up its controller
        if (effectTarget is EffectTarget.TargetController) {
            val targetEntity = context.targets.firstOrNull()?.toEntityId() ?: return null
            return controllerOf(state, targetEntity)
        }

        // "Its controller" as a *player* reference — the controller of the entity that fired the
        // trigger (Gonti, Night Minister: the creature that dealt the combat damage). The entity
        // resolver already walks projected controller → base controller → ability source →
        // last-known controller → owner, so this delegates rather than duplicating that ladder.
        if (effectTarget is EffectTarget.ControllerOfTriggeringEntity) {
            return resolveTarget(effectTarget, context, state)
        }

        // Handle ControllerOfPipelineTarget: look up controller of the pipeline-stored entity
        if (effectTarget is EffectTarget.ControllerOfPipelineTarget) {
            val targetEntityId = context.pipeline.storedCollections[effectTarget.collectionName]?.getOrNull(effectTarget.index) ?: return null
            return controllerOf(state, targetEntityId)
        }

        return null
    }

    /**
     * Resolve a player target to a list of player IDs (for multi-player effects like "each player").
     */
    fun resolvePlayerTargets(effectTarget: EffectTarget, state: GameState, context: EffectContext): List<EntityId> {
        return when (effectTarget) {
            is EffectTarget.Controller -> listOf(context.controllerId)
            is EffectTarget.BoundVariable -> context.pipeline.namedTargets[effectTarget.name]?.toEntityId()?.let { listOf(it) } ?: emptyList()
            is EffectTarget.PipelineTarget -> {
                context.pipeline.storedCollections[effectTarget.collectionName]?.getOrNull(effectTarget.index)
                    ?.let { listOf(it) } ?: emptyList()
            }
            is EffectTarget.ControllerOfPipelineTarget -> {
                val targetEntityId = context.pipeline.storedCollections[effectTarget.collectionName]?.getOrNull(effectTarget.index) ?: return emptyList()
                val entity = state.getEntity(targetEntityId) ?: return emptyList()
                val controllerId = entity.get<ControllerComponent>()?.playerId
                    ?: entity.get<CardComponent>()?.ownerId
                controllerId?.let { listOf(it) } ?: emptyList()
            }
            is EffectTarget.PlayerRef -> when (effectTarget.player) {
                Player.Each -> state.activePlayers
                // The APNAP-ordered flavour of Player.Each (CR 101.4) — for effects whose
                // per-player choices are made in turn order starting with the active player
                // ("each player sacrifices two creatures of their choice").
                Player.ActivePlayerFirst -> state.apnapOrder
                Player.EachOpponent -> state.getOpponents(context.controllerId)
                else -> resolvePlayerRef(effectTarget.player, context, state)
                    ?.let { listOf(it) } ?: emptyList()
            }
            // Use the state-aware resolver so state-dependent targets (e.g. TargetController,
            // which reads the target spell/permanent's controller) resolve here too. It tries
            // the stateless path first, so this stays a superset of the previous behavior.
            else -> resolvePlayerTarget(effectTarget, context, state)?.let { listOf(it) } ?: emptyList()
        }
    }

    /**
     * Resolve an [EntityReference] (AST-level "which entity" reference used by effects,
     * filters, and dynamic amounts) to a concrete entity id against the current [context]
     * and [state].
     *
     * Counterpart to [resolveTarget], which resolves [EffectTarget]s. [EntityReference] is the
     * value-AST reference (source / chosen target / sacrificed / tapped-as-cost / triggering /
     * affected / iteration / cost-storage / amassed army / enchanted creature). A `Target`
     * resolves to whatever the chosen target points at — permanent, card-in-zone, spell, or
     * player — via [toEntityId]; `EnchantedCreature` reads the source's attachment from [state].
     */
    fun resolveEntityReference(ref: EntityReference, context: EffectContext, state: GameState): EntityId? =
        when (ref) {
            is EntityReference.Source -> context.sourceId
            is EntityReference.EnchantedCreature ->
                context.sourceId?.let { state.getEntity(it)?.get<AttachedToComponent>()?.targetId }
            is EntityReference.Target -> context.positionalTarget(ref.index)?.toEntityId()
            is EntityReference.Sacrificed -> context.sacrificedPermanents.getOrNull(ref.index)?.entityId
            is EntityReference.TappedAsCost -> context.tappedPermanents.getOrNull(ref.index)
            is EntityReference.Triggering -> context.triggeringEntityId
            is EntityReference.RingBearer -> {
                // The creature carrying [player]'s Ring-bearer designation, on the battlefield
                // under their control (CR 701.54e). Null when the player has no Ring-bearer.
                val ownerId = when (ref.player) {
                    is Player.You -> context.controllerId
                    is Player.AnOpponent, is Player.EachOpponent,
                    is Player.TargetOpponent, is Player.TargetPlayer -> state.getOpponents(context.controllerId).firstOrNull()
                    else -> context.controllerId
                }
                ownerId?.let { owner ->
                    state.getBattlefield().firstOrNull { id ->
                        val bearer = state.getEntity(id)
                            ?.get<com.wingedsheep.engine.state.components.identity.RingBearerComponent>()
                        bearer?.ownerId == owner &&
                            state.getEntity(id)?.get<ControllerComponent>()?.playerId == owner
                    }
                }
            }
            is EntityReference.AffectedEntity -> context.affectedEntityId
            is EntityReference.IterationEntity -> context.pipeline.iterationTarget
            is EntityReference.FromCostStorage ->
                context.pipeline.storedCollections[ref.collectionName]?.getOrNull(ref.index)
            is EntityReference.AmassedArmy ->
                context.pipeline.storedCollections[EntityReference.AmassedArmy.STORAGE_KEY]?.firstOrNull()
        }

    /**
     * Convert a ChosenTarget to an EntityId.
     */
    fun ChosenTarget.toEntityId(): EntityId = when (this) {
        is ChosenTarget.Player -> playerId
        is ChosenTarget.Permanent -> entityId
        is ChosenTarget.Card -> cardId
        is ChosenTarget.Spell -> spellEntityId
    }
}
