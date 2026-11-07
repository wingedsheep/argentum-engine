package com.wingedsheep.mtg.sets.definitions.emn.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Boon of Emrakul — Eldritch Moon #81
 * {2}{B} · Enchantment — Aura
 *
 * Enchant creature
 * Enchanted creature gets +3/-3.
 *
 * The Immolation shape: a single [ModifyStats] over the enchanted creature. On anything with three
 * or less toughness it is removal (the creature dies to the state-based action); on a bigger body
 * it is a +3 power pump with a fragile toughness.
 */
val BoonOfEmrakul = card("Boon of Emrakul") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature\n" +
        "Enchanted creature gets +3/-3."

    auraTarget = TargetObject(filter = TargetFilter.Creature)

    staticAbility {
        ability = ModifyStats(3, -3)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "81"
        artist = "Svetlin Velinov"
        flavorText = "All around Emrakul, life re-formed in her image."
        imageUri = "https://cards.scryfall.io/normal/front/2/4/24bb97c6-d2ff-459f-986e-73e30534d865.jpg"
    }
}
