package com.wingedsheep.mtg.sets.definitions.ltr.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantKeyword

/**
 * Trailblazer's Boots
 * {2}
 * Artifact — Equipment
 *
 * Equipped creature has nonbasic landwalk. (It can't be blocked as long as defending player
 * controls a nonbasic land.)
 * Equip {2}
 *
 * Gap 24 (nonbasic landwalk): adds the `Keyword.NONBASIC_LANDWALK` keyword + the evasion check in
 * `BlockEvasionRules.LandwalkRule` (unblockable while the defending player controls any non-basic
 * land). Granted to the equipped creature via the standard `GrantKeyword(..., Filters.EquippedCreature)`
 * static.
 */
val TrailblazersBoots = card("Trailblazer's Boots") {
    manaCost = "{2}"
    typeLine = "Artifact — Equipment"
    oracleText = "Equipped creature has nonbasic landwalk. (It can't be blocked as long as defending " +
        "player controls a nonbasic land.)\n" +
        "Equip {2}"

    staticAbility {
        ability = GrantKeyword(Keyword.NONBASIC_LANDWALK, Filters.EquippedCreature)
    }

    equipAbility("{2}")

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "398"
        flavorText = "\"Not idly do the leaves of Lórien fall.\"\n—Aragorn"
        artist = "Alexander Gering"
        imageUri = "https://cards.scryfall.io/normal/front/3/6/36ee958c-ae8c-49bd-9d85-0128ca19901c.jpg?1778748786"
    }
}
