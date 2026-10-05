package com.wingedsheep.engine.core

import com.wingedsheep.engine.legality.LegalityKernel
import com.wingedsheep.engine.event.TriggerDetector
import com.wingedsheep.engine.event.TriggerProcessor
import com.wingedsheep.engine.handlers.ConditionEvaluator
import com.wingedsheep.engine.handlers.ContinuationHandler
import com.wingedsheep.engine.handlers.CostHandler
import com.wingedsheep.engine.handlers.DynamicAmountEvaluator
import com.wingedsheep.engine.handlers.MulliganHandler
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.handlers.TargetFinder
import com.wingedsheep.engine.handlers.actions.land.PlayLandHandler
import com.wingedsheep.engine.handlers.actions.spell.CastSpellHandler
import com.wingedsheep.engine.replacement.ReplacementEffectProcessor
import com.wingedsheep.engine.legalactions.utils.CastPermissionUtils
import com.wingedsheep.engine.handlers.effects.EffectExecutorRegistry
import com.wingedsheep.engine.handlers.effects.ZoneTransitionService
import com.wingedsheep.engine.mechanics.StateBasedActionChecker
import com.wingedsheep.engine.mechanics.combat.CombatManager
import com.wingedsheep.engine.mechanics.mana.AlternativePaymentHandler
import com.wingedsheep.engine.mechanics.mana.CostCalculator
import com.wingedsheep.engine.mechanics.mana.GrantedKeywordResolver
import com.wingedsheep.engine.mechanics.mana.ManaAbilitySideEffectExecutor
import com.wingedsheep.engine.mechanics.mana.ManaSolver
import com.wingedsheep.engine.mechanics.stack.SpellCounterer
import com.wingedsheep.engine.mechanics.stack.StackResolver
import com.wingedsheep.engine.legalactions.LegalActionEnumerator
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
    val tokenArtRegistry: TokenArtRegistry? = null
) {
    /**
     * The engine's one predicate / condition / dynamic-amount evaluator, built as a unit (see
     * [PredicateEvaluator.conditions]) over this engine's card registry, and handed to everything
     * below that evaluates a filter, a condition or an amount.
     */
    val predicateEvaluator = PredicateEvaluator(cardRegistry)
    val conditionEvaluator: ConditionEvaluator = predicateEvaluator.conditions
    val dynamicAmountEvaluator: DynamicAmountEvaluator = predicateEvaluator.amounts
    val targetFinder = TargetFinder(predicateEvaluator)
    val targetValidator = TargetValidator(predicateEvaluator)

    /**
     * The one zone-transition service for this engine. It carries the card and token-art
     * registries and the evaluator every zone move needs (battlefield-entry setup, replacement
     * checks, a zone-change rider's token), so
     * it is threaded through the graph rather than parked in a global — two engines in one JVM
     * (server plus gym, parallel tests) each keep their own.
     */
    val zones = ZoneTransitionService(cardRegistry, predicateEvaluator, tokenArtRegistry)

    /**
     * The one replacement-effect processor for this game. Declared before anything that
     * consumes it so the whole graph — the draw path via [EffectExecutorRegistry] and
     * [turnManager], and the continuation resumers — shares a single instance rather than
     * each constructing its own. The processor is stateless today; keeping it single is what
     * makes it safe for it to stop being so.
     */
    val replacementEffectProcessor = ReplacementEffectProcessor(conditionEvaluator)

    /**
     * Counters and exiles stack objects. Shared by [stackResolver] and the counter / exile-a-spell
     * executors, which need nothing else of the stack machinery.
     */
    val spellCounterer = SpellCounterer(cardRegistry, predicateEvaluator)

    /**
     * The one effect-executor registry. The cast and land-play pipelines and the cost-payment
     * service it needs (to cast a card "without paying its mana cost", to pay or suffer) are built
     * from this whole graph, so it receives them as providers of [castSpellHandler],
     * [playLandHandler] and [costPaymentService], which are only read once an effect executes.
     */
    val effectExecutorRegistry: EffectExecutorRegistry = EffectExecutorRegistry(
        zones,
        cardRegistry = cardRegistry,
        tokenArtRegistry = tokenArtRegistry,
        replacementProcessor = replacementEffectProcessor,
        spellCounterer = spellCounterer,
        castSpellHandler = { castSpellHandler },
        playLandHandler = { playLandHandler },
        costPaymentService = { costPaymentService },
        targetFinder = targetFinder,
        targetValidator = targetValidator,
        legalActionEnumerator = { legalActionEnumerator }
    )
    val manaAbilitySideEffectExecutor = ManaAbilitySideEffectExecutor(
        zones,
        cardRegistry = cardRegistry,
        effectExecutor = effectExecutorRegistry::execute
    )
    val combatManager = CombatManager(zones, cardRegistry, manaAbilitySideEffectExecutor)
    val triggerDetector = TriggerDetector(cardRegistry, predicateEvaluator = predicateEvaluator, conditionEvaluator = conditionEvaluator)
    val stateTriggerPoller = com.wingedsheep.engine.event.StateTriggerPoller(cardRegistry, conditionEvaluator = conditionEvaluator)
    val stackResolver = StackResolver(
        zones,
        cardRegistry = cardRegistry,
        effects = effectExecutorRegistry,
        spellCounterer = spellCounterer,
        predicateEvaluator = predicateEvaluator,
        spliceTargetValidator = targetValidator
    )
    val triggerProcessor = TriggerProcessor(cardRegistry = cardRegistry, stackResolver = stackResolver, amountEvaluator = dynamicAmountEvaluator, targetFinder = targetFinder)
    val manaSolver = ManaSolver(cardRegistry, predicateEvaluator, scopedPlanner = { scopedManaActivationPlanner })
    private val scopedManaActivationPlanner by lazy {
        com.wingedsheep.engine.mechanics.mana.ScopedManaActivationPlanner(this)
    }
    val costCalculator = CostCalculator(cardRegistry, predicateEvaluator)
    val grantedKeywordResolver = GrantedKeywordResolver(cardRegistry)
    val alternativePaymentHandler = AlternativePaymentHandler(grantedKeywordResolver)
    val costHandler = CostHandler(zones)
    val mulliganHandler = MulliganHandler(cardRegistry, effectExecutorRegistry::execute)
    val castPermissionUtils = CastPermissionUtils(cardRegistry, predicateEvaluator, conditionEvaluator)
    val legalityKernel = LegalityKernel(cardRegistry, conditionEvaluator)
    val sbaChecker = StateBasedActionChecker(zones, cardRegistry = cardRegistry)
    val turnManager = TurnManager(
        zones,
        cardRegistry = cardRegistry,
        combatManager = combatManager,
        sbaChecker = sbaChecker,
        spellCounterer = spellCounterer,
        effectExecutor = effectExecutorRegistry::execute,
        replacementProcessor = replacementEffectProcessor
    )
    val legalActionEnumerator: LegalActionEnumerator = LegalActionEnumerator(
        cardRegistry, manaSolver, costCalculator, predicateEvaluator, conditionEvaluator, turnManager
    )
    val continuationHandler = ContinuationHandler(this)
    val settler = Settler(
        triggerDetector, triggerProcessor, sbaChecker, stateTriggerPoller, turnManager,
        effectExecutor = effectExecutorRegistry::execute,
        gameRestarter = GameRestarter(cardRegistry) { state, events ->
            mulliganHandler.beginFirstTurn(state, events, turnManager)
        }
    )

    /** The cast pipeline (CR 601.2). Built last: it draws on nearly every service above. */
    val castSpellHandler: CastSpellHandler = CastSpellHandler.create(this)
    val playLandHandler: PlayLandHandler = PlayLandHandler.create(this)

    /** Pays [com.wingedsheep.sdk.scripting.costs.PayCost]s — for "pay or suffer" executors and resumers alike. */
    val costPaymentService = com.wingedsheep.engine.mechanics.cost.CostPaymentService(this)
}
