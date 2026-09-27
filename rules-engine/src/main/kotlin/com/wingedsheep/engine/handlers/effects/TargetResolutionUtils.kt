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
import com.wingedsheep.engine.state.components.stack.ActivatedAbilityOnStackComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.state.components.stack.SpellOnStackComponent
import com.wingedsheep.engine.state.components.stack.TriggeredAbilityOnStackComponent
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Utility functions for resolving effect targets from symbolic references to concrete entity IDs.
 *
 * Targets in MTG are late-bound: effects reference targets symbolically
 * (e.g., ContextTarget(0) = "the first target chosen at cast time") and these
 * are resolved at execution time against the current game state.
 */
object TargetResolutionUtils {

    /**
     * Resolve [effectTarget] for an instruction that **acts** on the entity, using [context]
     * alone. References that need the game state (attachments, the top of a library, a linked
     * exile pile, controller lookups, …) resolve to null here — use the overload taking a
     * [GameState] for those.
     *
     * An action aimed at the source, the triggering object or a loop's current object resolves to
     * nothing once that object has changed zones (CR 400.7); see [resolveEntity] for value reads,
     * which don't gate.
     */
    fun resolveTarget(effectTarget: EffectTarget, context: EffectContext): EntityId? =
        if (isLostObject(effectTarget, context, state = null)) null
        else entityOf(effectTarget, context, state = null, projected = null)

    /**
     * Resolve [effectTarget] for an instruction that **acts** on the entity. Like the
     * context-only overload, but resolves every reference, consulting [state] for relational ones,
     * and checks the identity-captured objects against [state] as well as against the flags frozen
     * on [context].
     */
    fun resolveTarget(effectTarget: EffectTarget, context: EffectContext, state: GameState): EntityId? =
        if (isLostObject(effectTarget, context, state)) null
        else entityOf(effectTarget, context, state, projected = null)

    /**
     * Resolve [reference] for a **value read** — a characteristic comparison, an
     * `EntityProperty`, the colors a loop iterates. Unlike [resolveTarget] this does not drop an
     * object that has changed zones: the reader falls back to last-known information for it
     * (CR 608.2h, [lkiPolicyFor]).
     *
     * [projected] is the projection a mid-projection caller is building, used where a controller
     * has to be read from it rather than from [GameState.projectedState].
     */
    fun resolveEntity(
        reference: EffectTarget.SingleEntity,
        context: EffectContext,
        state: GameState,
        projected: com.wingedsheep.engine.mechanics.layers.ProjectedState? = null,
    ): EntityId? = entityOf(reference, context, state, projected)

    /**
     * Whether [target] names the source, the triggering object, or a loop's current object and
     * that object has since become a new object (CR 400.7). [state] is null for the context-only
     * resolution, which reads the flags [EffectContext] froze at the start of the instruction.
     */
    private fun isLostObject(target: EffectTarget, context: EffectContext, state: GameState?): Boolean {
        val references = context.objectReferences
        return when (target) {
            EffectTarget.Self -> context.sourceReferenceLost ||
                (state != null && !references.isCurrent(references.source, state))
            EffectTarget.TriggeringEntity -> context.triggeringReferenceLost || (state != null &&
                context.triggeringEntityId !in state.turnOrder && !references.isCurrent(references.triggering, state))
            EffectTarget.IterationEntity -> context.iterationReferenceLost ||
                (state != null && references.iteration != null && !references.isIterationCurrent(state))
            else -> false
        }
    }

