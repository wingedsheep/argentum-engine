package com.wingedsheep.mtg.sets.definitions.iko.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Flame Spill
 * {2}{R}
 * Instant
 * Flame Spill deals 4 damage to target creature. Excess damage is dealt to that creature's
 * controller instead.
 *
 * Same shape as Gandalf's Sanction: [Effects.DealDamageExcessToController] deals only the lethal
 * portion to the creature and the excess to its controller.
 */
val FlameSpill = card("Flame Spill") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Instant"
    oracleText = "Flame Spill deals 4 damage to target creature. " +
        "Excess damage is dealt to that creature's controller instead."

    spell {
        val t = target(TargetFilter.Creature)
        effect = Effects.DealDamageExcessToController(DynamicAmounts.fixed(4), t)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "117"
        artist = "Zoltan Boros"
        flavorText = "Fragile, floating Skysail suddenly seemed like an appealingly safe place to live."
        imageUri = "https://cards.scryfall.io/normal/front/b/3/b3090004-d7dd-47bc-92e5-977be4fd9ae5.jpg?1783931049"
        ruling("2020-04-17", "Excess damage caused by a spell or ability is similar to how combat damage from a creature with trample is handled. Start with the amount of damage being dealt to the creature and determine what is “lethal.” This is the creature’s toughness minus the amount of damage that it already has marked on it, but ignoring any replacement or prevention effects that will modify this damage. Also ignore whether the creature has an ability such as indestructible that will result in it not being destroyed by this damage.")
        ruling("2020-04-17", "Once you’ve determined how much damage is excess, Flame Spill simultaneously deals damage to the creature and to its controller. This damage may be modified by replacement or prevention effects.")
        ruling("2020-04-17", "If the target creature is an illegal target by the time Flame Spill tries to resolve, the spell won’t resolve. It won’t deal damage to any player.")
    }
}
