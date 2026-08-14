package com.wingedsheep.engine.core

import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.ChoiceSlot
import kotlinx.serialization.Serializable

/**
 * Resume after a player picks the number for a
 * [com.wingedsheep.sdk.scripting.effects.ChooseNumberForSourceEffect]. The resumer writes the
 * chosen value as a [com.wingedsheep.engine.state.components.battlefield.ChoiceValue.NumberChoice]
 * into the source permanent's
 * [com.wingedsheep.engine.state.components.battlefield.CastChoicesComponent] under [slot],
 * replacing any prior value, so a characteristic-defining ability reads the latest choice.
 *
 * @property sourceId The permanent whose cast-choices bag receives the number.
 * @property controllerId The player who made the choice.
 * @property slot Which durable cast-choices slot to write.
 */
@Serializable
data class ChooseNumberForSourceContinuation(
    override val decisionId: String,
    val sourceId: EntityId,
    val controllerId: EntityId,
    val slot: ChoiceSlot
) : ContinuationFrame

/**
 * Resume after a player picks the recipient for a
 * [com.wingedsheep.sdk.scripting.effects.ChooseOpponentForSourceEffect]. The resumer writes the
 * chosen opponent as a [com.wingedsheep.engine.state.components.battlefield.ChoiceValue.EntityChoice]
 * into the source entity's
 * [com.wingedsheep.engine.state.components.battlefield.CastChoicesComponent] under
 * [com.wingedsheep.sdk.scripting.ChoiceSlot.OPPONENT], where
 * [com.wingedsheep.sdk.scripting.references.Player.ChosenOpponent] reads it back.
 *
 * @property sourceId The spell or permanent whose cast-choices bag receives the opponent.
 * @property controllerId The player who made the choice.
 * @property opponentIds The option list shown to the player, positionally aligned with the
 *   [com.wingedsheep.engine.core.ChooseOptionDecision]'s options.
 */
@Serializable
data class ChooseOpponentForSourceContinuation(
    override val decisionId: String,
    val sourceId: EntityId,
    val controllerId: EntityId,
    val opponentIds: List<EntityId>
) : ContinuationFrame

/**
 * Resume after a player picks the card type for a
 * [com.wingedsheep.sdk.scripting.effects.ChooseCardTypeForSourceEffect]. The resumer writes the
 * chosen type name as a [com.wingedsheep.engine.state.components.battlefield.ChoiceValue.TextChoice]
 * into the source entity's
 * [com.wingedsheep.engine.state.components.battlefield.CastChoicesComponent] under [slot], where
 * [com.wingedsheep.sdk.scripting.predicates.CardPredicate.CardTypeEqualsChosenComponent] reads it
 * back at cost-calculation / projection time (Arachne, Psionic Weaver).
 *
 * @property sourceId The permanent whose cast-choices bag receives the card type.
 * @property controllerId The player who made the choice.
 * @property slot Which durable cast-choices slot to write.
 * @property cardTypes The offered option list, positionally aligned with the
 *   [com.wingedsheep.engine.core.ChooseOptionDecision]'s options.
 */
@Serializable
data class ChooseCardTypeForSourceContinuation(
    override val decisionId: String,
    val sourceId: EntityId,
    val controllerId: EntityId,
    val slot: ChoiceSlot,
    val cardTypes: List<String>
) : ContinuationFrame

/**
 * Resume after the controller picks *which* opponent makes a
 * [com.wingedsheep.sdk.scripting.effects.Chooser.Opponent] decision (CR 601.7a / 602.3a and
 * the matching resolution-time rulings — see
 * [com.wingedsheep.engine.handlers.effects.ChooserResolution]).
 *
 * The resumer stamps the pick onto [baseContext] as
 * [com.wingedsheep.engine.handlers.EffectContext.opponentDeciderId] and re-runs [effect], which
 * then resolves its chooser straight to that opponent and pauses again with its own decision.
 * Nothing durable is written: the stamp lives only on the re-run context, so a second
 * "an opponent chooses" step in the same resolution asks again.
 *
 * @property controllerId The player picking the decider.
 * @property sourceId The spell or permanent whose effect is being resolved (display only).
 * @property opponentIds The option list shown, positionally aligned with the
 *   [ChooseOptionDecision]'s options.
 * @property effect The effect to re-execute once the decider is known.
 * @property baseContext The context the effect was first executed with, including pipeline storage.
 */
@Serializable
data class ChooseOpponentDeciderContinuation(
    override val decisionId: String,
    val controllerId: EntityId,
    val sourceId: EntityId?,
    val opponentIds: List<EntityId>,
    val effect: com.wingedsheep.sdk.scripting.effects.Effect,
    val baseContext: com.wingedsheep.engine.handlers.EffectContext
) : ContinuationFrame
