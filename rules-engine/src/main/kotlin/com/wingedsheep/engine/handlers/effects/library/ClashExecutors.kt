package com.wingedsheep.engine.handlers.effects.library

import com.wingedsheep.engine.core.ClashedEvent
import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.chosenOpponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.effects.CLASH_THEIRS
import com.wingedsheep.sdk.scripting.effects.CLASH_YOURS
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.Chooser
import com.wingedsheep.sdk.scripting.effects.ClashEffect
import com.wingedsheep.sdk.scripting.effects.CompositeEffect
import com.wingedsheep.sdk.scripting.effects.Effect
import com.wingedsheep.sdk.scripting.effects.EmitClashedEventEffect
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.effects.MoveCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectFromCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectionMode
import com.wingedsheep.sdk.scripting.effects.ZonePlacement
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import kotlin.reflect.KClass

/**
 * Executor for the [ClashEffect] macro (CR 701.30). Like [ScryExecutor] it carries no
 * gather/select/move logic of its own: it expands the marker into a composite of the ordinary
 * library primitives and delegates to the registry's recursive [effectExecutor], so the
 * `SelectCardsDecision` pauses and their continuations are the ones the pipeline machinery
 * already owns.
 *
 * The expansion is built here rather than in the SDK because one part of it depends on the live
 * state. CR 701.30c: *"Each clashing player reveals the top card of their library at the same
 * time. Then those players decide in APNAP order (see rule 101.4) where to put those cards."* A
 * `CompositeEffect` has a fixed step order, so the two top-or-bottom selections are emitted
 * active-player-first — which is the clasher's own decision first on their turn, and the
 * opponent's first when a clash spell is cast during the opponent's turn (Lash Out at instant
 * speed). Both gathers precede both selections regardless, so the simultaneous reveal holds: each
 * player already sees the other's card before anyone decides.
 *
 * Both gathers use `revealed = true` — clash is a public reveal (CR 701.30a), not a private look,
 * so the cards go face up for every player rather than only to the gatherer.
 *
 * With no opponent chosen (no [com.wingedsheep.sdk.scripting.effects.ChooseOpponentForSourceEffect]
 * ran, or the chosen player has left the game) the clash is a no-op: nothing is revealed, nothing
 * is written to the win collection, and the printed "if you win" rider fails closed.
 */
class ClashExecutor(
    private val effectExecutor: (GameState, Effect, EffectContext) -> EffectResult,
) : EffectExecutor<ClashEffect> {

    override val effectType: KClass<ClashEffect> = ClashEffect::class

    override fun execute(state: GameState, effect: ClashEffect, context: EffectContext): EffectResult {
        val clasherId = context.controllerId
        val opponentId = ClashScoring.chosenOpponentOf(state, context)
            ?: return EffectResult.success(state)

        // CR 701.30a — one card each, revealed publicly. Both gathers run before either decision
        // so the reveal is simultaneous even though the decisions are ordered.
        val gathers = listOf(
            GatherCardsEffect(
                source = CardSource.TopOfLibrary(DynamicAmount.Fixed(1), Player.You),
                storeAs = CLASH_YOURS,
                revealed = true
            ),
            GatherCardsEffect(
                source = CardSource.TopOfLibrary(DynamicAmount.Fixed(1), Player.ChosenOpponent),
                storeAs = CLASH_THEIRS,
                revealed = true
            )
        )

        // CR 701.30c — decisions in APNAP order. The clasher is not necessarily the active player:
        // an instant-speed clash on the opponent's turn has them deciding first.
        val yourSelect = SelectFromCollectionEffect(
            from = CLASH_YOURS,
            selection = SelectionMode.ChooseUpTo(DynamicAmount.Fixed(1)),
            chooser = Chooser.Controller,
            storeSelected = YOURS_TO_BOTTOM,
            storeRemainder = YOURS_TO_TOP,
            selectedLabel = "Put on bottom",
            remainderLabel = "Leave on top",
            prompt = "Clash — put your revealed card on the bottom of your library?"
        )
        val theirSelect = SelectFromCollectionEffect(
            from = CLASH_THEIRS,
            selection = SelectionMode.ChooseUpTo(DynamicAmount.Fixed(1)),
            chooser = Chooser.ChosenOpponent,
            storeSelected = THEIRS_TO_BOTTOM,
            storeRemainder = THEIRS_TO_TOP,
            selectedLabel = "Put on bottom",
            remainderLabel = "Leave on top",
            prompt = "Clash — put your revealed card on the bottom of your library?"
        )
        val selections =
            if (state.activePlayerId == opponentId) listOf(theirSelect, yourSelect)
            else listOf(yourSelect, theirSelect)

        // Only the bottom moves are real: a card left on top never left the library, so moving it
        // "back" would emit a spurious zone change. The remainder collections exist purely to give
        // the selection a named complement.
        val moves = listOf(
            MoveCollectionEffect(
                from = YOURS_TO_BOTTOM,
                destination = CardDestination.ToZone(Zone.LIBRARY, Player.You, placement = ZonePlacement.Bottom)
            ),
            MoveCollectionEffect(
                from = THEIRS_TO_BOTTOM,
                destination = CardDestination.ToZone(
                    Zone.LIBRARY, Player.ChosenOpponent, placement = ZonePlacement.Bottom
                )
            )
        )

        val tail = EmitClashedEventEffect(
            yourCollection = CLASH_YOURS,
            theirCollection = CLASH_THEIRS,
            storeWonAs = effect.storeWonAs
        )

        return effectExecutor(state, CompositeEffect(gathers + selections + moves + tail), context)
    }

    private companion object {
        const val YOURS_TO_BOTTOM = "clashYoursToBottom"
        const val YOURS_TO_TOP = "clashYoursToTop"
        const val THEIRS_TO_BOTTOM = "clashTheirsToBottom"
        const val THEIRS_TO_TOP = "clashTheirsToTop"
    }
}

