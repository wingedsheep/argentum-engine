package com.wingedsheep.engine.handlers.effects

import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.handlers.DecisionHandler
import com.wingedsheep.engine.handlers.DynamicAmountEvaluator
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.composite.CompositeEffectExecutor
import com.wingedsheep.engine.handlers.effects.composite.ConditionalEffectExecutor
import com.wingedsheep.engine.handlers.effects.damage.DealDamageExecutor
import com.wingedsheep.engine.handlers.effects.damage.DealDamageToAllCreaturesExecutor
import com.wingedsheep.engine.handlers.effects.damage.DealDamageToAllExecutor
import com.wingedsheep.engine.handlers.effects.damage.DealXDamageExecutor
import com.wingedsheep.engine.handlers.effects.drawing.DiscardCardsExecutor
import com.wingedsheep.engine.handlers.effects.drawing.DrawCardsExecutor
import com.wingedsheep.engine.handlers.effects.drawing.EachPlayerDiscardsDrawsExecutor
import com.wingedsheep.engine.handlers.effects.player.PlayAdditionalLandsExecutor
import com.wingedsheep.engine.handlers.effects.library.MillExecutor
import com.wingedsheep.engine.handlers.effects.library.ScryExecutor
import com.wingedsheep.engine.handlers.effects.library.ShuffleLibraryExecutor
import com.wingedsheep.engine.handlers.effects.life.GainLifeExecutor
import com.wingedsheep.engine.handlers.effects.life.LoseHalfLifeExecutor
import com.wingedsheep.engine.handlers.effects.life.LoseLifeExecutor
import com.wingedsheep.engine.handlers.effects.mana.AddColorlessManaExecutor
import com.wingedsheep.engine.handlers.effects.mana.AddManaExecutor
import com.wingedsheep.engine.handlers.effects.permanent.AddCountersExecutor
import com.wingedsheep.engine.handlers.effects.permanent.ModifyStatsExecutor
import com.wingedsheep.engine.handlers.effects.permanent.RemoveCountersExecutor
import com.wingedsheep.engine.handlers.effects.permanent.TapAllCreaturesExecutor
import com.wingedsheep.engine.handlers.effects.permanent.TapUntapExecutor
import com.wingedsheep.engine.handlers.effects.removal.DestroyAllCreaturesExecutor
import com.wingedsheep.engine.handlers.effects.removal.DestroyAllLandsExecutor
import com.wingedsheep.engine.handlers.effects.removal.DestroyExecutor
import com.wingedsheep.engine.handlers.effects.removal.ExileExecutor
import com.wingedsheep.engine.handlers.effects.removal.ReturnToHandExecutor
import com.wingedsheep.engine.handlers.effects.stack.CounterSpellExecutor
import com.wingedsheep.engine.handlers.effects.token.CreateTokenExecutor
import com.wingedsheep.engine.handlers.effects.token.CreateTreasureExecutor
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.sdk.scripting.*

/**
 * Registry that maps effect types to their executors.
 *
 * This implements the Strategy pattern, allowing each effect type to have
 * its own dedicated executor class while providing a unified dispatch mechanism.
 */
