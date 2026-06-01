package com.wingedsheep.engine.event

import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.KeywordAbility
import kotlinx.serialization.Serializable

/**
 * A keyword ability (one carrying its own parameters, e.g. a cost) granted to a card entity
 * temporarily. Mirrors [GrantedActivatedAbility] / [GrantedTriggeredAbility] but for
 * [KeywordAbility]s that change how a card can be *cast* — currently Harmonize (CR 702.180),
 * granted by Songcrafter Mage to an instant/sorcery in a graveyard.
 *
 * Stored in [com.wingedsheep.engine.state.GameState.grantedKeywordAbilities] and consulted by
 * the cast-from-graveyard enumerator, the cast handler, the alternative-payment handler, and the
 * stack resolver (for the exile-on-resolution clause). The record is keyed by [entityId], which
 * the engine preserves across the graveyard → stack zone change, so a spell cast via a granted
 * harmonize still resolves into exile while the grant is live.
 *
 * Plain keyword grants that carry no parameters (flying, trample, …) continue to go through the
 * projected-state keyword layer, not this record.
 *
 * @property entityId The card that has the granted keyword ability
 * @property ability The granted keyword ability (e.g., [KeywordAbility.Harmonize])
 * @property duration How long the grant lasts (until end of turn for Songcrafter Mage)
 */
@Serializable
data class GrantedKeywordAbility(
    val entityId: EntityId,
    val ability: KeywordAbility,
    val duration: Duration
)
