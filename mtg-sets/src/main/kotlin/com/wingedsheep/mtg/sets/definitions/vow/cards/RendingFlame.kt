package com.wingedsheep.mtg.sets.definitions.vow.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.ConditionalEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Rending Flame
 * {2}{R}
 * Instant
 *
 * Rending Flame deals 5 damage to target creature or planeswalker. If that permanent is a
 * Spirit, Rending Flame also deals 2 damage to that permanent's controller.
 *
 * A single target ([Targets.CreatureOrPlaneswalker]) takes 5 damage; a
 * [ConditionalEffect] gated on [Conditions.TargetMatchesFilter] (target is a Spirit) adds 2
 * damage to that permanent's controller ([EffectTarget.TargetController]) — the YipYip
 * "if that permanent is a [type]" rider idiom, with the controller-of-target damage sink
 * from Heated Argument.
 */
val RendingFlame = card("Rending Flame") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Instant"
    oracleText = "Rending Flame deals 5 damage to target creature or planeswalker. If that " +
        "permanent is a Spirit, Rending Flame also deals 2 damage to that permanent's controller."

    spell {
        val permanent = target("target creature or planeswalker", Targets.CreatureOrPlaneswalker)
        effect = Effects.Composite(
            Effects.DealDamage(5, permanent),
            ConditionalEffect(
                condition = Conditions.TargetMatchesFilter(GameObjectFilter.Any.withSubtype(Subtype.SPIRIT)),
                effect = Effects.DealDamage(2, EffectTarget.TargetController)
            )
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "175"
        artist = "Olena Richards"
        flavorText = "\"It is our duty to bring the Blessed Sleep to the dead, even if they resist " +
            "that gift.\"\n—Grete, Order of Saint Traft"
        imageUri = "https://cards.scryfall.io/normal/front/5/1/51332c31-41df-4379-aa63-6a734a4df618.jpg?1782703065"
    }
}
