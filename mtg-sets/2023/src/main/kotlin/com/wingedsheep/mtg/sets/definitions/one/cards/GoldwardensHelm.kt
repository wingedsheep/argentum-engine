package com.wingedsheep.mtg.sets.definitions.one.cards

import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.forMirrodin
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ModifyStats

/**
 * Goldwarden's Helm
 * {2}{W}
 * Artifact — Equipment
 * For Mirrodin! (When this Equipment enters, create a 2/2 red Rebel creature token, then attach this to it.)
 * Equipped creature gets +0/+1.
 * Equip {1}{W}
 */
val GoldwardensHelm = card("Goldwarden's Helm") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Artifact — Equipment"
    oracleText = "For Mirrodin! (When this Equipment enters, create a 2/2 red Rebel creature token, then attach this to it.)\n" +
        "Equipped creature gets +0/+1.\n" +
        "Equip {1}{W} ({1}{W}: Attach to target creature you control. Equip only as a sorcery.)"

    forMirrodin()

    staticAbility {
        ability = ModifyStats(0, 1, Filters.EquippedCreature)
    }

    equipAbility("{1}{W}")

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "13"
        artist = "Vincent Christiaens"
        flavorText = "\"My mother wore this into battle against the first Phyrexians. Now I wear it in her honor.\""
        imageUri = "https://cards.scryfall.io/normal/front/f/4/f4057210-ef07-4496-94c8-95c3b807c23c.jpg?1783918082"
    }
}
