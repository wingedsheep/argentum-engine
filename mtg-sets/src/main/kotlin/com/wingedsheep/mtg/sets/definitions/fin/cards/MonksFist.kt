package com.wingedsheep.mtg.sets.definitions.fin.cards

import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.jobSelect
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantSubtype
import com.wingedsheep.sdk.scripting.ModifyStats

/**
 * Monk's Fist
 * {2}
 * Artifact — Equipment
 * Job select (When this Equipment enters, create a 1/1 colorless Hero creature token,
 *   then attach this to it.)
 * Equipped creature gets +1/+0 and is a Monk in addition to its other types.
 * Equip {2}
 */
val MonksFist = card("Monk's Fist") {
    manaCost = "{2}"
    colorIdentity = ""
    typeLine = "Artifact — Equipment"
    oracleText = "Job select (When this Equipment enters, create a 1/1 colorless Hero creature token, then attach this to it.)\n" +
        "Equipped creature gets +1/+0 and is a Monk in addition to its other types.\n" +
        "Equip {2} ({2}: Attach to target creature you control. Equip only as a sorcery.)"

    jobSelect()

    staticAbility {
        ability = ModifyStats(1, 0)
    }
    staticAbility {
        ability = GrantSubtype("Monk", Filters.EquippedCreature)
    }

    equipAbility("{2}")

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "265"
        artist = "Thanh Tuấn"
        imageUri = "https://cards.scryfall.io/normal/front/9/9/995033f0-873d-4e46-b0c9-98ec8ef270ff.jpg?1748706776"
    }
}
