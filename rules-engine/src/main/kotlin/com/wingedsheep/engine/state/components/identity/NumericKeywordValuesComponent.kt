package com.wingedsheep.engine.state.components.identity

import com.wingedsheep.engine.state.Component
import com.wingedsheep.sdk.core.Keyword
import kotlinx.serialization.Serializable

/**
 * The printed N of each numeric keyword on the card ("bushido 2" → `BUSHIDO to 2`), summed per
 * keyword across instances. Seeded at entity creation from `KeywordAbility.Numeric` on the card
 * definition, so `EntityNumericProperty.KeywordValue` can read it inside the layer projection,
 * which has no card registry to ask. Toxic is left to [ToxicComponent], which projects its N as a
 * `TOXIC_<n>` keyword instead.
 */
@Serializable
data class NumericKeywordValuesComponent(val values: Map<Keyword, Int>) : Component
