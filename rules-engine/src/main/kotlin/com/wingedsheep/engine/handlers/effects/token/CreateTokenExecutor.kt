package com.wingedsheep.engine.handlers.effects.token

import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.event.DelayedTriggeredAbility
import com.wingedsheep.engine.handlers.DynamicAmountEvaluator
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.handlers.effects.EntersWithCountersHelper
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
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.TypeLine
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.CreatureStats
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.engine.core.ZoneChangeEvent
import com.wingedsheep.engine.event.GrantedActivatedAbility
import com.wingedsheep.engine.event.GrantedTriggeredAbility
import com.wingedsheep.engine.mechanics.layers.StaticAbilityHandler
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.TriggeredAbility
import com.wingedsheep.sdk.scripting.effects.CREATED_TOKENS
import com.wingedsheep.sdk.scripting.effects.CreateTokenEffect
import com.wingedsheep.sdk.scripting.effects.ModifyStatsEffect
import com.wingedsheep.sdk.scripting.effects.MoveToZoneEffect
import com.wingedsheep.sdk.scripting.effects.SacrificeTargetEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import java.util.UUID
import kotlin.reflect.KClass

/**
 * Executor for CreateTokenEffect.
 * "Create a 1/1 white Soldier creature token" or "Create X 1/1 green Insect creature tokens"
 *
 * Supports both fixed and dynamic counts via [DynamicAmountEvaluator].
 */
class CreateTokenExecutor(
    private val amountEvaluator: DynamicAmountEvaluator = DynamicAmountEvaluator(),
    private val staticAbilityHandler: StaticAbilityHandler? = null,
    private val cardRegistry: CardRegistry? = null
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

    /** Create [baseCount] tokens (pre-replacement) under [tokenControllerId]'s control. */
    private fun createTokensFor(
        state: GameState,
        effect: CreateTokenEffect,
        context: EffectContext,
        baseCount: Int,
        tokenControllerId: EntityId
    ): EffectResult {
        // Apply token-count replacements (Doubling Season / Exalted Sunborn,
        // and per-N modifiers) before downstream replacements get a look.
        val requestedCount = TokenCreationReplacementHelper.applyCountReplacements(
            state, tokenControllerId, baseCount
        )
        if (requestedCount <= 0) return EffectResult.success(state)

        // Structural cap: each token is a full ECS entity, so an unbounded doubler stack would
        // allocate entities until the JVM OOMs. Clamp before the allocation loop — a board this
        // large is already a decided game. See GameLimits.MAX_TOKENS_PER_EFFECT.
        val count = com.wingedsheep.engine.core.GameLimits.cappedTokenCount(requestedCount, "tokens")

        // Check for token creation replacement effects (e.g., Mirrormind Crown)
        val replacementResult = TokenCreationReplacementHelper.checkReplacement(
            state, effect, context, count, tokenControllerId, cardRegistry, staticAbilityHandler
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
        val resolvedImageUri = effect.imageUri
            ?: if (effect.creatureTypesFromChoice != null) {
                effectiveCreatureTypes.firstNotNullOfOrNull { TokenArt.IMAGES[it] }
            } else null

        var newState = state
        val createdTokens = mutableListOf<EntityId>()

        repeat(count) {
            val (tokenId, stateWithId) = newState.newEntity()
            newState = stateWithId
            createdTokens.add(tokenId)

            // Create token entity
            val defaultName = "${effectiveCreatureTypes.joinToString(" ")} Token"
            val tokenName = effect.name ?: defaultName
            val tokenPower = effect.dynamicPower?.let { amountEvaluator.evaluate(state, it, context) } ?: effect.power
            val tokenToughness = effect.dynamicToughness?.let { amountEvaluator.evaluate(state, it, context) } ?: effect.toughness

            val typeLinePrefix = buildString {
                if (effect.legendary) append("Legendary ")
                if (effect.artifactToken) append("Artifact ")
                append("Creature")
            }
            val tokenComponent = CardComponent(
                cardDefinitionId = "token:${effectiveCreatureTypes.joinToString("-")}",
                name = tokenName,
                manaCost = ManaCost.ZERO,
                typeLine = TypeLine.parse("$typeLinePrefix - ${effectiveCreatureTypes.joinToString(" ")}"),
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
            if (effect.staticAbilities.isNotEmpty() && staticAbilityHandler != null) {
                container = staticAbilityHandler.addContinuousEffectComponentFromAbilities(
                    container, effect.staticAbilities
                )
            }
            if (effect.initialCounters.isNotEmpty()) {
                var counters = CountersComponent()
                for ((counterTypeStr, amount) in effect.initialCounters) {
                    val counterType = try {
                        CounterType.valueOf(
                            counterTypeStr.uppercase()
                                .replace(' ', '_')
                                .replace('+', 'P')
                                .replace('-', 'M')
                                .replace("/", "_")
                        )
                    } catch (e: IllegalArgumentException) {
                        CounterType.PLUS_ONE_PLUS_ONE
                    }
                    counters = counters.withAdded(counterType, amount)
                }
                container = container.with(counters)
            }

            newState = newState.withEntity(tokenId, container)

            // Add to battlefield
            newState = com.wingedsheep.engine.handlers.effects.BattlefieldEntry
                .place(newState, tokenControllerId, tokenId)
        }

        // Apply "enters with counters" replacement effects from other battlefield permanents
        // (e.g., Gev, Scaled Scorch granting +1/+1 counters to tokens entering under your control).
        val counterEvents = mutableListOf<com.wingedsheep.engine.core.GameEvent>()
        for (tokenId in createdTokens) {
            val (nextState, events) = EntersWithCountersHelper.applyGlobalEntersWithCounters(
                newState, tokenId, tokenControllerId
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
                val delayedTrigger = DelayedTriggeredAbility(
                    id = UUID.randomUUID().toString(),
                    effect = MoveToZoneEffect(EffectTarget.SpecificEntity(tokenId), Zone.EXILE),
                    fireAtStep = exileStep,
                    sourceId = sourceId,
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
                val delayedTrigger = DelayedTriggeredAbility(
                    id = UUID.randomUUID().toString(),
                    effect = SacrificeTargetEffect(EffectTarget.SpecificEntity(tokenId)),
                    fireAtStep = sacrificeStep,
                    sourceId = sourceId,
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

        // Prowess is a keyword ability with an intrinsic triggered ability.
        // Grant it automatically when the token has the PROWESS keyword.
        if (Keyword.PROWESS in effect.keywords) {
            val prowessAbility = TriggeredAbility.create(
                trigger = Triggers.YouCastNoncreature.event,
                binding = Triggers.YouCastNoncreature.binding,
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
                ownerId = tokenControllerId
            )
        }

        // Apply "create those tokens plus an additional X token" replacements (Worldwalker
        // Helm) once for this batch. Only the just-created tokens are matched against the
        // filter, so an added artifact token can't recursively re-trigger.
        val (afterAdditional, additionalEvents) = TokenCreationReplacementHelper
            .applyAdditionalTokenReplacements(
                newState, tokenControllerId, createdTokens, effect.tapped,
                cardRegistry, staticAbilityHandler
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
