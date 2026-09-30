package com.wingedsheep.engine.state.components.identity

import com.wingedsheep.engine.state.Component
import com.wingedsheep.sdk.core.Color
import kotlinx.serialization.Serializable

/**
 * Static "hexproof from [quality]" (Rule 702.11b) printed on a card — one entry per quality the
 * card is hexproof from. Attached at card-entity creation from the card's
 * [com.wingedsheep.sdk.scripting.KeywordAbility.Hexproof] abilities; hexproof granted at runtime by
 * a spell or static ability uses floating effects / projected keywords instead.
 *
 * "Hexproof from [quality]" means "This permanent can't be the target of [quality] spells your
 * opponents control or abilities your opponents control from [quality] sources."
 *
 * [StateProjector][com.wingedsheep.engine.mechanics.layers.StateProjector] flattens this into the
 * projected keyword set as `HEXPROOF_FROM_<COLOR>` / `HEXPROOF_FROM_CARDTYPE_<TYPE>`, mirroring the
 * `PROTECTION_FROM_*` idiom, so targeting checks read a single uniform keyword namespace.
 *
 * @property colors Colors this permanent is hexproof from (Knight of Malice — hexproof from white).
 * @property cardTypes Uppercased card-type names this permanent is hexproof from
 *   (Elenda, Saint of Dusk — hexproof from instants).
 * @property nonColors Colors whose *complement* this permanent is hexproof from — "nongreen"
 *   (Thrun, Breaker of Silence), projected as `HEXPROOF_FROM_NON_<COLOR>`.
 */
@Serializable
data class HexproofFromComponent(
    val colors: Set<Color> = emptySet(),
    val cardTypes: Set<String> = emptySet(),
    val nonColors: Set<Color> = emptySet(),
    /**
     * Source-kind qualities — "hexproof from activated and triggered abilities" (Volatile
     * Stormdrake), projected as `HEXPROOF_FROM_SOURCEKIND_<KIND>`.
     */
    val sourceKinds: Set<com.wingedsheep.engine.mechanics.targeting.SourceKind> = emptySet()
) : Component
