package com.wingedsheep.mtg.sets.definitions.eld.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Steelclaw Lance — Throne of Eldraine #202
 * {B}{R} · Artifact — Equipment
 *
 * Equipped creature gets +2/+2.
 * Equip Knight {1}
 * Equip {3}
 *
 * "Equip Knight {1}" is an "Equip [quality]" variant (CR 702.6c), authored as a separate equip
 * ability with a Knight-only target filter alongside the plain "Equip {3}" (the Thinking Cap shape).
 */
val SteelclawLance = card("Steelclaw Lance") {
    manaCost = "{B}{R}"
    typeLine = "Artifact — Equipment"
    oracleText = "Equipped creature gets +2/+2.\n" +
        "Equip Knight {1}\n" +
        "Equip {3}"

    // Equipped creature gets +2/+2.
    staticAbility {
        ability = ModifyStats(+2, +2, Filters.EquippedCreature)
    }

    // Equip Knight {1}: attach only to a Knight you control, sorcery speed.
    equipAbility(
        "{1}",
        quality = "Knight",
        targetFilter = TargetFilter.CreatureYouControl.withSubtype(Subtype.KNIGHT),
    )

    // Equip {3}
    equipAbility("{3}")

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "202"
        artist = "Volkan Baǵa"
        flavorText = "\"A traditional lance is fine for the Burning Yard, but in the wilds you need a nastier bite.\"\n" +
            "—Kenver, Embereth weaponsmith"
        imageUri = "https://cards.scryfall.io/normal/front/c/a/cafcf909-d726-4bc2-adf6-c12ca242f0c1.jpg?1783932592"
    }
}
