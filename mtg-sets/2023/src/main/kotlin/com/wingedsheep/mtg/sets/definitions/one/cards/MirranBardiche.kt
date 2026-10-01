package com.wingedsheep.mtg.sets.definitions.one.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.forMirrodin
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.ModifyStats

/**
 * Mirran Bardiche
 * {4}{W}
 * Artifact — Equipment
 * For Mirrodin! (When this Equipment enters, create a 2/2 red Rebel creature token, then attach this to it.)
 * Equipped creature gets +2/+1 and has vigilance.
 * Equip {3}{W}
 */
val MirranBardiche = card("Mirran Bardiche") {
    manaCost = "{4}{W}"
    colorIdentity = "W"
    typeLine = "Artifact — Equipment"
    oracleText = "For Mirrodin! (When this Equipment enters, create a 2/2 red Rebel creature token, then attach this to it.)\n" +
        "Equipped creature gets +2/+1 and has vigilance.\n" +
        "Equip {3}{W} ({3}{W}: Attach to target creature you control. Equip only as a sorcery.)"

    forMirrodin()

    staticAbility {
        ability = ModifyStats(2, 1, Filters.EquippedCreature)
    }

    staticAbility {
        ability = GrantKeyword(Keyword.VIGILANCE, Filters.EquippedCreature)
    }

    equipAbility("{3}{W}")

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "22"
        artist = "Julian Kok Joon Wen"
        imageUri = "https://cards.scryfall.io/normal/front/3/0/3006ea5a-5391-41eb-b0c8-092741dca2eb.jpg?1783918079"
    }
}
