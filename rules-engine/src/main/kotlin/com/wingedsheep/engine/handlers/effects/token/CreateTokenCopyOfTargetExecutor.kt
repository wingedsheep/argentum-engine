package com.wingedsheep.engine.handlers.effects.token

import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.core.ZoneChangeEvent
import com.wingedsheep.engine.handlers.DynamicAmountEvaluator
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.handlers.effects.copy.CopyExceptionApplier
import com.wingedsheep.engine.mechanics.layers.StaticAbilityHandler
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.event.DelayedTriggeredAbility
import com.wingedsheep.engine.event.GrantedActivatedAbility
import com.wingedsheep.engine.event.GrantedStaticAbility
import com.wingedsheep.engine.event.GrantedTriggeredAbility
import com.wingedsheep.engine.state.Component
import com.wingedsheep.engine.state.ComponentContainer
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.state.components.battlefield.AttachmentsComponent
import com.wingedsheep.engine.state.components.battlefield.EnteredThisTurnComponent
import com.wingedsheep.engine.state.components.battlefield.SummoningSicknessComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.combat.AttackingComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.identity.DoubleFacedComponent
import com.wingedsheep.engine.state.components.identity.TokenComponent
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.conditions.SourceIsRingBearer
import com.wingedsheep.sdk.scripting.effects.CompositeEffect
import com.wingedsheep.sdk.scripting.effects.CreateTokenCopyOfTargetEffect
import com.wingedsheep.sdk.scripting.effects.Gate
import com.wingedsheep.sdk.scripting.effects.GatedEffect
import com.wingedsheep.sdk.scripting.effects.MoveToZoneEffect
import com.wingedsheep.sdk.scripting.effects.SacrificeTargetEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import java.util.UUID
import kotlin.reflect.KClass

/**
 * Executor for CreateTokenCopyOfTargetEffect.
 *
 * Creates N token copies of a targeted permanent (resolved via EffectTarget).
 * Used for "Create X tokens that are copies of target token you control."
 *
 * **Aura copies (CR 303.4h).** A token copy of an Aura is put onto the battlefield without being
 * cast, so it doesn't target — its controller instead *chooses* what it enchants as it enters,
 * bound by the copied Aura's own enchant restriction (CR 303.4f; targeting restrictions such as
 * hexproof and shroud are ignored). The choice is raised *before* the token exists, so the token
 * enters already attached and its enters-the-battlefield triggers see the attachment. If there is
 * no legal object to enchant, the token isn't created at all (CR 303.4g) — Yenna, Redtooth Regent
 * copying an Aura whose only legal hosts have left the battlefield.
 */
