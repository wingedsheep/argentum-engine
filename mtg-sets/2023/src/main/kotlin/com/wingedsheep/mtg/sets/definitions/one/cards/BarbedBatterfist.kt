package com.wingedsheep.mtg.sets.definitions.one.cards

import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.forMirrodin
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ModifyStats

/**
 * Barbed Batterfist
 * {1}{R}
 * Artifact — Equipment
 * For Mirrodin! (When this Equipment enters, create a 2/2 red Rebel creature token, then attach this to it.)
 * Equipped creature gets +1/-1.
 * Equip {1}
 */
val BarbedBatterfist = card("Barbed Batterfist") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Artifact — Equipment"
    oracleText = "For Mirrodin! (When this Equipment enters, create a 2/2 red Rebel creature token, then attach this to it.)\n" +
        "Equipped creature gets +1/-1.\n" +
        "Equip {1} ({1}: Attach to target creature you control. Equip only as a sorcery.)"

    forMirrodin()

    staticAbility {
        ability = ModifyStats(1, -1, Filters.EquippedCreature)
    }

    equipAbility("{1}")

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "121"
        artist = "Randy Gallegos"
        flavorText = "\"Finally, a safe way to punch the rotters in the face.\""
        imageUri = "https://cards.scryfall.io/normal/front/d/e/de1d02d1-91dc-47d6-bdbe-87602428abfb.jpg?1783918036"
    }
}
