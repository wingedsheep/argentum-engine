package com.wingedsheep.mtg.sets.definitions.neo.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantKeyword

/**
 * Lizard Blades — Kamigawa: Neon Dynasty #153 (canonical printing)
 * {1}{R} · Artifact Creature — Equipment Lizard · 1/1
 *
 * Double strike
 * Equipped creature has double strike.
 * Reconfigure {2}
 */
val LizardBlades = card("Lizard Blades") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Artifact Creature — Equipment Lizard"
    power = 1
    toughness = 1
    oracleText = "Double strike\nEquipped creature has double strike.\n" +
        "Reconfigure {2} ({2}: Attach to target creature you control; or unattach from a creature. " +
        "Reconfigure only as a sorcery. While attached, this isn't a creature.)"

    keywords(Keyword.DOUBLE_STRIKE)

    staticAbility {
        ability = GrantKeyword(Keyword.DOUBLE_STRIKE, Filters.EquippedCreature)
    }

    reconfigure("{2}")

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "153"
        artist = "Jason Kang"
        imageUri = "https://cards.scryfall.io/normal/front/a/7/a747e6cf-c687-4c4f-8e07-d51165d6cb62.jpg?1783923864"
    }
}
