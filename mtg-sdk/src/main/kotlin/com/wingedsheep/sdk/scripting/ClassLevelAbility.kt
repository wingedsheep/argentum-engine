package com.wingedsheep.sdk.scripting

import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.scripting.ReplacementEffect
import kotlinx.serialization.Serializable

/**
 * Represents a class level upgrade and the abilities gained at that level.
 *
 * Class enchantments (Rule 716) have three levels. Level 1 abilities are defined
 * on the base [com.wingedsheep.sdk.model.CardScript] fields. Levels 2 and 3
 * are represented as ClassLevelAbility entries.
 *
 * When a Class enters the battlefield, it starts at level 1. Players can pay
 * the [cost] as a sorcery-speed activated ability to advance to the next level.
 * Abilities are cumulative — gaining a higher level doesn't remove lower-level abilities.
 * Unlike Sagas, Classes remain on the battlefield after reaching their final level.
 *
 * @param level The class level (2 or 3) — level 1 abilities use the base CardScript fields
 * @param cost The mana cost to level up to this level
 * @param triggeredAbilities Triggered abilities gained at this level
 * @param staticAbilities Static abilities gained at this level
 * @param activatedAbilities Activated abilities gained at this level
 */
@Serializable
data class ClassLevelAbility(
    val level: Int,
    val cost: ManaCost,
    val triggeredAbilities: List<TriggeredAbility> = emptyList(),
    val staticAbilities: List<StaticAbility> = emptyList(),
    val activatedAbilities: List<ActivatedAbility> = emptyList(),
    val replacementEffects: List<ReplacementEffect> = emptyList()
)