    /**
     * The one mapping from a reference to the entity it names. Every resolution — actions, value
     * reads, predicate evaluation — goes through here; the entry points above differ only in the
     * identity gate they apply first. [state] is null when resolving from the context alone, and
     * every reference that needs the game state then names nothing.
     */
    private fun entityOf(
        target: EffectTarget,
        context: EffectContext,
        state: GameState?,
        projected: com.wingedsheep.engine.mechanics.layers.ProjectedState?,
    ): EntityId? = when (target) {
        EffectTarget.Self -> context.sourceId
        EffectTarget.GrantingSource -> context.granterId
        EffectTarget.Controller -> context.controllerId
        is EffectTarget.ContextTarget -> context.positionalTarget(target.index)?.toEntityId()
        is EffectTarget.BoundVariable -> context.pipeline.namedTargets[target.name]?.toEntityId()
        is EffectTarget.SpecificEntity -> target.entityId
        EffectTarget.TriggeringEntity -> context.triggeringEntityId
        is EffectTarget.DiscardedAsCost -> context.discardedAsCostCards.getOrNull(target.index)
        is EffectTarget.SacrificedAsCost -> context.sacrificedPermanents.getOrNull(target.index)?.entityId
        is EffectTarget.TappedAsCost -> context.tappedPermanents.getOrNull(target.index)
        is EffectTarget.PipelineTarget ->
            context.pipeline.storedCollections[target.collectionName]?.getOrNull(target.index)
        EffectTarget.AmassedArmy ->
            context.pipeline.storedCollections[EffectTarget.AmassedArmy.STORAGE_KEY]?.firstOrNull()
        EffectTarget.AffectedEntity -> context.affectedEntityId
        EffectTarget.IterationEntity -> context.iterationEntityId
        is EffectTarget.LibraryTop -> state?.let { resolveLibraryTop(target.player, context, it, projected) }
        EffectTarget.EnchantedCreature,
        EffectTarget.EquippedCreature,
        EffectTarget.EnchantedPermanent -> state?.let { attachmentHost(context, it) }
        EffectTarget.ChosenCreature -> context.sourceId?.let { state?.getEntity(it)?.chosenCreatureRef() }
        is EffectTarget.LinkedExiledCard -> state?.let {
            com.wingedsheep.engine.handlers.effects.linkedexile.LinkedExileLookup
                .exiledCard(it, context.sourceId, target.index)
        }
        is EffectTarget.RingBearer -> state?.let { ringBearer(target.player, context, it) }
        EffectTarget.AttachedToTriggeringPermanent -> state?.let { attachedToTriggeringPermanent(context, it) }
        EffectTarget.TargetController -> state?.let { s ->
            context.targets.firstOrNull()?.toEntityId()?.let { controllerOf(s, it) }
        }
        EffectTarget.ControllerOfTriggeringEntity -> state?.let { controllerOfTriggeringEntity(context, it) }
        is EffectTarget.ControllerOfPipelineTarget -> state?.let { s ->
            context.pipeline.storedCollections[target.collectionName]?.getOrNull(target.index)
                ?.let { controllerOf(s, it) }
        }
        // A single-player reference names that player; a plural one ("each opponent") names no
        // single entity and goes through [resolvePlayerTargets].
        is EffectTarget.PlayerRef -> state?.let { resolvePlayerRef(target.player, context, it) }
        // Sets of objects name no single entity: the group resolvers handle them.
        // ControllerOfDamageSource is resolved by the damage pipeline from the damage in flight.
        is EffectTarget.GroupRef,
        is EffectTarget.FilteredTarget,
        EffectTarget.EachDamagedBySourceThisGame,
        EffectTarget.ControllerOfDamageSource -> null
    }

    /**
     * The permanent the source Aura/Equipment is attached to. Once the attachment itself is gone —
     * its own ability paid for it — "enchanted creature" means the host it was attached to as it
     * last existed on the battlefield (CR 608.2h): Thrull Retainer's "Sacrifice this Aura:
     * Regenerate enchanted creature" has already unattached itself by the time the regeneration
     * shield is created, and reading the live link there answers "nothing".
     */
    private fun attachmentHost(context: EffectContext, state: GameState): EntityId? {
        val container = context.sourceId?.let { state.getEntity(it) } ?: return null
        return container.get<AttachedToComponent>()?.targetId
            ?: container.get<LastKnownPermanentComponent>()?.snapshot?.attachedTo
    }

    /**
     * The creature carrying [player]'s Ring-bearer designation, on the battlefield under their
     * control (CR 701.54). Null when the player has no Ring-bearer.
     */
    private fun ringBearer(player: Player, context: EffectContext, state: GameState): EntityId? {
        val ownerId = when (player) {
            Player.AnOpponent, Player.EachOpponent, Player.TargetOpponent, Player.TargetPlayer ->
                state.getOpponents(context.controllerId).firstOrNull()
            else -> context.controllerId
        } ?: return null
        return state.getBattlefield().firstOrNull { id ->
            val entity = state.getEntity(id) ?: return@firstOrNull false
            entity.get<com.wingedsheep.engine.state.components.identity.RingBearerComponent>()?.ownerId == ownerId &&
                entity.get<ControllerComponent>()?.playerId == ownerId
        }
    }

