package com.wingedsheep.mtg.sets.definitions.mmq.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantKeyword

/**
 * Buoyancy
 * {1}{U}
 * Enchantment — Aura
 *
 * Flash
 * Enchant creature
 * Enchanted creature has flying.
 *
 * Mercadian Masques' flash-Aura cycle, the direct sibling of Prophecy's (Greel's Caress,
 * Mageta's Boon, Alexis's Cloak): flash on an Aura needs nothing beyond the keyword — the
 * cast-permission path reads it, and `auraTarget` supplies the "Enchant creature" restriction.
 * The grant is the plain [GrantKeyword] whose default filter is the attached creature.
 */
val Buoyancy = card("Buoyancy") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Enchantment — Aura"
    oracleText = "Flash\n" +
        "Enchant creature\n" +
        "Enchanted creature has flying."

    keywords(Keyword.FLASH)

    auraTarget = Targets.Creature

    staticAbility {
        ability = GrantKeyword(Keyword.FLYING)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "63"
        artist = "Jeff Miracola"
        flavorText = "Saprazzan merfolk become legged on land, but some find it quicker to bring the water with them."
        imageUri = "https://cards.scryfall.io/normal/front/b/2/b208dad2-a412-45fd-b19a-d370426ef5b8.jpg"
    }
}
