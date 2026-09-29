package com.wingedsheep.engine.handlers.effects.token

import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.event.DelayedTriggeredAbility
import com.wingedsheep.engine.handlers.DynamicAmountEvaluator
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.handlers.effects.EnterTappedReplacements
import com.wingedsheep.engine.handlers.effects.EntersWithReplacements
import com.wingedsheep.engine.state.Component
import com.wingedsheep.engine.state.ComponentContainer
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.battlefield.EnteredThisTurnComponent
import com.wingedsheep.engine.state.components.battlefield.SummoningSicknessComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.CastChoicesComponent
import com.wingedsheep.engine.state.components.battlefield.ChoiceValue
import com.wingedsheep.engine.state.components.combat.AttackingComponent
import com.wingedsheep.engine.state.components.identity.TokenComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.TypeLine
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.CreatureStats
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.engine.core.ZoneChangeEvent
import com.wingedsheep.engine.event.GrantedActivatedAbility
import com.wingedsheep.engine.event.GrantedStaticAbility
import com.wingedsheep.engine.event.GrantedTriggeredAbility
import com.wingedsheep.engine.mechanics.layers.StaticAbilityHandler
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.registry.TokenArtRegistry
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.scripting.AbilityId
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.TriggeredAbility
import com.wingedsheep.sdk.scripting.effects.CREATED_TOKENS
import com.wingedsheep.sdk.scripting.effects.CreateTokenEffect
import com.wingedsheep.sdk.scripting.effects.ModifyStatsEffect
import com.wingedsheep.sdk.scripting.effects.MoveToZoneEffect
import com.wingedsheep.sdk.scripting.effects.SacrificeTargetEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import kotlin.reflect.KClass
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Executor for CreateTokenEffect.
 * "Create a 1/1 white Soldier creature token" or "Create X 1/1 green Insect creature tokens"
 *
 * Supports both fixed and dynamic counts via [DynamicAmountEvaluator].
 */
