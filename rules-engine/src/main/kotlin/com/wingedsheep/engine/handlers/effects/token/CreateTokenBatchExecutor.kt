package com.wingedsheep.engine.handlers.effects.token

import com.wingedsheep.engine.core.EffectContinuation
import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.core.GameLimits
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.handlers.DynamicAmountEvaluator
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.mechanics.layers.StaticAbilityHandler
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.identity.TokenComponent
import com.wingedsheep.sdk.scripting.effects.*
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import kotlin.reflect.KClass

/** The saved snapshot is also the scope marker carried through existing resolution continuations. */
internal object TokenCreationBatch {
    const val BEFORE = "__tokenCreationBatchBefore"
    const val SOURCES = "__tokenCreationBatchSources"
    fun sources(context: EffectContext): Set<com.wingedsheep.sdk.model.EntityId>? =
        context.pipeline.storedCollections[SOURCES]?.toSet()
    fun active(context: EffectContext): Boolean = BEFORE in context.pipeline.storedCollections
}

class CreateTokenBatchExecutor(
    private val executeEffect: (GameState, Effect, EffectContext) -> EffectResult,
    private val amountEvaluator: DynamicAmountEvaluator,
    private val cardRegistry: CardRegistry,
    private val staticAbilityHandler: StaticAbilityHandler,
) : EffectExecutor<CreateTokenBatchEffect> {
    override val effectType: KClass<CreateTokenBatchEffect> = CreateTokenBatchEffect::class

    /** Read only for the would-be tokens a batch-wide substitution question tests. */
    private val creatureTokens by lazy { CreateTokenExecutor(amountEvaluator, staticAbilityHandler, cardRegistry) }
    private val predefinedTokens by lazy { CreatePredefinedTokenExecutor(cardRegistry, staticAbilityHandler, amountEvaluator) }

    override fun execute(state: GameState, effect: CreateTokenBatchEffect, context: EffectContext): EffectResult {
        if (effect.groups.isEmpty()) {
            if (!TokenCreationBatch.active(context)) return EffectResult.success(state)
            val before = context.pipeline.storedCollections.getValue(TokenCreationBatch.BEFORE).toHashSet()
            val created = state.getBattlefield().filter {
                it !in before && state.getEntity(it)?.has<TokenComponent>() == true
            }
            val (after, events) = TokenCreationReplacementHelper.applyAdditionalTokenReplacements(
                state, context.controllerId, created,
                originalTapped = created.isNotEmpty() && created.all { state.getEntity(it)?.has<TappedComponent>() == true },
                cardRegistry, staticAbilityHandler, amountEvaluator.predicates,
                eligibleSources = TokenCreationBatch.sources(context),
            )
            return EffectResult(after, events, updatedCollections = mapOf(CREATED_TOKENS to created))
        }
        check(!TokenCreationBatch.active(context)) { "Token creation batches cannot nest" }
        val controller = effect.controller?.let { context.resolvePlayerTarget(it, state) }
            ?: if (effect.controller == null) context.controllerId else return EffectResult.success(state)
        val counts = effect.groups.map {
            when (it) {
                is CreateTokenEffect -> amountEvaluator.evaluate(state, it.count, context).coerceAtLeast(0)
                is CreatePredefinedTokenEffect -> it.dynamicCount?.let { amount ->
                    amountEvaluator.evaluate(state, amount, context).coerceAtLeast(0)
                } ?: it.count.coerceAtLeast(0)
                else -> error("Unsupported token group")
            }
        }
        val baseCount = counts.fold(0) { total, count -> GameLimits.addClamped(total, count) }
        if (baseCount == 0) return EffectResult.success(state)
        val adjustedCounts = TokenCreationReplacementHelper.applyBatchCountReplacements(
            state, controller, counts, amountEvaluator.predicates,
        )
        val totalCount = GameLimits.cappedTokenCount(
            adjustedCounts.fold(0) { total, count -> GameLimits.addClamped(total, count) }, "token batch",
        )
        var capacity = totalCount
        val groups = effect.groups.mapIndexed { index, group ->
            val count = minOf(adjustedCounts[index], capacity)
            capacity -= count
            when (group) {
                is CreateTokenEffect -> group.copy(
                    count = DynamicAmount.Fixed(count),
                    dynamicPower = group.dynamicPower?.let { DynamicAmount.Fixed(amountEvaluator.evaluate(state, it, context)) },
                    dynamicToughness = group.dynamicToughness?.let { DynamicAmount.Fixed(amountEvaluator.evaluate(state, it, context)) },
                )
                is CreatePredefinedTokenEffect -> group.copy(count = count, dynamicCount = null)
                else -> error("Unsupported token group")
            }
        }
        // A batch is one creation event, so a substitution that asks (Jinnie Fay's Cats or Dogs)
        // asks once for all of it, before any group is created; every group then reads the answer.
        if (totalCount > 0) {
            for (group in groups) {
                val prospective = when (group) {
                    is CreateTokenEffect -> creatureTokens.identity(state, group, context).prospective(group, controller)
                    is CreatePredefinedTokenEffect -> cardRegistry.getCard(group.tokenType)
                        ?.let { predefinedTokens.prospectiveToken(it, group, controller) }
                    else -> null
                } ?: continue
                val lookup = TokenCreationReplacementHelper.findTokenSubstitution(
                    state, controller, prospective, amountEvaluator.predicates,
                    choices = context.pipeline.chosenValues,
                )
                if (lookup is TokenSubstitutionChoice) {
                    return TokenCreationReplacementHelper.askForSubstitution(state, lookup, effect, context, totalCount)
                }
            }
        }
        val batchContext = context.copy(controllerId = controller, pipeline = context.pipeline.copy(
            storedCollections = context.pipeline.storedCollections + mapOf(
                TokenCreationBatch.BEFORE to state.getBattlefield(),
                TokenCreationBatch.SOURCES to com.wingedsheep.engine.replacement.ActiveReplacements.all(state).map { it.sourceId }.distinct(),
            ),
        ))
        val finish = CreateTokenBatchEffect(emptyList())
        // The finalizer is below a replacement decision. Both accepting the copy replacement and
        // declining it reach that same finalizer, including after serialization of a paused game.
        val pendingFinish = state.pushContinuation(EffectContinuation(listOf(finish), batchContext))
        val replacement = TokenCreationReplacementHelper.checkReplacement(
            pendingFinish, CompositeEffect(groups), batchContext, totalCount, controller,
            cardRegistry, staticAbilityHandler, amountEvaluator.predicates,
        )
        if (replacement != null) {
            if (replacement.outcome is Outcome.Paused) return replacement
            val (_, clean) = replacement.state.popContinuation()
            val finalized = execute(clean, finish, batchContext)
            return finalized.copy(events = replacement.events + finalized.events)
        }
        return executeEffect(state, CompositeEffect(groups + finish), batchContext)
    }
}
