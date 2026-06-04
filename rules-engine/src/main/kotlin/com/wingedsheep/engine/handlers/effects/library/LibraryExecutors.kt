package com.wingedsheep.engine.handlers.effects.library

import com.wingedsheep.engine.core.EngineServices
import com.wingedsheep.engine.handlers.TargetFinder
import com.wingedsheep.engine.handlers.actions.spell.CastSpellHandler
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.handlers.effects.ExecutorModule
import com.wingedsheep.engine.registry.CardRegistry
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

    /** Late-bind the cast machinery once [EngineServices] is fully constructed. */
    fun initialize(services: EngineServices) {
        castSpellHandlerRef.set(CastSpellHandler.create(services))
    }

    override fun executors(): List<EffectExecutor<*>> = listOf(
        ShuffleLibraryExecutor(),
        GrantMayPlayFromExileExecutor(),
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
        GatherCardsExecutor(),
        CopyCardIntoCollectionExecutor(),
        GrantSuspendExecutor(),
        SelectFromCollectionExecutor(),
        ChoosePileExecutor(),
        SelectTargetPipelineExecutor(targetFinder = targetFinder ?: TargetFinder()),
        MoveCollectionExecutor(cardRegistry = cardRegistry, targetFinder = targetFinder),
        FilterCollectionExecutor(),
        PutOnTopOrBottomOfLibraryExecutor(),
        StoreNumberExecutor(),
        StoreCardNameExecutor(),
        EmitScriedEventExecutor()
    )
}
