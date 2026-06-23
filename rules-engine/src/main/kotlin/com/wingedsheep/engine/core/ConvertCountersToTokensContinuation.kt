package com.wingedsheep.engine.core

import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.effects.CreateTokenEffect
import com.wingedsheep.sdk.scripting.events.CounterTypeFilter
import kotlinx.serialization.Serializable

/**
 * Resume after a player picks how many [counterType] counters to remove from [sourceId] for a
 * [com.wingedsheep.sdk.scripting.effects.ConvertCountersToTokensEffect]. The resumer removes that
 * many counters and then runs [tokenFactory] with its count overridden to the number removed, so
 * exactly one token is minted per counter removed (Tetravus's counters-to-Tetravite half).
 *
 * @property sourceId The permanent whose counters are removed and that mints the tokens.
 * @property controllerId The player resolving the ability.
 * @property counterType Which counter kind is being removed.
 * @property tokenFactory The per-token template; its count is set to the number removed at resume.
 */
@Serializable
data class ConvertCountersToTokensContinuation(
    override val decisionId: String,
    val sourceId: EntityId,
    val controllerId: EntityId,
    val counterType: CounterTypeFilter,
    val tokenFactory: CreateTokenEffect
) : ContinuationFrame
