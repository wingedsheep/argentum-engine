package com.wingedsheep.mtg.sets.definitions.mmq.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.ModifyStats

/**
 * Tiger Claws
 * {2}{G}
 * Enchantment — Aura
 *
 * Flash
 * Enchant creature
 * Enchanted creature gets +1/+1 and has trample.
 *
 * The green member of Mercadian Masques' flash-Aura cycle. Same two-static shape as
 * [FlamingSword]: [ModifyStats] for the pump, [GrantKeyword] for the evasion word, both scoped
 * by default to the attached creature.
 */
val TigerClaws = card("Tiger Claws") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Enchantment — Aura"
    oracleText = "Flash\n" +
        "Enchant creature\n" +
        "Enchanted creature gets +1/+1 and has trample."

    keywords(Keyword.FLASH)

    auraTarget = Targets.Creature

    staticAbility {
        ability = ModifyStats(1, 1)
    }

    staticAbility {
        ability = GrantKeyword(Keyword.TRAMPLE)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "279"
        artist = "Adam Rex"
        flavorText = "Cho-Arrim martial artists emulate the beasts of their home."
        imageUri = "https://cards.scryfall.io/normal/front/0/1/0146a689-4817-4849-a90d-4cc64566960d.jpg"
    }
}
