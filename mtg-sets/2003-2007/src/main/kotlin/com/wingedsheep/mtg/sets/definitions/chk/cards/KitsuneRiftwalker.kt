package com.wingedsheep.mtg.sets.definitions.chk.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.ProtectionScope

/**
 * Kitsune Riftwalker
 * {1}{W}{W}
 * Creature — Fox Wizard
 * 2/1
 * Protection from Spirits and from Arcane
 *
 * Two subtype protections: Spirit is a creature type, Arcane a spell type (CR 205.3k). Both project
 * as `PROTECTION_FROM_SUBTYPE_<X>`, which targeting, damage prevention, and blocking all read against
 * the source's subtypes — so an Arcane spell can't target it and a Spirit can't block it or damage it.
 */
val KitsuneRiftwalker = card("Kitsune Riftwalker") {
    manaCost = "{1}{W}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Fox Wizard"
    oracleText = "Protection from Spirits and from Arcane"
    power = 2
    toughness = 1
    keywordAbility(KeywordAbility.Protection(ProtectionScope.Subtype("Spirit")))
    keywordAbility(KeywordAbility.Protection(ProtectionScope.Subtype("Arcane")))
    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "29"
        artist = "Pete Venters"
        flavorText = "The wake of his passage shoved aside the influence of the kami."
        imageUri = "https://cards.scryfall.io/normal/front/f/5/f545d9d0-52d8-4b90-a8bb-178f4ba3c4b7.jpg?1783944336"
    }
}