    private fun attachedToTriggeringPermanent(context: EffectContext, state: GameState): EntityId? {
        // "Becomes unattached": the host recorded when the trigger fired is the only right
        // answer — the live link is by now either gone or, if the unattach was caused by
        // equipping the attachment elsewhere, pointing at the *new* host. Scoped to the
        // battlefield so a former host that has itself left resolves to nothing, which is
        // Stitcher's Graft's "the triggered ability won't do anything in that case".
        context.triggerContext?.unattachedFromEntityId?.let {
            return it.takeIf { id -> id in state.getBattlefield() }
        }
        // "Becomes attached": the triggering entity is the attachment, and the host is its
        // current attachment target. Reading it live means a "for as long as attached" payoff
        // does nothing if the attachment has already moved or left (CR 611.2b) — what Eriette
        // and Assimilation Aegis want.
        val attachmentId = context.triggeringEntityId ?: return null
        return state.getEntity(attachmentId)?.get<AttachedToComponent>()?.targetId
    }

    private fun controllerOfTriggeringEntity(context: EffectContext, state: GameState): EntityId? {
        if (!context.objectReferences.isCurrent(context.objectReferences.triggering, state)) {
            return context.triggeringPlayerId
        }
        val triggerId = context.triggeringEntityId ?: return null
        val entity = state.getEntity(triggerId) ?: return null
        state.projectedState.getController(triggerId)?.let { return it }
        entity.get<ControllerComponent>()?.playerId?.let { return it }
        // An activated ability's stack entity is a bare container with no
        // ControllerComponent. "That artifact's controller" (Haunting Wind, Artifact
        // Possession) means the controller of the ability's SOURCE permanent — fall
        // through to it, or to the ability's own controller as last-known information
        // if the source has left the battlefield.
        entity.get<ActivatedAbilityOnStackComponent>()?.let { ability ->
            return controllerOf(state, ability.sourceId) ?: ability.controllerId
        }
        // The triggering permanent may itself have left the battlefield: last-known
        // controller (CR 608.2h) before the owner.
        entity.get<LastKnownPermanentComponent>()?.snapshot?.controllerId?.let { return it }
        return entity.get<CardComponent>()?.ownerId
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
        defendingPlayerOfAttacker(state, context.sourceId)?.let { return it }
        return (context.triggeringPlayerId ?: context.triggeringEntityId)
            ?.takeIf { it in state.turnOrder }
    }

