package com.wingedsheep.engine.handlers.effects.library

import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.core.EngineServices
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.TargetFinder
import com.wingedsheep.engine.handlers.actions.spell.CastSpellHandler
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.handlers.effects.ExecutorModule
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.sdk.scripting.effects.Effect
import java.util.concurrent.atomic.AtomicReference

/**
 * Module providing all library-related effect executors.
 *
 * [CastFromCollectionWithoutPayingCostExecutor] needs a [CastSpellHandler], which depends on
 * the full [EngineServices] graph. Because this module is registered while that graph is
 * still being built, the handler is wired in after construction via [initialize]; the
 * executor reads it through an [AtomicReference] holder, so it stays null until the
 * services finish constructing.
 */
class LibraryExecutors(
    private val cardRegistry: CardRegistry,
    private val targetFinder: TargetFinder? = null,
) : ExecutorModule {

    private val castSpellHandlerRef = AtomicReference<CastSpellHandler?>(null)

    /**
     * The registry's recursive effect executor, used by the scry/surveil macro executors to run
     * their expanded pipelines. Late-bound via [initializeRecursion] because the recursion entry
     * point doesn't exist until the registry is wiring its deferred (recursive) modules. Read
     * through the ref at execution time (like [castSpellHandlerRef]) so constructing this module
     * before initialization — as some unit tests do — never trips over an uninitialized property;
     * only an actual scry/surveil with no recursion wired errors.
     */
    private val recursionRef =
        AtomicReference<((GameState, Effect, EffectContext) -> EffectResult)?>(null)

    private val recursion: (GameState, Effect, EffectContext) -> EffectResult =
        { state, effect, context ->
            val executor = recursionRef.get()
                ?: error("LibraryExecutors.initializeRecursion(...) was not called before the scry/surveil macro ran")
            executor(state, effect, context)
        }

    /** Late-bind the cast machinery once [EngineServices] is fully constructed. */
    fun initialize(services: EngineServices) {
        castSpellHandlerRef.set(CastSpellHandler.create(services))
    }

    /** Late-bind the registry's recursive executor so the scry/surveil macros can delegate. */
    fun initializeRecursion(executor: (GameState, Effect, EffectContext) -> EffectResult) {
        recursionRef.set(executor)
    }

    override fun executors(): List<EffectExecutor<*>> = listOf(
        ScryExecutor(recursion),
        SurveilExecutor(recursion),
        ShuffleLibraryExecutor(),
        GrantMayPlayFromExileExecutor(),
        MakePlottedExecutor(),
        GrantPlayWithoutPayingCostExecutor(),
        GrantPlayWithCostIncreaseExecutor(),
        GrantPlayWithAdditionalCostExecutor(),
        GrantFreeCastTargetFromExileExecutor(),
        GatherUntilMatchExecutor(),
        RevealCollectionExecutor(),
        ExileFromTopRepeatingExecutor(),
        ExileLibraryUntilManaValueExecutor(),
        CascadeExecutor(),
        CastFromCollectionWithoutPayingCostExecutor(
            castSpellHandlerProvider = {
                castSpellHandlerRef.get()
                    ?: error("LibraryExecutors.initialize(services) was not called before the executor ran")
            },
            cardRegistry = cardRegistry,
            targetFinder = targetFinder ?: TargetFinder(),
        ),
        CastAnyNumberFromCollectionWithoutPayingCostExecutor(),
        GatherSubtypesExecutor(),
        CaptureControllersExecutor(),
        ChooseCreatureTypePipelineExecutor(),
        ChooseOptionPipelineExecutor(cardRegistry = cardRegistry),
        NoteCreatureTypePipelineExecutor(),
        GatherCardsExecutor(),
        CopyCardIntoCollectionExecutor(),
        GrantSuspendExecutor(),
        SelectFromCollectionExecutor(cardRegistry = cardRegistry),
        ChoosePileExecutor(),
        SelectTargetPipelineExecutor(targetFinder = targetFinder ?: TargetFinder()),
        MoveCollectionExecutor(cardRegistry = cardRegistry, targetFinder = targetFinder),
        FilterCollectionExecutor(),
        PutOnTopOrBottomOfLibraryExecutor(),
        StoreNumberExecutor(),
        StoreCardNameExecutor(),
        EmitScriedEventExecutor(),
        EmitSurveiledEventExecutor()
    )
}
