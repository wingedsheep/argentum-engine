package com.wingedsheep.engine.view.projection

import com.wingedsheep.engine.handlers.ConditionEvaluator
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.PipelineState
import com.wingedsheep.engine.handlers.effects.composite.asConditional
import com.wingedsheep.engine.mechanics.stack.buildBeheldStoredCollections
import com.wingedsheep.engine.state.FACE_DOWN_CARD_DISPLAY_NAME
import com.wingedsheep.engine.state.FACE_DOWN_DISPLAY_NAME
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.PlayerComponent
import com.wingedsheep.engine.state.components.stack.ActivatedAbilityOnStackComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.state.components.stack.SpellOnStackComponent
import com.wingedsheep.engine.state.components.stack.TargetsComponent
import com.wingedsheep.engine.state.components.stack.TriggeredAbilityOnStackComponent
import com.wingedsheep.engine.view.ClientChosenTarget
import com.wingedsheep.engine.view.ClientPerModeTargetGroup
import com.wingedsheep.engine.view.Visibility
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.effects.CompositeEffect
import com.wingedsheep.sdk.scripting.effects.Effect
import com.wingedsheep.sdk.scripting.effects.ModalEffect
import com.wingedsheep.sdk.scripting.effects.SelfReferentialDescription
import com.wingedsheep.sdk.scripting.targets.DEFAULT_SELF_NOUN
import com.wingedsheep.sdk.scripting.targets.resolveSelfNoun

/**
 * Renders the text a spell or ability shows on the stack: dynamic amounts evaluated against what
 * was actually chosen and paid, conditional branches collapsed to the one that will fire, chosen
 * modes spelled out per mode with their targets named (or redacted, per [Visibility]).
 */