    /**
     * The player [attackerId] is attacking (CR 802.2a), read from combat: its own
     * `AttackingComponent` while it is still on the battlefield, else the defender frozen into its
     * battlefield-exit snapshot. A creature attacking a planeswalker or battle maps to that
     * permanent's controller / protector — "defending player" is always a player.
     *
     * The frozen leg is the rule's own second clause: once the creature "is no longer attacking",
     * the defending player is still the one it *was* attacking before it left combat. That is what
     * lets an ability which sacrifices its own source *before* naming the defending player still
     * find them (Mindstab Thrull, Necrite) — the sacrifice tears the live component down
     * mid-resolution, and every read after it would otherwise fall through to the ability's
     * controller, the attacking player, the one player it cannot be.
     */
    fun defendingPlayerOfAttacker(state: GameState, attackerId: EntityId?): EntityId? {
        val container = attackerId?.let { state.getEntity(it) } ?: return null
        val defenderId = container.get<AttackingComponent>()?.defenderId
            ?: container.get<LastKnownPermanentComponent>()?.snapshot?.attackedDefenderId
            ?: return null
        // A player defends as themselves, a planeswalker for its controller, and a battle for its
        // protector (CR 310.9d) — which for a Siege is not its controller.
        if (defenderId in state.turnOrder) return defenderId
        return com.wingedsheep.engine.mechanics.battle.Battles.protectorOf(state, defenderId)
            ?: state.getEntity(defenderId)?.get<ControllerComponent>()?.playerId
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
            is Player.BoundVariable -> context.pipeline.namedTargets[player.name]?.toEntityId()
            Player.TriggeringPlayer -> context.triggeringPlayerId ?: context.triggeringEntityId
            Player.Candidate -> context.candidatePlayerId
            Player.AnOpponent -> state.getOpponents(context.controllerId).firstOrNull()
            Player.DefendingPlayer -> resolveDefendingPlayer(context, state)
            Player.ChosenOpponent -> context.chosenOpponent(state)
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
            // "its controller", inside a ForEach over entities — the loop's current entity, not the
            // effect's source or its chosen target.
            Player.ControllerOfIterationEntity -> context.iterationEntityId
                ?.let { controllerOf(state, it) }
            // "its controller" for the permanent a continuous effect is modifying.
            Player.ControllerOfAffectedEntity -> context.affectedEntityId
                ?.let { controllerOf(state, it) }
            // The other end of a becomes-target trigger: whoever controls the spell or ability
            // that did the targeting (Fractured Loyalty). The trigger context carries the
            // targeting stack object; [stackObjectController] reads it while it is still on the
            // stack, and [controllerOf] supplies last-known information once it has left.
            Player.ControllerOfTargetingSource -> context.triggerContext?.targetingSourceEntityId
                ?.let { stackObjectController(state, it) ?: controllerOf(state, it) }
            // "That source's controller", for the pipelines that are keyed by Player rather than
            // EffectTarget (Belltower Sphinx's mill). Same entity the EffectTarget form reads, and
            // [controllerOf]'s ladder ends in last-known controller then owner — which is what
            // makes it work for a burn spell that has already left the stack by resolution
            // (CR 608.2h). Distinct from [Player.TriggeringPlayer], which reads the context's
            // *player* slot and is null when the thing that triggered the ability was an object.
            Player.ControllerOfTriggeringEntity -> context.triggeringEntityId
                ?.let { controllerOf(state, it) }
            // Multi-player / list-only references have no single resolution here.
            // OwnersOfLinkedExile is resolved by ForEachExecutor.resolvePlayers (a player loop);
            // EachTargetedPlayer by DynamicAmountEvaluator.resolveUnifiedPlayerIds. Collapsing
            // either to its first player is exactly the bug they exist to avoid, so neither gets a
            // single-player arm.
            Player.Each, Player.EachOpponent, Player.ActivePlayerFirst,
            Player.EachTargetedPlayer, Player.OwnersOfLinkedExile, is Player.InCollection -> null
        }
    }

    /**
     * The players a `StorePlayer` step recorded in the pipeline collection [collection]
     * ([Player.InCollection]), in APNAP order (CR 101.4), skipping anyone who has left the game.
     * A missing or empty collection is nobody.
     */
    fun playersInCollection(state: GameState, context: EffectContext, collection: String): List<EntityId> {
        val recorded = context.pipeline.storedCollections[collection].orEmpty().toSet()
        if (recorded.isEmpty()) return emptyList()
        return state.apnapOrder.filter { it in recorded }
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
    /**
     * The controller of a **stack object** — the caster of a spell, or the controller of an
     * activated or triggered ability. Returns `null` for anything that is not a stack object,
     * so callers can fall through to a battlefield/last-known lookup.
     *
     * A stack object's own [ControllerComponent] is not authoritative: it still reflects the
     * owner when a player casts a card they don't own, which is why each of the three stack
     * components carries its own controller field.
     */
    fun stackObjectController(state: GameState, entityId: EntityId): EntityId? {
        val container = state.getEntity(entityId) ?: return null
        return container.get<SpellOnStackComponent>()?.casterId
            ?: container.get<ActivatedAbilityOnStackComponent>()?.controllerId
            ?: container.get<TriggeredAbilityOnStackComponent>()?.controllerId
    }

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

        // A player pinned by entity id. Used when an executor computes a set of players at
        // resolution and lowers the choice between them into a sub-effect — there is no symbolic
        // reference that could name the answer, so the id is carried directly (Loxodon
        // Peacekeeper's tie-break). Guarded on turn order so a permanent's id can never be
        // mistaken for a player.
        if (effectTarget is EffectTarget.SpecificEntity) {
            return effectTarget.entityId.takeIf { it in state.turnOrder }
        }

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

    /** Library membership is read live, without revealing the card or retaining an old top. */
    fun resolveLibraryTop(
        player: Player,
        context: EffectContext,
        state: GameState,
        projected: com.wingedsheep.engine.mechanics.layers.ProjectedState? = null
    ): EntityId? {
        val playerId = if (player == Player.ControllerOfSource && projected != null) {
            context.sourceId?.let { projected.getController(it) } ?: context.controllerId
        } else resolvePlayerRef(player, context, state)
        return playerId?.takeIf { it in state.turnOrder }?.let { state.getLibrary(it).firstOrNull() }
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
