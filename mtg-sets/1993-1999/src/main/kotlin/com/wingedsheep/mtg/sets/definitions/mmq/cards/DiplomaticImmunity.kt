package com.wingedsheep.mtg.sets.definitions.mmq.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantKeyword

/**
 * Diplomatic Immunity
 * {1}{U}
 * Enchantment — Aura
 *
 * Enchant creature
 * Shroud
 * Enchanted creature has shroud.
 *
 * Shroud appears twice, in two different SDK positions, and the distinction is the whole card:
 * the Aura's *own* shroud is a printed keyword on the enchantment (`keywords(...)`), while the
 * enchanted creature's is a granted static ([GrantKeyword], default filter = the attached
 * creature). Robe of Mirrors is the grant-only half of this; the printed half is what makes the
 * Aura itself untargetable, so it can't be Disenchanted off with a targeted spell.
 */
val DiplomaticImmunity = card("Diplomatic Immunity") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature\n" +
        "Shroud (A permanent with shroud can't be the target of spells or abilities.)\n" +
        "Enchanted creature has shroud."

    keywords(Keyword.SHROUD)

    auraTarget = Targets.Creature

    staticAbility {
        ability = GrantKeyword(Keyword.SHROUD)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "75"
        artist = "Terese Nielsen"
        imageUri = "https://cards.scryfall.io/normal/front/f/b/fb1e610e-a4a2-460b-8e4c-13674badbce3.jpg"
    }
}
