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
