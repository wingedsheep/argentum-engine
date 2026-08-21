package com.wingedsheep.mtg.sets.definitions.shm.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.ModifyStats

/**
 * Blight Sickle
 * {2}
 * Artifact — Equipment
 *
 * Equipped creature gets +1/+0 and has wither. (It deals damage to creatures in the form of -1/-1 counters.)
 * Equip {2}
 *
 * - The printed line is one sentence but two independent continuous effects (layer 7c for the stat
 *   bump, layer 6 for the keyword), so it is two [staticAbility] blocks over the same
 *   [Filters.EquippedCreature] group rather than one combined ability.
 * - `equipAbility("{2}")` lowers the printed "Equip {2}" into the sorcery-speed attach ability; the
 *   equip cost field is set by that helper, so it is not written separately.
 */
val BlightSickle = card("Blight Sickle") {
    manaCost = "{2}"
    typeLine = "Artifact — Equipment"
    oracleText = "Equipped creature gets +1/+0 and has wither. (It deals damage to creatures in the form of -1/-1 counters.)\n" +
        "Equip {2}"

    // Equipped creature gets +1/+0 ...
    staticAbility {
        ability = ModifyStats(1, 0, Filters.EquippedCreature)
    }

    // ... and has wither.
    staticAbility {
        ability = GrantKeyword(Keyword.WITHER, Filters.EquippedCreature)
    }

    equipAbility("{2}")

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "247"
        artist = "John Avon"
        flavorText = "Its scars cut deeper than its blade."
        imageUri = "https://cards.scryfall.io/normal/front/c/0/c0f828c7-aedc-462b-8455-46e8a90feb85.jpg?1783942712"
    }
}
