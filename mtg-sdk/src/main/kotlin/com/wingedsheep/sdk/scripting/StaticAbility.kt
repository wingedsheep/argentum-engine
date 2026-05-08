package com.wingedsheep.sdk.scripting

import com.wingedsheep.sdk.scripting.text.TextReplaceable
import kotlinx.serialization.Serializable

/**
 * Static abilities provide continuous effects that don't use the stack.
 * These include effects from enchantments, equipment, and other permanents.
 *
 * Static abilities are data objects - application is handled by the ECS
 * layer system (StateProjector) which calculates the projected game state.
 *
 * Concrete subtypes are organized into categorized files:
 * - KeywordStaticAbilities.kt - keyword grants
 * - AbilityGrantStaticAbilities.kt - triggered/activated ability grants
 * - StatsStaticAbilities.kt - power/toughness modifications
 * - CombatStaticAbilities.kt - attack/block restrictions, combat damage
 * - BlockingStaticAbilities.kt - blocking evasion and restrictions
 * - CostStaticAbilities.kt - spell cost modifications
 * - TypeStaticAbilities.kt - type/subtype/color changes
 * - ProtectionStaticAbilities.kt - protection and targeting restrictions
 * - SpellStaticAbilities.kt - casting permissions and spell modifications
 * - MiscStaticAbilities.kt - miscellaneous static abilities
 *
 * Each concrete subtype carries a `filter: GroupFilter` field that determines
 * which permanents the ability affects. Use [com.wingedsheep.sdk.scripting.filters.unified.GroupFilter.source]
 * for "this creature", [com.wingedsheep.sdk.scripting.filters.unified.GroupFilter.attachedCreature]
 * for "the attached/equipped/enchanted creature", or any battlefield-scoped filter
 * for lord/sliver-style "all X creatures have ..." effects.
 */
@Serializable
sealed interface StaticAbility : TextReplaceable<StaticAbility> {
    val description: String
}
