package com.wingedsheep.mtg.sets.definitions.one.cards

import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.forMirrodin
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ModifyStats

/**
 * Vulshok Splitter
 * {3}{R}
 * Artifact — Equipment
 * For Mirrodin! (When this Equipment enters, create a 2/2 red Rebel creature token, then attach this to it.)
 * Equipped creature gets +2/+0.
 * Equip {2}{R}
 */
val VulshokSplitter = card("Vulshok Splitter") {
    manaCost = "{3}{R}"
    colorIdentity = "R"
    typeLine = "Artifact — Equipment"
    oracleText = "For Mirrodin! (When this Equipment enters, create a 2/2 red Rebel creature token, then attach this to it.)\n" +
        "Equipped creature gets +2/+0.\n" +
        "Equip {2}{R} ({2}{R}: Attach to target creature you control. Equip only as a sorcery.)"

    forMirrodin()

    staticAbility {
        ability = ModifyStats(2, 0, Filters.EquippedCreature)
    }

    equipAbility("{2}{R}")

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "156"
        artist = "Kai Carpenter"
        flavorText = "\"Not my style, but it'll get the job done.\"\n—Nahiri"
        imageUri = "https://cards.scryfall.io/normal/front/f/0/f05078dd-b928-4b3f-9636-f299ebac180b.jpg?1783918021"
    }
}
