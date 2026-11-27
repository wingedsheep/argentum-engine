package com.wingedsheep.mtg.sets.definitions.avr.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantColor
import com.wingedsheep.sdk.scripting.GrantSubtype
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Ghoulflesh
 * {B}
 * Enchantment — Aura
 * Enchant creature
 * Enchanted creature gets -1/-1 and is a black Zombie in addition to its other colors and types.
 */
val Ghoulflesh = card("Ghoulflesh") {
    manaCost = "{B}"
    colorIdentity = "B"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature\n" +
        "Enchanted creature gets -1/-1 and is a black Zombie in addition to its other colors and types."

    auraTarget = TargetObject(filter = TargetFilter.Creature)

    staticAbility { ability = ModifyStats(-1, -1) }
    staticAbility { ability = GrantColor(Color.BLACK, Filters.EnchantedCreature) }
    staticAbility { ability = GrantSubtype(Subtype.ZOMBIE.value, Filters.EnchantedCreature) }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "103"
        artist = "Igor Kieryluk"
        flavorText = "The body dies in stages. First the skin, then the muscle. The brain is last, " +
            "much to the victim's dismay."
        imageUri = "https://cards.scryfall.io/normal/front/2/e/2eed3d1b-3142-437c-99e9-85ba76e23e6d.jpg?1783940699"
    }
}
