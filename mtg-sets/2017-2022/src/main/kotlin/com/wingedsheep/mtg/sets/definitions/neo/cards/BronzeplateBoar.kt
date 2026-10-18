package com.wingedsheep.mtg.sets.definitions.neo.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.ModifyStats

/**
 * Bronzeplate Boar — Kamigawa: Neon Dynasty #135 (canonical printing)
 * {2}{R} · Artifact Creature — Equipment Boar · 3/2
 *
 * Trample
 * Equipped creature gets +3/+2 and has trample.
 * Reconfigure {5}
 */
val BronzeplateBoar = card("Bronzeplate Boar") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Artifact Creature — Equipment Boar"
    power = 3
    toughness = 2
    oracleText = "Trample\nEquipped creature gets +3/+2 and has trample.\n" +
        "Reconfigure {5} ({5}: Attach to target creature you control; or unattach from a creature. " +
        "Reconfigure only as a sorcery. While attached, this isn't a creature.)"

    keywords(Keyword.TRAMPLE)

    staticAbility {
        ability = ModifyStats(+3, +2, Filters.EquippedCreature)
    }

    staticAbility {
        ability = GrantKeyword(Keyword.TRAMPLE, Filters.EquippedCreature)
    }

    reconfigure("{5}")

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "135"
        artist = "Campbell White"
        imageUri = "https://cards.scryfall.io/normal/front/7/c/7c8f5da8-13a8-4b39-8ddc-7661892924af.jpg?1783923871"
    }
}
