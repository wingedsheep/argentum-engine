package com.wingedsheep.engine.state

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.GameObjectFilter
import kotlinx.serialization.Serializable

/**
 * Tracks a pending "the next [spellFilter] spell you cast this turn has [keyword]" rider (Archway of
 * Innovation's improvise), installed by
 * [com.wingedsheep.sdk.scripting.effects.GrantNextSpellKeywordEffect].
 *
 * One-shot sibling of [PendingNextSpellAffinity]:
 * [com.wingedsheep.engine.mechanics.mana.GrantedKeywordResolver] reads it, so every site that asks
 * whether a spell being cast has the keyword (the cast enumerator, the validator, the payment
 * handler) sees the grant, and [com.wingedsheep.engine.handlers.actions.spell.CastTriggers] removes
 * it once [controllerId] casts a matching spell — whether or not the keyword was used.
 *
 * Cleared at every turn boundary by [com.wingedsheep.engine.core.TurnManager.startTurn].
 *
 * @property controllerId The player whose next matching spell gains [keyword].
 * @property keyword The cost-payment keyword granted.
 * @property spellFilter Which spell the rider waits for.
 * @property sourceId The entity that created this rider.
 * @property sourceName Human-readable name of the source, shown on the client badge.
 */
@Serializable
data class PendingNextSpellKeyword(
    val controllerId: EntityId,
    val keyword: Keyword,
    val spellFilter: GameObjectFilter,
    val sourceId: EntityId,
    val sourceName: String
)