class CreateTokenExecutor(
    private val amountEvaluator: DynamicAmountEvaluator,
    private val staticAbilityHandler: StaticAbilityHandler? = null,
    private val cardRegistry: CardRegistry? = null,
    private val tokenArtRegistry: TokenArtRegistry? = null
) : EffectExecutor<CreateTokenEffect> {

    override val effectType: KClass<CreateTokenEffect> = CreateTokenEffect::class

    override fun execute(
        state: GameState,
        effect: CreateTokenEffect,
        context: EffectContext
    ): EffectResult {
        val baseCount = amountEvaluator.evaluate(state, effect.count, context)
        if (baseCount <= 0) return EffectResult.success(state)

        // Resolve who receives the token — defaults to the spell/ability controller. A
        // multi-player reference ("each opponent creates ...") fans out: every resolved
        // player creates their own token(s).
        val tokenControllerIds = effect.controller
            ?.let { context.resolvePlayerTargets(it, state) }
            ?.takeIf { it.isNotEmpty() }
            ?: listOf(context.controllerId)

        var currentState = state
        val allEvents = mutableListOf<com.wingedsheep.engine.core.GameEvent>()
        val allCreatedTokens = mutableListOf<EntityId>()
        for (tokenControllerId in tokenControllerIds) {
            val result = createTokensFor(currentState, effect, context, baseCount, tokenControllerId)
            if (result.error != null) return result
            currentState = result.state
            allEvents.addAll(result.events)
            allCreatedTokens.addAll(result.updatedCollections[CREATED_TOKENS].orEmpty())
        }
        return EffectResult(
            state = currentState,
            events = allEvents,
            updatedCollections = mapOf(CREATED_TOKENS to allCreatedTokens)
        )
    }

    /**
     * Create [count] tokens of [substitute] under [tokenControllerId]'s control *in place of* tokens
     * a [com.wingedsheep.sdk.scripting.ReplaceTokenCreationWithToken] replaced ("that many 5/5 red
     * Dragon creature tokens with flying are created instead" — Draconic Visitor). Shared by every
     * token-creation executor that reads the substitution, so the substitute tokens are built by
     * the one creature-token path.
     *
     * [count] is already final — a doubler scaled the replaced event before the substitution — so
     * no count replacement runs again, and the substitution isn't re-checked (no loop). The
     * substitute's own `count`, `controller`, `tapped` and `attacking` are ignored: "that many",
     * under the player the replaced tokens were for, with none of the replaced effect's riders.
     */
    internal fun createSubstituteTokens(
        state: GameState,
        substitute: CreateTokenEffect,
        context: EffectContext,
        count: Int,
        tokenControllerId: EntityId
    ): EffectResult {
        if (count <= 0) return EffectResult.success(state)
        val normalized = substitute.copy(
            count = com.wingedsheep.sdk.scripting.values.DynamicAmount.Fixed(count),
            controller = null,
            tapped = false,
            attacking = false
        )
        return createTokensFor(state, normalized, context, count, tokenControllerId, substituted = true)
    }

    /**
     * Create [baseCount] tokens (pre-replacement) under [tokenControllerId]'s control.
     *
     * @param substituted true when these tokens are themselves the substitute of a
     *   [com.wingedsheep.sdk.scripting.ReplaceTokenCreationWithToken] — see [createSubstituteTokens].
     */
    private fun createTokensFor(
        state: GameState,
        effect: CreateTokenEffect,
        context: EffectContext,
        baseCount: Int,
        tokenControllerId: EntityId,
        substituted: Boolean = false
    ): EffectResult {
        // Apply token-count replacements (Doubling Season / Exalted Sunborn,
        // and per-N modifiers) before downstream replacements get a look.
        val requestedCount = if (substituted) baseCount else TokenCreationReplacementHelper.applyCountReplacements(
            state, tokenControllerId, baseCount,
            predicateEvaluator = amountEvaluator.predicates
        )
        if (requestedCount <= 0) return EffectResult.success(state)

        // Structural cap: each token is a full ECS entity, so an unbounded doubler stack would
        // allocate entities until the JVM OOMs. Clamp before the allocation loop — a board this
        // large is already a decided game. See GameLimits.MAX_TOKENS_PER_EFFECT.
        val count = com.wingedsheep.engine.core.GameLimits.cappedTokenCount(requestedCount, "tokens")

        // Check for token creation replacement effects (e.g., Mirrormind Crown)
        val replacementResult = TokenCreationReplacementHelper.checkReplacement(
            state, effect, context, count, tokenControllerId, cardRegistry, staticAbilityHandler,
            predicateEvaluator = amountEvaluator.predicates
        )
        if (replacementResult != null) return replacementResult

        // Resolve the token's color / creature type from the source's cast-choices bag when the
        // effect sources them from a ChoiceSlot (Riptide Replicator "of the chosen color and type");
        // otherwise use the fixed sets baked into the effect. Both slots live on the one source bag.
        val sourceBag = (effect.colorsFromChoice ?: effect.creatureTypesFromChoice)
            ?.let { context.sourceId }
            ?.let { state.getEntity(it) }
            ?.get<CastChoicesComponent>()
        // Defensive fallbacks for a malformed state (a *FromChoice slot declared but never written):
        // colorless for color, generic "Creature" for type — a token is always created.
        val effectiveColors = effect.colorsFromChoice?.let { slot ->
            (sourceBag?.chosen?.get(slot) as? ChoiceValue.ColorChoice)?.color?.let { setOf(it) } ?: emptySet()
        } ?: effect.colors
        val effectiveCreatureTypes = effect.creatureTypesFromChoice?.let { slot ->
            (sourceBag?.chosen?.get(slot) as? ChoiceValue.TextChoice)?.text?.let { setOf(it) } ?: setOf("Creature")
        } ?: effect.creatureTypes
        // The token's identity is the same for every copy in this batch, so resolve it once rather
        // than per iteration. Its art is the one thing that can differ between copies — see below.
        val defaultName = "${effectiveCreatureTypes.joinToString(" ")} Token"
        val tokenName = effect.name ?: defaultName
        val tokenPower = effect.dynamicPower?.let { amountEvaluator.evaluate(state, it, context) } ?: effect.power
        val tokenToughness = effect.dynamicToughness?.let { amountEvaluator.evaluate(state, it, context) } ?: effect.toughness
        val typeLinePrefix = buildString {
            if (effect.legendary) append("Legendary ")
            if (effect.artifactToken) append("Artifact ")
            if (effect.enchantmentToken) append("Enchantment ")
            append("Creature")
        }
        val tokenTypeLine = TypeLine.parse("$typeLinePrefix - ${effectiveCreatureTypes.joinToString(" ")}")
        val tokenCardDefinitionId = "token:${effectiveCreatureTypes.joinToString("-")}"

        // "If one or more artifact tokens would be created under your control, that many 5/5 red
        // Dragon creature tokens with flying are created instead" (Draconic Visitor): swap the whole
        // batch for the substitute before any of it is created.
        if (!substituted) {
            val prospective = CardComponent(
                cardDefinitionId = tokenCardDefinitionId,
                name = tokenName,
                manaCost = ManaCost.ZERO,
                typeLine = tokenTypeLine,
                baseStats = CreatureStats(tokenPower, tokenToughness),
                baseKeywords = effect.keywords,
                colors = effectiveColors,
                ownerId = tokenControllerId
            )
            TokenCreationReplacementHelper.findTokenSubstitution(state, tokenControllerId, prospective, predicateEvaluator = amountEvaluator.predicates)
                ?.let { return createSubstituteTokens(state, it, context, count, tokenControllerId) }
        }

        // Art: an explicit per-card override wins, then the art printed by the set the creating
        // card came from (so a reprint mints its own set's token), then the engine-wide generic
        // art for the creature type. A token always ends up with *some* image — TokenArtCoverageTest
        // holds that line across the whole card corpus.
        //
        // A set that printed one token with several illustrations contributes a row per art, so
        // this is a list: the batch is dealt out of it in order and wraps, which is why Release the
        // Dogs' four Dogs show Jumpstart's four Dog arts. Indexing by position in the batch keeps
        // it deterministic, so a replay re-simulates the same board.
        val sourceCard = context.sourceId
            ?.let { state.getEntity(it) }
            ?.get<CardComponent>()
        val resolvedImageUris = effect.imageUri?.let(::listOf)
            ?: tokenArtRegistry?.resolveAll(
                sourceCardDefinitionId = sourceCard?.cardDefinitionId,
                tokenName = tokenName.removeSuffix(" Token"),
                power = tokenPower,
                toughness = tokenToughness,
                colors = effectiveColors,
                sourcePrintingSetCode = sourceCard?.printingSetCode,
            )?.takeIf { it.isNotEmpty() }
            ?: listOf(TokenArt.forCreatureTypes(effectiveCreatureTypes))

        var newState = state
        val createdTokens = mutableListOf<EntityId>()

        repeat(count) { indexInBatch ->
            val resolvedImageUri = resolvedImageUris[indexInBatch % resolvedImageUris.size]
            val (tokenId, stateWithId) = newState.newEntity()
            newState = stateWithId
            createdTokens.add(tokenId)

            val tokenComponent = CardComponent(
                cardDefinitionId = tokenCardDefinitionId,
                name = tokenName,
                manaCost = ManaCost.ZERO,
                typeLine = tokenTypeLine,
                baseStats = CreatureStats(tokenPower, tokenToughness),
                baseKeywords = effect.keywords,
                colors = effectiveColors,
                ownerId = tokenControllerId,
                imageUri = resolvedImageUri
            )

            val components = mutableListOf<Component>(
                tokenComponent,
                TokenComponent,
                ControllerComponent(tokenControllerId),
                SummoningSicknessComponent,
                EnteredThisTurnComponent
            )
            if (effect.tapped) {
                components.add(TappedComponent)
            }
            // Provenance: record the creating permanent so "tokens created with this creature"
            // (StatePredicate.CreatedBySource) can recognize them later (Tetravus).
            if (effect.stampCreator) {
                context.sourceId?.let { creatorId ->
                    components.add(
                        com.wingedsheep.engine.state.components.identity.CreatedByComponent(creatorId)
                    )
                }
            }
            if (effect.attacking) {
                // Token enters attacking — it joins the attack of the source creature
                // (CR 802.2a: defender per attacking creature), falling back to the sole
                // active opponent outside combat-derived contexts.
                val defenderId = com.wingedsheep.engine.handlers.effects.TargetResolutionUtils
                    .resolveDefendingPlayer(context, newState)
                    ?: newState.getOpponents(tokenControllerId).firstOrNull()
                if (defenderId != null) {
                    components.add(AttackingComponent(defenderId))
                }
            }
            var container = ComponentContainer.of(*components.toTypedArray())
            // "with toxic 1" — the token's printed numeric keywords ride the same components a
            // card's do, so combat damage and "creatures with toxic" read them.
            container = com.wingedsheep.engine.core.CardEntityFactory
                .applyNumericKeywords(container, effect.numericKeywords)
            if (effect.staticAbilities.isNotEmpty() && staticAbilityHandler != null) {
                container = staticAbilityHandler.addContinuousEffectComponentFromAbilities(
                    container, effect.staticAbilities
                )
            }
            if (effect.initialCounters.isNotEmpty()) {
                var counters = CountersComponent()
                // A token created "with a +1/+1 counter on it" is given those counters as it
                // enters, which CR 122.6 counts as counters being *put on* it, and CR 122.6a makes
                // its controller the player putting them there. Stamp the same per-permanent
                // history marker the ordinary placement paths do, so counter-history readings
                // (Kid Loki's "you've put one or more +1/+1 counters on this turn") see the token
                // and so a later placement on it reports firstThisTurn = false. No
                // CountersAddedEvent is emitted: the counters arrive as part of the token's
                // creation, not as a separate placement event.
                var history = com.wingedsheep.engine.state.components.battlefield
                    .ReceivedCountersThisTurnComponent()
                for ((counterType, amount) in effect.initialCounters) {
                    counters = counters.withAdded(counterType, amount)
                    if (amount > 0) {
                        history = history.with(
                            counterType,
                            byController = true,
                        )
                    }
                }
                container = container.with(counters)
                if (history.counterTypes.isNotEmpty()) container = container.with(history)
            }

            newState = newState.withEntity(tokenId, container)

            // Add to battlefield
            newState = com.wingedsheep.engine.handlers.effects.BattlefieldEntry
                .place(newState, tokenControllerId, tokenId)

            // Tokens honor global "[filter] enter tapped" replacements from other permanents
            // (Dauntless Dismantler, Authority of the Consuls, …) — BattlefieldEntry.place doesn't
            // set tapped state, so resolve it here now the token carries its controller/type.
            newState = EnterTappedReplacements.applyCreatedTokenEntryTap(
                newState, tokenId, tokenControllerId,
                definedTapped = effect.tapped, attacking = effect.attacking,
                predicateEvaluator = amountEvaluator.predicates
            )
        }

        // Apply "enters with counters" replacement effects from other battlefield permanents
        // (e.g., Gev, Scaled Scorch granting +1/+1 counters to tokens entering under your control).
        val counterEvents = mutableListOf<com.wingedsheep.engine.core.GameEvent>()
        for (tokenId in createdTokens) {
            val (nextState, events) = EntersWithReplacements.applyGlobal(
                newState, tokenId, tokenControllerId, cardRegistry,
                predicateEvaluator = amountEvaluator.predicates
            )
            newState = nextState
            counterEvents.addAll(events)
        }

        // If exileAtStep is set, create delayed triggers to exile each created token
        val exileStep = effect.exileAtStep
        if (exileStep != null) {
            val sourceId = context.sourceId ?: context.controllerId
            val sourceName = sourceId.let { id ->
                state.getEntity(id)?.get<CardComponent>()?.name ?: "Unknown"
            }
            for (tokenId in createdTokens) {
                val (delayedTriggerId, stateWithRoutingId) = newState.newRoutingId()
                newState = stateWithRoutingId
                val delayedTrigger = DelayedTriggeredAbility(
                    id = delayedTriggerId,
                    effect = MoveToZoneEffect(EffectTarget.SpecificEntity(tokenId), Zone.EXILE),
                    fireAtStep = exileStep,
                    sourceId = sourceId,
                    objectReferences = context.objectReferences,
                    sourceName = sourceName,
                    controllerId = tokenControllerId
                )
                newState = newState.addDelayedTrigger(delayedTrigger)
            }
        }

        // If sacrificeAtStep is set, create delayed triggers to sacrifice each created token
        // (the sacrifice sibling of exileAtStep — used by Mobilize N). Sacrifice sends the
        // token to the graveyard, firing dies/leaves and "whenever you sacrifice" triggers.
        val sacrificeStep = effect.sacrificeAtStep
        if (sacrificeStep != null) {
            val sourceId = context.sourceId ?: context.controllerId
            val sourceName = sourceId.let { id ->
                state.getEntity(id)?.get<CardComponent>()?.name ?: "Unknown"
            }
            for (tokenId in createdTokens) {
                val (delayedTriggerId, stateWithRoutingId) = newState.newRoutingId()
                newState = stateWithRoutingId
                val delayedTrigger = DelayedTriggeredAbility(
                    id = delayedTriggerId,
                    effect = SacrificeTargetEffect(EffectTarget.SpecificEntity(tokenId)),
                    fireAtStep = sacrificeStep,
                    sourceId = sourceId,
                    objectReferences = context.objectReferences,
                    sourceName = sourceName,
                    controllerId = tokenControllerId
                )
                newState = newState.addDelayedTrigger(delayedTrigger)
            }
        }

        // If triggered abilities are specified, grant them permanently to each created token
        if (effect.triggeredAbilities.isNotEmpty()) {
            for (tokenId in createdTokens) {
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
            }
        }

        // If activated abilities are specified, grant them permanently to each created token.
        // Mirrors the triggered-ability path: tokens have no CardDefinition, so their activated
        // abilities live in GameState.grantedActivatedAbilities, which the legal-action
        // enumerator and ActivateAbilityHandler already consult for any entity. Models e.g.
        // Mourner's Surprise's Mercenary token with "{T}: Target creature you control gets +1/+0...".
        if (effect.activatedAbilities.isNotEmpty()) {
            for (tokenId in createdTokens) {
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
            }
        }

        // Static abilities also live on the token entity for the layer system
        // (ContinuousEffectSourceComponent, attached above). But combat-restriction statics like
        // CantAttackUnlessCoAttacker / CantBlockUnlessCoBlocker are read directly by the combat
        // legality managers from the card definition or grantedStaticAbilities — never projected —
        // and a token has no CardDefinition. So grant them per-token here, mirroring the triggered/
        // activated-ability paths above, so those managers can find a token's "can't attack/block
        // alone"-style restrictions (Toby's Beast token).
        if (effect.staticAbilities.isNotEmpty()) {
            for (tokenId in createdTokens) {
                for (ability in effect.staticAbilities) {
                    val grant = GrantedStaticAbility(
                        entityId = tokenId,
                        ability = ability,
                        duration = Duration.Permanent
                    )
                    newState = newState.copy(
                        grantedStaticAbilities = newState.grantedStaticAbilities + grant
                    )
                }
            }
        }

        // Prowess is a keyword ability with an intrinsic triggered ability.
        // Grant it automatically when the token has the PROWESS keyword.
        if (Keyword.PROWESS in effect.keywords) {
            val prowessAbility = TriggeredAbility.create(
                id = AbilityId("prowess"),
                trigger = Triggers.you.casts(GameObjectFilter.Noncreature),
                effect = ModifyStatsEffect(
                    powerModifier = 1,
                    toughnessModifier = 1,
                    target = EffectTarget.Self
                )
            )
            for (tokenId in createdTokens) {
                val grant = GrantedTriggeredAbility(
                    entityId = tokenId,
                    ability = prowessAbility,
                    duration = Duration.Permanent
                )
                newState = newState.copy(
                    grantedTriggeredAbilities = newState.grantedTriggeredAbilities + grant
                )
            }
        }

        val events = createdTokens.map { tokenId ->
            val entity = newState.getEntity(tokenId)!!
            val card = entity.get<CardComponent>()!!
            ZoneChangeEvent(
                entityId = tokenId,
                entityName = card.name,
                fromZone = null,
                toZone = Zone.BATTLEFIELD,
                ownerId = tokenControllerId,
                oldObject = null,
                newObject = newState.objectRef(tokenId)
            )
        }

        // Apply "create those tokens plus an additional X token" replacements (Worldwalker
        // Helm) once for this batch. Only the just-created tokens are matched against the
        // filter, so an added artifact token can't recursively re-trigger.
        val (afterAdditional, additionalEvents) = TokenCreationReplacementHelper
            .applyAdditionalTokenReplacements(
                newState, tokenControllerId, createdTokens, effect.tapped,
                cardRegistry, staticAbilityHandler, amountEvaluator.predicates
            )
        newState = afterAdditional

        // Publish the freshly-created token entity IDs to the pipeline so sibling effects in a
        // CompositeEffect can address them via EffectTarget.PipelineTarget(CREATED_TOKENS, index).
        // Mirrors CreatePredefinedTokenExecutor — lets a composite grant keywords/counters to the
        // tokens it just made (e.g. Mardu Monument's "create three Warriors; they gain menace and
        // haste until end of turn").
        return EffectResult(
            state = newState,
            events = events + counterEvents + additionalEvents,
            updatedCollections = mapOf(CREATED_TOKENS to createdTokens)
        )
    }
}
