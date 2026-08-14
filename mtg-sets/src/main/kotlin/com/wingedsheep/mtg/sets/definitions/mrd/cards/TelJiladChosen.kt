package com.wingedsheep.mtg.sets.definitions.mrd.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.ProtectionScope

/**
 * Tel-Jilad Chosen — Mirrodin #132
 * {1}{G} · Creature — Elf Warrior · 2/1
 *
 * Protection from artifacts
 *
 * Green's answer to a set built entirely out of artifacts. Modelled with the card-type protection
 * scope ([ProtectionScope.CardType]), projected as `PROTECTION_FROM_CARDTYPE_ARTIFACT`, so
 * targeting, damage prevention, and block evasion all honour it without per-card wiring — an
 * artifact creature can't block it, an artifact source can't damage it, and an artifact's ability
 * can't target it.
 */
val TelJiladChosen = card("Tel-Jilad Chosen") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Elf Warrior"
    power = 2
    toughness = 1
    oracleText = "Protection from artifacts"

    keywordAbility(KeywordAbility.Protection(ProtectionScope.CardType("Artifact")))

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "132"
        artist = "Matthew D. Wilson"
        flavorText = "\"It is my honor to keep safe Tel-Jilad's secrets, not to know them.\""
        imageUri = "https://cards.scryfall.io/normal/front/e/e/eefea84f-d657-491d-b60d-63e6a61e9eb2.jpg?1783944532"
    }
}