class EffectExecutorRegistry(
    private val amountEvaluator: DynamicAmountEvaluator = DynamicAmountEvaluator(),
    private val decisionHandler: DecisionHandler = DecisionHandler()
) {
    // Life executors
    private val gainLifeExecutor = GainLifeExecutor(amountEvaluator)
    private val loseLifeExecutor = LoseLifeExecutor()
    private val loseHalfLifeExecutor = LoseHalfLifeExecutor()

    // Damage executors
    private val dealDamageExecutor = DealDamageExecutor()
    private val dealXDamageExecutor = DealXDamageExecutor()
    private val dealDamageToAllCreaturesExecutor = DealDamageToAllCreaturesExecutor()
    private val dealDamageToAllExecutor = DealDamageToAllExecutor()

    // Drawing executors
    private val drawCardsExecutor = DrawCardsExecutor(amountEvaluator)
    private val discardCardsExecutor = DiscardCardsExecutor(decisionHandler)
    private val eachPlayerDiscardsDrawsExecutor = EachPlayerDiscardsDrawsExecutor(decisionHandler)

    // Removal executors
    private val destroyExecutor = DestroyExecutor()
    private val destroyAllCreaturesExecutor = DestroyAllCreaturesExecutor()
    private val destroyAllLandsExecutor = DestroyAllLandsExecutor()
    private val exileExecutor = ExileExecutor()
    private val returnToHandExecutor = ReturnToHandExecutor()

    // Permanent executors
    private val tapUntapExecutor = TapUntapExecutor()
    private val tapAllCreaturesExecutor = TapAllCreaturesExecutor()
    private val modifyStatsExecutor = ModifyStatsExecutor()
    private val addCountersExecutor = AddCountersExecutor()
    private val removeCountersExecutor = RemoveCountersExecutor()

    // Mana executors
    private val addManaExecutor = AddManaExecutor()
    private val addColorlessManaExecutor = AddColorlessManaExecutor()

    // Token executors
    private val createTokenExecutor = CreateTokenExecutor()
    private val createTreasureExecutor = CreateTreasureExecutor()

    // Library executors
    private val scryExecutor = ScryExecutor()
    private val millExecutor = MillExecutor()
    private val shuffleLibraryExecutor = ShuffleLibraryExecutor()

    // Stack executors
    private val counterSpellExecutor = CounterSpellExecutor()

    // Player executors
    private val playAdditionalLandsExecutor = PlayAdditionalLandsExecutor()

    // Composite executors (initialized lazily with reference to execute function)
    private val compositeEffectExecutor by lazy { CompositeEffectExecutor(::execute) }
    private val conditionalEffectExecutor by lazy { ConditionalEffectExecutor(::execute) }

    /**
     * Execute an effect using the appropriate executor.
     *
     * @param state The current game state
     * @param effect The effect to execute
     * @param context The execution context
     * @return The execution result with new state and events
     */
    fun execute(state: GameState, effect: Effect, context: EffectContext): ExecutionResult {
        return when (effect) {
            // Life effects
            is GainLifeEffect -> gainLifeExecutor.execute(state, effect, context)
            is LoseLifeEffect -> loseLifeExecutor.execute(state, effect, context)
            is LoseHalfLifeEffect -> loseHalfLifeExecutor.execute(state, effect, context)

            // Damage effects
            is DealDamageEffect -> dealDamageExecutor.execute(state, effect, context)
            is DealXDamageEffect -> dealXDamageExecutor.execute(state, effect, context)
            is DealDamageToAllCreaturesEffect -> dealDamageToAllCreaturesExecutor.execute(state, effect, context)
            is DealDamageToAllEffect -> dealDamageToAllExecutor.execute(state, effect, context)

            // Drawing effects
            is DrawCardsEffect -> drawCardsExecutor.execute(state, effect, context)
            is DiscardCardsEffect -> discardCardsExecutor.execute(state, effect, context)
            is EachPlayerDiscardsDrawsEffect -> eachPlayerDiscardsDrawsExecutor.execute(state, effect, context)

            // Removal effects
            is DestroyEffect -> destroyExecutor.execute(state, effect, context)
            is DestroyAllCreaturesEffect -> destroyAllCreaturesExecutor.execute(state, effect, context)
            is DestroyAllLandsEffect -> destroyAllLandsExecutor.execute(state, effect, context)
            is ExileEffect -> exileExecutor.execute(state, effect, context)
            is ReturnToHandEffect -> returnToHandExecutor.execute(state, effect, context)

            // Permanent effects
            is TapUntapEffect -> tapUntapExecutor.execute(state, effect, context)
            is TapAllCreaturesEffect -> tapAllCreaturesExecutor.execute(state, effect, context)
            is ModifyStatsEffect -> modifyStatsExecutor.execute(state, effect, context)
            is AddCountersEffect -> addCountersExecutor.execute(state, effect, context)
            is RemoveCountersEffect -> removeCountersExecutor.execute(state, effect, context)

            // Mana effects
            is AddManaEffect -> addManaExecutor.execute(state, effect, context)
            is AddColorlessManaEffect -> addColorlessManaExecutor.execute(state, effect, context)

            // Token effects
            is CreateTokenEffect -> createTokenExecutor.execute(state, effect, context)
            is CreateTreasureTokensEffect -> createTreasureExecutor.execute(state, effect, context)

            // Library effects
            is ScryEffect -> scryExecutor.execute(state, effect, context)
            is MillEffect -> millExecutor.execute(state, effect, context)
            is ShuffleLibraryEffect -> shuffleLibraryExecutor.execute(state, effect, context)

            // Composite effects
            is CompositeEffect -> compositeEffectExecutor.execute(state, effect, context)
            is ConditionalEffect -> conditionalEffectExecutor.execute(state, effect, context)

            // Stack effects
            is CounterSpellEffect -> counterSpellExecutor.execute(state, effect, context)

            // Player effects
            is PlayAdditionalLandsEffect -> playAdditionalLandsExecutor.execute(state, effect, context)

            // Default handler for unimplemented effects
            else -> {
                // Log unhandled effect type (in production, could emit warning event)
                ExecutionResult.success(state)
            }
        }
    }
}
