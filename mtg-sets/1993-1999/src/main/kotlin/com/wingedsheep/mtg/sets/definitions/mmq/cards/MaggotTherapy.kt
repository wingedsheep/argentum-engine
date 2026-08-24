package com.wingedsheep.mtg.sets.definitions.mmq.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ModifyStats

/**
 * Maggot Therapy
 * {2}{B}
 * Enchantment — Aura
 *
 * Flash
 * Enchant creature
 * Enchanted creature gets +2/-2.
 *
 * The black member of Mercadian Masques' flash-Aura cycle — Greel's Caress with the sign
 * flipped onto toughness. A single [ModifyStats] static; the toughness reduction is a layer-7c
 * modification, so a creature already at 2 toughness dies to the state-based action rather than
 * to damage.
 */
val MaggotTherapy = card("Maggot Therapy") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Enchantment — Aura"
    oracleText = "Flash\n" +
        "Enchant creature\n" +
        "Enchanted creature gets +2/-2."

    keywords(Keyword.FLASH)

    auraTarget = Targets.Creature

    staticAbility {
        ability = ModifyStats(2, -2)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "145"
        artist = "Jeff Easley"
        flavorText = "\"If this is the cure, I'd hate to see the disease.\"\n" +
            "—Orim"
        imageUri = "https://cards.scryfall.io/normal/front/6/a/6ab963aa-2304-4ee6-a8c7-c485c5133b40.jpg"
    }
}
