package com.wingedsheep.engine.core

import com.wingedsheep.engine.event.TriggerDetector
import com.wingedsheep.engine.event.TriggerProcessor
import com.wingedsheep.engine.handlers.ConditionEvaluator
import com.wingedsheep.engine.handlers.ContinuationHandler
import com.wingedsheep.engine.handlers.CostHandler
import com.wingedsheep.engine.handlers.EffectHandler
import com.wingedsheep.engine.handlers.MulliganHandler
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.handlers.TargetFinder
import com.wingedsheep.engine.replacement.ReplacementEffectProcessor
import com.wingedsheep.engine.legalactions.utils.CastPermissionUtils
import com.wingedsheep.engine.handlers.effects.DamageUtils
import com.wingedsheep.engine.handlers.effects.EffectExecutorRegistry
import com.wingedsheep.engine.handlers.effects.ZoneTransitionService
import com.wingedsheep.engine.mechanics.StateBasedActionChecker
import com.wingedsheep.engine.mechanics.layers.StaticAbilityHandler
import com.wingedsheep.engine.mechanics.combat.CombatManager
import com.wingedsheep.engine.mechanics.mana.AlternativePaymentHandler
import com.wingedsheep.engine.mechanics.mana.CostCalculator
import com.wingedsheep.engine.mechanics.mana.GrantedKeywordResolver
import com.wingedsheep.engine.mechanics.mana.ManaAbilitySideEffectExecutor
import com.wingedsheep.engine.mechanics.mana.ManaSolver
import com.wingedsheep.engine.mechanics.stack.StackResolver
import com.wingedsheep.engine.mechanics.targeting.TargetValidator
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.registry.PrintingRegistry
import com.wingedsheep.engine.registry.TokenArtRegistry

/**
 * Composition root for the rules engine.
 *
 * Constructs and wires all engine services from a single [CardRegistry].
 * This eliminates duplicated wiring across ActionProcessor and GameSession,
 * and ensures all consumers share the same service instances.
 */
class EngineServices(
    val cardRegistry: CardRegistry,
    /**
     * Optional per-printing registry. Threaded into [GameInitializer] so deck entries with
     * pinned printings can override per-entity art at game-init. Null is fine — every
     * lookup is null-safe.
     */
    val printingRegistry: PrintingRegistry? = null,
    /**
     * Optional per-set token art. Threaded into the token executors so a created token shows the
     * art printed by the set of the card that created it. Null is fine — tokens then fall back to
     * the engine-wide generic art for their creature type.
     */
    val tokenArtRegistry: TokenArtRegistry? = null,
) {
    init {
        DamageUtils.cardRegistry = cardRegistry
        // ZoneTransitionService.applyBattlefieldEntry registers a permanent's static abilities
        // and replacement effects on entry, so any code that moves a card to the battlefield
        // (reanimation, exile returns, leyline starts) gets the same wiring the cast pipeline
        // does. The handler is stateless beyond the registry, so a singleton is sufficient.
        ZoneTransitionService.staticAbilityHandler = StaticAbilityHandler(cardRegistry)
        ZoneTransitionService.cardRegistry = cardRegistry
        // A zone-change replacement's "…instead. When you do, create a token" rider (Head of the
        // Hunt) mints through the same executor an ability would, so the token gets the minting
        // set's art and its static abilities rather than the bare generic fallback.
        com.wingedsheep.engine.handlers.effects.ZoneMovementUtils.tokenExecutor =
            com.wingedsheep.engine.handlers.effects.token.CreateTokenExecutor(
                staticAbilityHandler = StaticAbilityHandler(cardRegistry),
                cardRegistry = cardRegistry,
                tokenArtRegistry = tokenArtRegistry
            )
    }
    /**
     * The one replacement-effect processor for this game. Declared before anything that
     * consumes it so the whole graph — the draw path via [EffectExecutorRegistry] and
     * [turnManager], and the continuation resumers — shares a single instance rather than
     * each constructing its own. The processor is stateless today; keeping it single is what
     * makes it safe for it to stop being so.
     */
    val replacementEffectProcessor = ReplacementEffectProcessor()
    val effectExecutorRegistry = EffectExecutorRegistry(
        cardRegistry = cardRegistry,
        tokenArtRegistry = tokenArtRegistry,
        replacementProcessor = replacementEffectProcessor
    )
    val manaAbilitySideEffectExecutor = ManaAbilitySideEffectExecutor(
        cardRegistry = cardRegistry,
        effectExecutor = effectExecutorRegistry::execute
    )
    val combatManager = CombatManager(cardRegistry, manaAbilitySideEffectExecutor)
    val triggerDetector = TriggerDetector(cardRegistry)
    val stateTriggerPoller = com.wingedsheep.engine.event.StateTriggerPoller(cardRegistry)
    val stackResolver = StackResolver(
        effectHandler = EffectHandler(cardRegistry = cardRegistry, registry = effectExecutorRegistry),
        cardRegistry = cardRegistry
    )
    val triggerProcessor = TriggerProcessor(cardRegistry = cardRegistry, stackResolver = stackResolver)
    val manaSolver = ManaSolver(cardRegistry)
    val costCalculator = CostCalculator(cardRegistry)
    val grantedKeywordResolver = GrantedKeywordResolver(cardRegistry)
    val alternativePaymentHandler = AlternativePaymentHandler(grantedKeywordResolver)
    val costHandler = CostHandler()
    val mulliganHandler = MulliganHandler(cardRegistry)
    val conditionEvaluator = ConditionEvaluator()
    val targetValidator = TargetValidator()
    val targetFinder = TargetFinder()
    val predicateEvaluator = PredicateEvaluator()
    val castPermissionUtils = CastPermissionUtils(cardRegistry, predicateEvaluator, conditionEvaluator)
    val sbaChecker = StateBasedActionChecker(cardRegistry = cardRegistry)
    val turnManager = TurnManager(
        cardRegistry = cardRegistry,
        combatManager = combatManager,
        sbaChecker = sbaChecker,
        effectExecutor = effectExecutorRegistry::execute,
        replacementProcessor = replacementEffectProcessor
    )
    val continuationHandler = ContinuationHandler(this)

    init {
        // Late wiring: every service in the graph is now constructed, so it's safe to
        // hand `this` to executors that need to synthesize a `CastSpell` action through
        // the full cast pipeline (currently only
        // [com.wingedsheep.engine.handlers.effects.library.CastFromCollectionWithoutPayingCostExecutor]).
        effectExecutorRegistry.libraryExecutors.initialize(this)
    }
}
