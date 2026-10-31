package com.wingedsheep.engine.handlers.effects.library

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.TargetFinder
import com.wingedsheep.engine.handlers.actions.spell.CastSpellHandler
import com.wingedsheep.engine.handlers.actions.land.PlayLandHandler
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.handlers.effects.ExecutorModule
import com.wingedsheep.engine.handlers.effects.ZoneTransitionService
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.effects.Effect

/**
 * Module providing all library-related effect executors.
 */
class LibraryExecutors(
    /** The registry's re-entrant entry point, for the scry/surveil/clash/discover macros' pipelines. */
    private val recursion: (GameState, Effect, EffectContext) -> EffectResult,
    private val zones: ZoneTransitionService,
    private val cardRegistry: CardRegistry,
    /**
     * The engine's cast and land-play pipelines, for the "cast / play it without paying its mana
     * cost" executors. Providers, read when an executor runs: the pipelines are built from the whole
     * engine graph — this module's registry included — so they can't exist yet when it is built.
     */
    private val castSpellHandler: () -> CastSpellHandler,
    private val playLandHandler: () -> PlayLandHandler,
    private val targetFinder: TargetFinder,
    private val legalActionEnumerator: () -> com.wingedsheep.engine.legalactions.LegalActionEnumerator
) : ExecutorModule {

    /** A resolution-time cast a prohibition forbids (Drannith Magistrate) is never offered. */
    private val castForbidden: (GameState, EntityId, EntityId) -> Boolean = { state, playerId, cardId ->
        castSpellHandler().resolutionCastProhibition(state, CastSpell(playerId, cardId)) != null
    }

    override fun executors(): List<EffectExecutor<*>> = listOf(
        ForcePlayExecutor(legalActionEnumerator),
        ScryExecutor(recursion),
        ClashExecutor(recursion),
        SurveilExecutor(recursion),
        ShuffleLibraryExecutor(),
        GrantMayPlayFromExileExecutor(),
        MakePlottedExecutor(),
        GrantPlayWithoutPayingCostExecutor(),
        GrantPlayWithCostIncreaseExecutor(),
        GrantPlayWithAdditionalCostExecutor(),
        GrantFreeCastTargetFromExileExecutor(),
        GatherUntilMatchExecutor(predicateEvaluator = zones.predicateEvaluator),
        RevealCollectionExecutor(),
        ExileFromTopRepeatingExecutor(zones),
        ExileLibraryUntilManaValueExecutor(zones),
        ExileTopCardContestExecutor(zones),
        CascadeExecutor(zones, castForbidden),
        DiscoverExecutor(zones, recursion, castForbidden),
        CastFromCollectionWithoutPayingCostExecutor(
            castSpellHandlerProvider = castSpellHandler,
            cardRegistry = cardRegistry,
            targetFinder = targetFinder,
        ),
        PlayFromCollectionWithoutPayingCostExecutor(
            castSpellHandlerProvider = castSpellHandler,
            playLandHandlerProvider = playLandHandler,
            cardRegistry = cardRegistry,
            targetFinder = targetFinder,
        ),
        CastAnyNumberFromCollectionWithoutPayingCostExecutor(
            playLandHandlerProvider = playLandHandler,
            castSpellHandlerProvider = castSpellHandler,
        ),
        GatherSubtypesExecutor(),
        CaptureControllersExecutor(),
        ChooseCreatureTypePipelineExecutor(),
        ChooseOptionPipelineExecutor(cardRegistry = cardRegistry),
        ChooseSpellCastThisTurnExecutor(zones.predicateEvaluator),
        NoteCreatureTypePipelineExecutor(),
        GatherCardsExecutor(zones.predicateEvaluator, cardRegistry),
        RecordSourceObjectsExecutor(),
        CopyCardIntoCollectionExecutor(),
        CopyCollectionIntoCollectionExecutor(),
        GrantSuspendExecutor(),
        SelectFromCollectionExecutor(cardRegistry = cardRegistry, predicateEvaluator = zones.predicateEvaluator),
        ChoosePileExecutor(),
        SelectTargetPipelineExecutor(targetFinder = targetFinder),
        MoveCollectionExecutor(zones, cardRegistry = cardRegistry, targetFinder = targetFinder),
        FilterCollectionExecutor(predicateEvaluator = zones.predicateEvaluator),
        ChooseOnePerCategoryExecutor(predicateEvaluator = zones.predicateEvaluator),
        PutOnTopOrBottomOfLibraryExecutor(),
        StoreNumberExecutor(amountEvaluator = zones.predicateEvaluator.amounts),
        StorePlayerExecutor(),
        StoreCardNameExecutor(),
        EmitScriedEventExecutor(),
        EmitClashedEventExecutor(),
        EmitSurveiledEventExecutor(),
        EmitDiscoveredEventExecutor(),
        EmitManifestedDreadEventExecutor(),
        EmitLibrarySearchedEventExecutor()
    )
}