class CreateTokenCopyOfTargetExecutor(
    private val amountEvaluator: DynamicAmountEvaluator = DynamicAmountEvaluator(),
    private val staticAbilityHandler: StaticAbilityHandler? = null,
    private val cardRegistry: CardRegistry? = null
) : EffectExecutor<CreateTokenCopyOfTargetEffect> {

    override val effectType: KClass<CreateTokenCopyOfTargetEffect> = CreateTokenCopyOfTargetEffect::class

    override fun execute(
        state: GameState,
        effect: CreateTokenCopyOfTargetEffect,
        context: EffectContext
    ): EffectResult {
        val targetId = context.resolveTarget(effect.target, state)
            ?: return EffectResult.success(state)

        val targetContainer = state.getEntity(targetId)
            ?: return EffectResult.success(state)

        val targetCard = targetContainer.get<CardComponent>()
            ?: return EffectResult.success(state)

        val count = amountEvaluator.evaluate(state, effect.count, context)
        if (count <= 0) return EffectResult.success(state)

        // Who creates (controls + owns) the token. Defaults to the effect's controller; a player
        // target (e.g. "Target player creates a token …" — Echocasting Symposium) puts the token
        // under that player's control instead.
        val controllerId = effect.controller
            ?.let { context.resolvePlayerTargets(it, state).firstOrNull() }
            ?: context.controllerId

        // Check for token creation replacement effects (e.g., Mirrormind Crown).
        // Mirrormind's replacement copies the equipped creature instead of this
        // effect's intended copy, dropping any added keywords / triggered abilities.
        val replacementResult = TokenCreationReplacementHelper.checkReplacement(
            state, effect, context, count, controllerId, cardRegistry, staticAbilityHandler
        )
        if (replacementResult != null) return replacementResult

        // An Aura token needs its host chosen before it can be created (CR 303.4h) — the copy's
        // type line decides, so read it off the copied CardComponent (copiable values only).
        if (auraTypeLineOf(effect, targetCard).isAura) {
            return AuraTokenHostChooser.pause(
                state = state,
                effect = effect,
                context = context,
                auraDefinitionId = targetCard.cardDefinitionId,
                auraName = targetCard.name,
                controllerId = controllerId,
                remaining = com.wingedsheep.engine.core.GameLimits
                    .cappedTokenCount(count, "target-copy tokens"),
                cardRegistry = cardRegistry,
            )
        }

        return createTokens(state, effect, context, controllerId, count, auraHostId = null)
    }

    /**
     * Create [count] token copies of the effect's target. When [auraHostId] is non-null every
     * created token enters attached to it (the Aura path — see the class docs); otherwise the
     * tokens enter unattached. Split out of [execute] so the Aura host-choice continuation can
     * re-enter here once the controller has picked a host.
     */
    internal fun createTokens(
        state: GameState,
        effect: CreateTokenCopyOfTargetEffect,
        context: EffectContext,
        controllerId: EntityId,
        count: Int,
        auraHostId: EntityId?,
    ): EffectResult {
        val targetId = context.resolveTarget(effect.target, state)
            ?: return EffectResult.success(state)
        val targetContainer = state.getEntity(targetId)
            ?: return EffectResult.success(state)
        val targetCard = targetContainer.get<CardComponent>()
            ?: return EffectResult.success(state)

        var newState = state
        val events = mutableListOf<com.wingedsheep.engine.core.GameEvent>()
        val createdTokens = mutableListOf<EntityId>()

        // Every "except …" clause (CR 707.9) — added/removed types, added keywords, base P/T,
        // colors, no-mana-cost — is applied by the shared CopyExceptionApplier, so the token
        // path and the "permanent becomes a copy" path can't drift. Both the exceptions view and
        // the resulting component are loop-invariant, so they are built once rather than per token.
        val exceptions = effect.copyExceptions
        val tokenCard = CopyExceptionApplier.apply(targetCard, exceptions)
            .copy(ownerId = controllerId)

        val cappedCount = com.wingedsheep.engine.core.GameLimits.cappedTokenCount(count, "target-copy tokens")
        for (index in 0 until cappedCount) {
            val (tokenId, stateWithId) = newState.newEntity()
            newState = stateWithId

            val components = mutableListOf<Component>(
                tokenCard,
                TokenComponent,
                ControllerComponent(controllerId),
                SummoningSicknessComponent,
                EnteredThisTurnComponent
            )
            if (effect.tapped) {
                components.add(TappedComponent)
            }
            // Only creatures can be attacking. A copy of a card whose printed type line isn't a
            // creature (e.g. an animated permanent exiled and reverted to its printed type) still
            // enters tapped but never attacking — see Mardu Siegebreaker's rulings.
            if (effect.attacking && tokenCard.typeLine.isCreature) {
                // The token joins the source's attack (CR 802.2a) — see CreateTokenExecutor.
                val defenderId = com.wingedsheep.engine.handlers.effects.TargetResolutionUtils
                    .resolveDefendingPlayer(context, newState)
                    ?: newState.getOpponents(controllerId).firstOrNull()
                if (defenderId != null) {
                    components.add(AttackingComponent(defenderId))
                }
            }
            // CR 707.8a: a token copy of a double-faced permanent has both faces and enters
            // with the same face up as the source. Counters
            // are intentionally not copied (handled by the absence of CountersComponent copy
            // throughout this executor).
            targetContainer.get<DoubleFacedComponent>()?.let { sourceDfc ->
                components.add(
                    DoubleFacedComponent(
                        frontCardDefinitionId = sourceDfc.frontCardDefinitionId,
                        backCardDefinitionId = sourceDfc.backCardDefinitionId,
                        currentFace = sourceDfc.currentFace
                    )
                )
            }

            // CR 303.4h: an Aura token enters already attached to the host its controller chose
            // before it was created, so the attachment is in place for any enters-the-battlefield
            // trigger and for the very first state-based check.
            if (auraHostId != null) {
                components.add(AttachedToComponent(auraHostId))
            }

            var container = ComponentContainer.of(*components.toTypedArray())

            if (staticAbilityHandler != null) {
                container = staticAbilityHandler.addContinuousEffectComponent(container)
                container = staticAbilityHandler.addReplacementEffectComponent(container)
            }

            newState = newState.withEntity(tokenId, container)
            newState = com.wingedsheep.engine.handlers.effects.BattlefieldEntry
                .place(newState, controllerId, tokenId)
            // A token copy honors global "[filter] enter tapped" replacements (Authority of the
            // Consuls / Dauntless Dismantler on an opponent's token copy).
            newState = com.wingedsheep.engine.handlers.effects.EnterTappedReplacements
                .applyCreatedTokenEntryTap(
                    newState, tokenId, controllerId, definedTapped = effect.tapped,
                )
            // Wire the host side of the attachment and announce it, so "becomes attached"
            // triggers (Eriette, the Beguiler) fire for an Aura token the same way they do when
            // an Aura card is put onto the battlefield attached (CR 603.2e).
            if (auraHostId != null) {
                newState = newState.updateEntity(auraHostId) { hostContainer ->
                    val existing = hostContainer.get<AttachmentsComponent>()
                    hostContainer.with(
                        AttachmentsComponent((existing?.attachedIds ?: emptyList()) + tokenId)
                    )
                }
                events.add(
                    com.wingedsheep.engine.core.PermanentAttachedEvent(
                        attachmentId = tokenId,
                        attachmentName = tokenCard.name,
                        attachedToId = auraHostId,
                        controllerId = controllerId,
                    )
                )
            }

            // As-enters "enters with counters" (CR 614.1c): the copied card's own
            // EntersWithCounters (a copy of a creature that "enters with a +1/+1 counter"), plus
            // global grants from other permanents (Gev, Scaled Scorch). BattlefieldEntry.place skips
            // this setup, so apply it here the way the standard entry pipeline does. Non-pausing.
            val (afterCounters, counterEvents) = if (cardRegistry != null) {
                com.wingedsheep.engine.handlers.effects.EntersWithReplacements
                    .applyOnEntry(newState, tokenId, controllerId, cardRegistry)
            } else {
                com.wingedsheep.engine.handlers.effects.EntersWithReplacements
                    .applyGlobal(newState, tokenId, controllerId)
            }
            newState = afterCounters
            events.addAll(counterEvents)

            for (ability in effect.triggeredAbilities) {
                val grant = GrantedTriggeredAbility(
                    entityId = tokenId,
                    ability = ability,
                    duration = Duration.Permanent
                )
                newState = newState.copy(
                    grantedTriggeredAbilities = newState.grantedTriggeredAbilities + grant
                )
            }

            // Activated abilities granted to the copy (e.g. Shelob's Food sacrifice ability). Tokens
            // have no CardDefinition, so granted abilities live in GameState.grantedActivatedAbilities,
            // which the legal-action enumerator and ActivateAbilityHandler consult for any entity.
            for (ability in effect.activatedAbilities) {
                val grant = GrantedActivatedAbility(
                    entityId = tokenId,
                    ability = ability,
                    duration = Duration.Permanent
                )
                newState = newState.copy(
                    grantedActivatedAbilities = newState.grantedActivatedAbilities + grant
                )
            }

            // Static abilities granted to the copy — the "except it has \"[static ability]\"" copy
            // clause (Firion, Wild Rose Warrior: "except it has \"This Equipment's equip abilities
            // cost {2} less to activate\""). Like triggered/activated grants, these live in
            // GameState.grantedStaticAbilities since tokens have no CardDefinition; each static
            // reader (e.g. the equip-cost reducer) unions granted statics with printed ones.
            for (ability in effect.addedStaticAbilities) {
                val grant = GrantedStaticAbility(
                    entityId = tokenId,
                    ability = ability,
                    duration = Duration.Permanent
                )
                newState = newState.copy(
                    grantedStaticAbilities = newState.grantedStaticAbilities + grant
                )
            }

            // As-enters "choose X as this enters" (CR 614.12) + granted riot (CR 702.136/702.136b).
            // A token copy of a creature that "enters with your choice of …" — or a Spider entering
            // while Spider-Punk grants it riot — pauses for a player decision. We deliberately do NOT
            // emit this token's entry ZoneChangeEvent when we pause: the choice resumer synthesizes it
            // after the choice resolves, so ETB triggers fire exactly once (mirroring
            // TokenFromDefinition). Counters already added ride along as carryEvents.
            val choicePlan = if (cardRegistry != null) {
                TokenEntryReplacements.firstEntersWithChoice(newState, tokenId, cardRegistry)
            } else null
            if (choicePlan != null) {
                val remaining = cappedCount - (index + 1)
                var pausedState = newState
                if (remaining > 0) {
                    // The rest of the batch resumes below the choice's continuation once this token's
                    // choice (and every granted-riot instance) has fully resolved.
                    pausedState = pausedState.pushContinuation(
                        com.wingedsheep.engine.core.CreateTokenCopyRemainingContinuation(
                            decisionId = "create-token-copy-remaining-${UUID.randomUUID()}",
                            effect = effect,
                            context = context,
                            controllerId = controllerId,
                            remaining = remaining,
                        )
                    )
                }
                val paused = com.wingedsheep.engine.handlers.effects.PermanentEntryReplacements
                    .pauseForEntersWithChoice(
                        state = pausedState,
                        entityId = tokenId,
                        controllerId = controllerId,
                        cardComponent = tokenCard,
                        choice = choicePlan.choice,
                        fromZone = null,
                        carryEvents = events,
                        syntheticRiot = choicePlan.syntheticRiot,
                        syntheticRiotRemaining = choicePlan.syntheticRiotRemaining,
                    )
                if (paused != null) return EffectResult.from(paused)
                // A null pause means the choice couldn't actually be presented (e.g. no legal object) —
                // fall through and complete this token's entry normally.
            }

            events.add(
                ZoneChangeEvent(
                    entityId = tokenId,
                    entityName = tokenCard.name,
                    fromZone = null,
                    toZone = Zone.BATTLEFIELD,
                    ownerId = controllerId
                )
            )

            // CR 714.2b/714.3a: a token copy of a Saga enters as a Saga and gets its on-enter lore
            // counter (chapter I then triggers). BattlefieldEntry.place is the ad-hoc insertion path
            // and intentionally skips enters-with-counters setup, so apply the shared Saga-entry
            // helper here — the same one the standard moveToZone pipeline uses. No-op for non-Sagas.
            val (sagaState, sagaEvents) = com.wingedsheep.engine.handlers.effects.ZoneMovementUtils
                .applySagaEntryIfNeeded(newState, tokenId)
            newState = sagaState
            events.addAll(sagaEvents)

            // CR 306.5b: likewise a token copy of a planeswalker enters with the copied printed
            // loyalty (a copiable value, CR 707.2). Without it state-based actions (CR 704.5i)
            // bin the token the instant it enters. No-op for non-planeswalkers.
            cardRegistry?.let { registry ->
                val (loyaltyState, loyaltyEvents) = com.wingedsheep.engine.handlers.effects.ZoneMovementUtils
                    .applyIntrinsicEntryCountersIfNeeded(newState, tokenId, controllerId, registry)
                newState = loyaltyState
                events.addAll(loyaltyEvents)
            }

            createdTokens.add(tokenId)
        }

        // If sacrificeAtStep is set, create a delayed trigger to sacrifice each created token
        // copy at that step (e.g. Mardu Siegebreaker: "at the beginning of your next end step,
        // sacrifice those tokens"). Mirrors CreateTokenExecutor's sacrificeAtStep handling.
        val sacrificeStep = effect.sacrificeAtStep
        if (sacrificeStep != null && createdTokens.isNotEmpty()) {
            val sourceId = context.sourceId ?: controllerId
            val sourceName = state.getEntity(sourceId)?.get<CardComponent>()?.name ?: "Unknown"
            for (tokenId in createdTokens) {
                val delayedTrigger = DelayedTriggeredAbility(
                    id = UUID.randomUUID().toString(),
                    effect = SacrificeTargetEffect(EffectTarget.SpecificEntity(tokenId)),
                    fireAtStep = sacrificeStep,
                    sourceId = sourceId,
                    sourceName = sourceName,
                    controllerId = controllerId,
                    // "sacrifice at the beginning of your next end step" → gate the firing step
                    // to the token controller's turn (the single fireOnPlayerId gate).
                    fireOnPlayerId = if (effect.sacrificeOnlyOnControllersTurn) controllerId else null
                )
                newState = newState.addDelayedTrigger(delayedTrigger)
            }
        }

        // If exileAtStep is set, create a delayed trigger to exile each created token copy at that
        // step (Sauron, the Necromancer: "at the beginning of the next end step, exile that token
        // unless Sauron is your Ring-bearer"). The firing step is the next matching step of any
        // player's turn ("the next end step", so no fireOnPlayerId gate). When
        // exileUnlessSourceIsRingBearer is set the exile is wrapped in a condition that skips it
        // while the source (resolved to the delayed trigger's sourceId) is the controller's
        // Ring-bearer (CR 701.54e) — the condition is re-evaluated at fire time.
        val exileStep = effect.exileAtStep
        if (exileStep != null && createdTokens.isNotEmpty()) {
            val sourceId = context.sourceId ?: controllerId
            val sourceName = state.getEntity(sourceId)?.get<CardComponent>()?.name ?: "Unknown"
            for (tokenId in createdTokens) {
                val exileEffect = MoveToZoneEffect(EffectTarget.SpecificEntity(tokenId), Zone.EXILE)
                val delayedEffect = if (effect.exileUnlessSourceIsRingBearer) {
                    GatedEffect(
                        gate = Gate.WhenCondition(SourceIsRingBearer),
                        then = CompositeEffect(emptyList()),
                        otherwise = exileEffect
                    )
                } else {
                    exileEffect
                }
                val delayedTrigger = DelayedTriggeredAbility(
                    id = UUID.randomUUID().toString(),
                    effect = delayedEffect,
                    fireAtStep = exileStep,
                    sourceId = sourceId,
                    sourceName = sourceName,
                    controllerId = controllerId
                )
                newState = newState.addDelayedTrigger(delayedTrigger)
            }
        }

        // Publish the created token ids into the shared CREATED_TOKENS pipeline collection so a
        // following composite step can reference them — e.g. "Create a token that's a copy of
        // target permanent ... Put six +1/+1 counters on it" composes this with
        // AddCountersToCollection(CREATED_TOKENS, ...). Mirrors CreateTokenExecutor /
        // CreateTokenCopyOfSourceExecutor, which expose their tokens the same way.
        return EffectResult(
            state = newState,
            events = events,
            updatedCollections = mapOf(com.wingedsheep.sdk.scripting.effects.CREATED_TOKENS to createdTokens.toList())
        )
    }

    /**
     * The type line the copy will actually have, after every "except …" type clause. Read to
     * decide whether the Aura host choice applies — "except it's a creature" turns an Aura copy
     * into something that isn't an Aura and needs no host. Shares [CopyExceptionApplier.typeLine]
     * with the token that is actually built, so the two can't disagree about what the copy is.
     */
    private fun auraTypeLineOf(
        effect: CreateTokenCopyOfTargetEffect,
        targetCard: CardComponent,
    ) = CopyExceptionApplier.typeLine(targetCard.typeLine, effect.copyExceptions)
}
