package com.wingedsheep.mtg.sets.definitions.neo.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantKeyword

/**
 * Leech Gauntlet — Kamigawa: Neon Dynasty #106 (canonical printing)
 * {1}{B} · Artifact Creature — Equipment Leech · 2/2
 *
 * Lifelink
 * Equipped creature has lifelink.
 * Reconfigure {4}
 */
val LeechGauntlet = card("Leech Gauntlet") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Artifact Creature — Equipment Leech"
    power = 2
    toughness = 2
    oracleText = "Lifelink\nEquipped creature has lifelink.\n" +
        "Reconfigure {4} ({4}: Attach to target creature you control; or unattach from a creature. " +
        "Reconfigure only as a sorcery. While attached, this isn't a creature.)"

    keywords(Keyword.LIFELINK)

    staticAbility {
        ability = GrantKeyword(Keyword.LIFELINK, Filters.EquippedCreature)
    }

    reconfigure("{4}")

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "106"
        artist = "Robin Olausson"
        imageUri = "https://cards.scryfall.io/normal/front/9/7/97986b67-5ae4-492f-a9e0-81f57a829d86.jpg?1783923882"
    }
}