/**
 * Tail of the clash pipeline: score the clash (CR 701.30d) and emit the [ClashedEvent].
 *
 * Scoring is "strictly greatest mana value among the cards revealed in this clash". Two
 * consequences the tests pin down: a tie means **nobody** wins, and a player whose library was
 * empty revealed nothing, so they cannot win — but their opponent still can, because the empty
 * side contributes no card to beat.
 *
 * One event is emitted **per clashing player**, because both of them clashed. The Entangling Trap
 * and Sylvan Echoes rulings are explicit that a clash caused by an opponent's spell still fires
 * your own "whenever you clash" abilities, and that you can win a clash you did not initiate.
 * Emitting after the pipeline's moves is what makes the printed reminder true — "this ability
 * triggers after the clash ends".
 */
class EmitClashedEventExecutor : EffectExecutor<EmitClashedEventEffect> {

    override val effectType: KClass<EmitClashedEventEffect> = EmitClashedEventEffect::class

    override fun execute(
        state: GameState,
        effect: EmitClashedEventEffect,
        context: EffectContext
    ): EffectResult {
        val clasherId = context.controllerId
        val opponentId = ClashScoring.chosenOpponentOf(state, context)
            ?: return EffectResult.success(state)

        val yourCard = context.pipeline.storedCollections[effect.yourCollection]?.firstOrNull()
        val theirCard = context.pipeline.storedCollections[effect.theirCollection]?.firstOrNull()

        val yourValue = ClashScoring.manaValueOf(state, yourCard)
        val theirValue = ClashScoring.manaValueOf(state, theirCard)

        // CR 701.30d: strictly greater than every other revealed card. A null value means no card
        // was revealed (empty library) — that player never wins, and never blocks the other from
        // winning either.
        val youWon = yourValue != null && (theirValue == null || yourValue > theirValue)
        val theyWon = theirValue != null && (yourValue == null || theirValue > yourValue)

        val sourceName = context.sourceId
            ?.let { state.getEntity(it)?.get<CardComponent>()?.name }
            ?: "Clash"

        return EffectResult(
            state = state,
            events = listOf(
                ClashedEvent(
                    playerId = clasherId,
                    opponentId = opponentId,
                    won = youWon,
                    sourceName = sourceName
                ),
                ClashedEvent(
                    playerId = opponentId,
                    opponentId = clasherId,
                    won = theyWon,
                    sourceName = sourceName
                )
            ),
            // The win flag the printed "if you win" rider gates on, expressed as a collection so
            // the ordinary SuccessCriterion.CollectionNonEmpty can read it — see
            // MechanicPatterns.clash. Empty on a loss or a tie.
            updatedCollections = mapOf(
                effect.storeWonAs to if (youWon && yourCard != null) listOf(yourCard) else emptyList()
            )
        )
    }
}

/** Shared reads for the two clash executors. */
internal object ClashScoring {

    /**
     * The opponent this clash is with — the durable pick a preceding
     * [com.wingedsheep.sdk.scripting.effects.ChooseOpponentForSourceEffect] wrote onto the source.
     * Null when no choice was made or the source is gone, which makes the clash a silent no-op.
     */
    fun chosenOpponentOf(state: GameState, context: EffectContext): EntityId? =
        context.chosenOpponent(state)

    /**
     * Mana value of a revealed card, or null when no card was revealed. Read off the printed
     * [CardComponent] rather than projected state: the card is in a library, which the layer
     * projector does not cover, and mana value is a printed characteristic there.
     */
    fun manaValueOf(state: GameState, cardId: EntityId?): Int? =
        cardId?.let { state.getEntity(it)?.get<CardComponent>()?.manaValue }
}
