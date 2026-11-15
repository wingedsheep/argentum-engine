package com.wingedsheep.mtg.sets.definitions.dtk.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Radiant Purge
 * {1}{W}
 * Instant
 *
 * Exile target multicolored creature or multicolored enchantment.
 *
 * One target whose filter is "(creature or enchantment) and multicolored" — the multicolored
 * requirement binds to both branches. Colors are read from projected state on the battlefield,
 * so a creature made multicolored (or monocolored) by a continuous effect is judged as it is now.
 */
val RadiantPurge = card("Radiant Purge") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Instant"
    oracleText = "Exile target multicolored creature or multicolored enchantment."

    spell {
        val t = target(
            TargetFilter(GameObjectFilter.CreatureOrEnchantment and GameObjectFilter.Multicolored)
        )
        effect = Effects.Exile(t)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "31"
        artist = "Igor Kieryluk"
        flavorText = "The Shifting Wastes are Dromoka's domain. She will not tolerate intruders."
        imageUri = "https://cards.scryfall.io/normal/front/c/3/c34d94f4-c504-4cf1-9bc7-d318fbee54d7.jpg?1783938614"
    }
}