internal class StackTextRenderer(
    private val conditionEvaluator: ConditionEvaluator,
    private val visibility: Visibility
) {

    /**
     * Generate stack text with dynamic amounts evaluated to concrete values.
     * Falls back to static description if evaluation fails.
     */
    fun runtimeStackText(
        state: GameState,
        spellEntityId: EntityId,
        spellOnStack: SpellOnStackComponent,
        cardDef: CardDefinition
    ): String? {
        val effect = cardDef.script.spellEffect ?: return null

        // For modal spells with modes chosen at cast time, concatenate all chosen mode
        // descriptions (choose-N commands show every picked mode, in order, one per line).
        if (spellOnStack.chosenModes.isNotEmpty() && effect is ModalEffect) {
            val descriptions = chosenModeDescriptions(state, spellEntityId, spellOnStack, effect)
            if (descriptions.isNotEmpty()) {
                return descriptions.joinToString("\n")
            }
        }

        return try {
            val evaluator = conditionEvaluator.amounts
            val chosenTargets = state.getEntity(spellEntityId)
                ?.get<TargetsComponent>()
                ?.targets
                ?: emptyList()
            val context = EffectContext(
                sourceId = spellEntityId,
                controllerId = spellOnStack.casterId,
                xValue = spellOnStack.xValue,
                declaredCostSlot = spellOnStack.declaredCostSlot,
                wasBlightPaid = spellOnStack.wasBlightPaid,
                sacrificedPermanents = spellOnStack.sacrificedPermanents,
                discardedAsCostCards = spellOnStack.discardedAsCostCards,
                chosenEntitySnapshots = spellOnStack.chosenEntitySnapshots,
                exiledCardCount = spellOnStack.exiledCardCount,
                additionalCostBlightAmount = spellOnStack.additionalCostBlightAmount,
                targets = chosenTargets,
                pipeline = PipelineState(
                    storedCollections = buildBeheldStoredCollections(spellOnStack.beheldCards, cardDef)
                )
            )
            // Resolve Effects.If at stack-time: opponents see only the branch that
            // will fire (e.g., Cinder Strike shows "deals 4 damage" vs "deals 2 damage"
            // depending on whether the optional Blight cost was paid) instead of the full
            // "if X, do Y. Otherwise, do Z." description.
            val resolvedEffect = resolveConditionalForStack(state, effect, context)
            resolvedEffect.runtimeDescription { amount -> evaluator.evaluateForDisplay(state, amount, context) }
        } catch (_: Exception) {
            effect.description
        }
    }

    /**
     * Recursively replace [Effects.If]s in [effect] with the branch the spell will
     * actually take, using [context] to evaluate each condition. Composite branches are
     * resolved one level deep so nested conditions also collapse. Conditions that depend
     * on state not yet captured at cast time fall through to the original effect.
     */
    private fun resolveConditionalForStack(
        state: GameState,
        effect: Effect,
        context: EffectContext
    ): Effect {
        effect.asConditional()?.let { conditional ->
            val taken = if (conditionEvaluator.evaluate(state, conditional.condition, context)) {
                conditional.then
            } else {
                conditional.otherwise
            }
            return taken?.let { resolveConditionalForStack(state, it, context) }
                ?: CompositeEffect(emptyList())
        }
        return when (effect) {
            is CompositeEffect ->
                effect.copy(effects = effect.effects.map { resolveConditionalForStack(state, it, context) })
            else -> effect
        }
    }

    /**
     * Evaluate each chosen mode's runtime description for a modal spell on the stack. Aligned
     * 1:1 with [SpellOnStackComponent.chosenModes]; unknown indices yield "Unknown mode" so the
     * client still sees a placeholder rather than silently dropping entries.
     */
    fun chosenModeDescriptions(
        state: GameState,
        spellEntityId: EntityId,
        spellOnStack: SpellOnStackComponent,
        modal: ModalEffect
    ): List<String> {
        if (spellOnStack.chosenModes.isEmpty()) return emptyList()
        val context = EffectContext(
            sourceId = spellEntityId,
            controllerId = spellOnStack.casterId,
            xValue = spellOnStack.xValue,
            sacrificedPermanents = spellOnStack.sacrificedPermanents,
            discardedAsCostCards = spellOnStack.discardedAsCostCards,
            exiledCardCount = spellOnStack.exiledCardCount,
            additionalCostBlightAmount = spellOnStack.additionalCostBlightAmount
        )
        return modeDescriptions(state, modal, spellOnStack.chosenModes, context)
    }

    /**
     * The same per-mode breakdown for a triggered ability on the stack — spell copies carry the
     * original's chosenModes (700.2g) so the opponent can see the same per-mode text on the copy.
     */
    fun chosenModeDescriptions(
        state: GameState,
        abilityEntityId: EntityId,
        triggered: TriggeredAbilityOnStackComponent,
        modal: ModalEffect
    ): List<String> =
        modeDescriptions(state, modal, triggered.chosenModes, triggeredAbilityContext(state, abilityEntityId, triggered))

    private fun modeDescriptions(
        state: GameState,
        modal: ModalEffect,
        chosenModes: List<Int>,
        context: EffectContext
    ): List<String> {
        val evaluator = conditionEvaluator.amounts
        return chosenModes.map { modeIndex ->
            val mode = modal.modes.getOrNull(modeIndex) ?: return@map "Unknown mode"
            try {
                mode.effect.runtimeDescription { amount -> evaluator.evaluateForDisplay(state, amount, context) }
            } catch (_: Exception) {
                mode.description
            }
        }
    }

    /**
     * Build per-mode target groups for a modal spell on the stack, aligned with
     * [SpellOnStackComponent.modeTargetsOrdered]. Hidden-zone targets are redacted to a generic
     * "a card in X's hand/library" string, and a face-down object gets its generic public name,
     * whenever the shared identity authority says this viewer may not know it (see
     * [resolveTargetDisplayName]).
     */
    fun perModeTargetGroups(
        state: GameState,
        chosenModes: List<Int>,
        modeTargetsOrdered: List<List<ChosenTarget>>,
        modeDescriptions: List<String>,
        viewingPlayerId: EntityId,
        isSpectator: Boolean
    ): List<ClientPerModeTargetGroup> {
        if (chosenModes.isEmpty()) return emptyList()
        return chosenModes.mapIndexed { index, modeIndex ->
            val rawTargets = modeTargetsOrdered.getOrNull(index) ?: emptyList()
            val targetNames = rawTargets.map { target ->
                resolveTargetDisplayName(state, target, viewingPlayerId, isSpectator)
            }
            ClientPerModeTargetGroup(
                modeIndex = modeIndex,
                modeDescription = modeDescriptions.getOrNull(index) ?: "",
                targets = rawTargets.map(::toClientTarget),
                targetNames = targetNames
            )
        }
    }

    /**
     * Resolve a [ChosenTarget] to a human-readable display name for the stack view. The engine's
     * [Visibility] authority decides identity; this method owns only the client-facing placeholder.
     */
    private fun resolveTargetDisplayName(
        state: GameState,
        target: ChosenTarget,
        viewingPlayerId: EntityId,
        isSpectator: Boolean
    ): String = when (target) {
        is ChosenTarget.Player -> state.getEntity(target.playerId)
            ?.get<PlayerComponent>()?.name ?: "a player"
        is ChosenTarget.Permanent -> {
            if (visibility.isCardIdentityVisibleTo(
                    state,
                    Zone.BATTLEFIELD,
                    target.entityId,
                    viewingPlayerId,
                    isSpectator,
                )
            ) {
                state.getEntity(target.entityId)?.get<CardComponent>()?.name ?: "a permanent"
            } else {
                FACE_DOWN_DISPLAY_NAME
            }
        }
        is ChosenTarget.Spell -> {
            if (visibility.isCardIdentityVisibleTo(
                    state,
                    Zone.STACK,
                    target.spellEntityId,
                    viewingPlayerId,
                    isSpectator,
                )
            ) {
                state.getEntity(target.spellEntityId)?.get<CardComponent>()?.name ?: "a spell"
            } else {
                FACE_DOWN_DISPLAY_NAME
            }
        }
        is ChosenTarget.Card -> {
            val identityVisible = visibility.isCardIdentityVisibleTo(
                state,
                ZoneKey(target.ownerId, target.zone),
                target.cardId,
                viewingPlayerId,
                isSpectator,
            )
            if (identityVisible) {
                state.getEntity(target.cardId)?.get<CardComponent>()?.name ?: "a card"
            } else if (target.zone == Zone.HAND ||
                target.zone == Zone.LIBRARY ||
                target.zone == Zone.SIDEBOARD
            ) {
                val ownerName = state.getEntity(target.ownerId)
                    ?.get<PlayerComponent>()?.name ?: "opponent"
                "a card in ${ownerName}'s ${target.zone.name.lowercase()}"
            } else if (target.zone == Zone.EXILE) {
                FACE_DOWN_CARD_DISPLAY_NAME
            } else {
                "a card"
            }
        }
    }

    /**
     * Generate runtime text for an activated ability on the stack with dynamic amounts resolved.
     * Mirrors the cost-payment LKI carried on [ActivatedAbilityOnStackComponent] (sacrificed, discarded and
     * tapped permanent snapshots, X) into the [EffectContext] so stack text for effects like
     * "draw cards equal to the sacrificed creature's power" renders the actual number instead
     * of falling back to 0. Returns null if evaluation fails or the effect has no dynamic amounts.
     */
    fun runtimeAbilityText(
        state: GameState,
        abilityEntityId: EntityId,
        activated: ActivatedAbilityOnStackComponent
    ): String? {
        val context = EffectContext(
            sourceId = abilityEntityId,
            controllerId = activated.controllerId,
            targets = chosenTargetsOf(state, abilityEntityId),
            xValue = activated.xValue,
            sacrificedPermanents = activated.sacrificedPermanents,
            discardedAsCostCards = activated.discardedAsCostCards,
            tappedPermanents = activated.tappedPermanents,
            tappedEntitySnapshots = activated.tappedEntitySnapshots
        )
        return runtimeAbilityText(state, activated.effect, context)
    }

    /**
     * Generate runtime text for a triggered ability on the stack. Builds an [EffectContext] with
     * the triggering-entity fields populated so dynamic amounts referencing
     * [com.wingedsheep.sdk.scripting.targets.EffectTarget.TriggeringEntity] (e.g., "deals damage equal
     * to its power") render the correct value instead of falling back to 0.
     */
    fun runtimeAbilityText(
        state: GameState,
        abilityEntityId: EntityId,
        triggered: TriggeredAbilityOnStackComponent
    ): String? = runtimeAbilityText(
        state,
        triggered.effect,
        triggeredAbilityContext(state, abilityEntityId, triggered)
    )

    private fun runtimeAbilityText(state: GameState, effect: Effect, context: EffectContext): String? {
        return try {
            val evaluator = conditionEvaluator.amounts
            val text = effect.runtimeDescription { amount -> evaluator.evaluateForDisplay(state, amount, context) }
            // Only return if it differs from static description (i.e., dynamic amounts were resolved)
            if (text != effect.description) text else null
        } catch (_: Exception) {
            null
        }
    }

    /**
     * The targets already chosen for an ability sitting on the stack.
     *
     * An ability's targets are locked in when it's put on the stack, so text rendered for it can
     * read them — "double its power" on a 5/5 should say "+5/+5", not fall back to the amount's
     * wording. The spell path does the same thing; omitting it here left every
     * [com.wingedsheep.sdk.scripting.targets.EffectTarget.ContextTarget]-relative amount undeterminable
     * on the stack, where it is in fact known.
     */
    private fun chosenTargetsOf(state: GameState, abilityEntityId: EntityId) =
        state.getEntity(abilityEntityId)
            ?.get<TargetsComponent>()
            ?.targets
            ?: emptyList()

    /**
     * Build an [EffectContext] mirroring how [TriggerProcessor] does at resolution time, so
     * stack-text rendering can evaluate `EffectTarget.TriggeringEntity`-based dynamic amounts (e.g.,
     * "deals damage equal to its power") with the actual triggering entity instead of a null.
     */
    private fun triggeredAbilityContext(
        state: GameState,
        abilityEntityId: EntityId,
        triggered: TriggeredAbilityOnStackComponent
    ): EffectContext = EffectContext(
        sourceId = abilityEntityId,
        controllerId = triggered.controllerId,
        targets = chosenTargetsOf(state, abilityEntityId),
        xValue = triggered.xValue,
        triggerContext = triggered.triggerContext,
        triggeringEntityId = triggered.triggerContext?.triggeringEntityId,
        triggeringPlayerId = triggered.triggerContext?.triggeringPlayerId
    )

    /**
     * The noun a [com.wingedsheep.sdk.scripting.targets.SELF_NOUN_TOKEN] referring to [entityId]
     * should render as — "this creature" for a creature (incl. artifact/enchantment creatures), else
     * the noun matching its projected card type, falling back to [DEFAULT_SELF_NOUN]
     * ("this permanent") for a multi-type, DFC, or off-battlefield permanent whose type we can't pin
     * down. Battlefield type reads go through projected state (Rule 613), never base `typeLine`.
     */
    fun selfNounFor(state: GameState, entityId: EntityId): String {
        val types = state.projectedState.getTypes(entityId)
        return when {
            "CREATURE" in types -> "this creature"
            "LAND" in types -> "this land"
            "ARTIFACT" in types -> "this artifact"
            "ENCHANTMENT" in types -> "this enchantment"
            "PLANESWALKER" in types -> "this planeswalker"
            "BATTLE" in types -> "this battle"
            else -> DEFAULT_SELF_NOUN
        }
    }

    /**
     * Render [effect]'s display text with any self-noun placeholder resolved to [selfNoun] (the noun
     * for the source permanent — see [selfNounFor]). Effects that carry no self-reference fall
     * through to their default-resolved [Effect.description]. This is the type-aware render layer
     * for [SelfReferentialDescription]: the SDK emits the placeholder token, the client sees the
     * type-correct noun.
     */
    fun effectDisplayText(effect: Effect, selfNoun: String): String =
        (effect as? SelfReferentialDescription)
            ?.let { resolveSelfNoun(it.descriptionTemplate, selfNoun) }
            ?: effect.description

    companion object {
        /** The client's view of a chosen target (an id to draw an arrow to, nothing more). */
        fun toClientTarget(target: ChosenTarget): ClientChosenTarget = when (target) {
            is ChosenTarget.Player -> ClientChosenTarget.Player(target.playerId)
            is ChosenTarget.Permanent -> ClientChosenTarget.Permanent(target.entityId)
            is ChosenTarget.Spell -> ClientChosenTarget.Spell(target.spellEntityId)
            is ChosenTarget.Card -> ClientChosenTarget.Card(target.cardId)
        }

        /** All of [targetsComponent]'s targets as client targets, or none. */
        fun toClientTargets(targetsComponent: TargetsComponent?): List<ClientChosenTarget> =
            targetsComponent?.targets?.map(::toClientTarget) ?: emptyList()
    }
}
