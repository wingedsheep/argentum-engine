package com.wingedsheep.mtg.sets.definitions.shm.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.ProtectionScope

/**
 * Oversoul of Dusk
 * {G/W}{G/W}{G/W}{G/W}{G/W}
 * Creature — Spirit Avatar
 * 5 / 5
 *
 * Protection from blue, from black, and from red
 *
 * - CR 702.16g: "protection from [quality A] and from [quality B]" is shorthand for three (here)
 *   *separate* protection abilities, so this is three [KeywordAbility.Protection] instances with a
 *   single-colour [ProtectionScope.Color] each — not one [ProtectionScope.Colors] holding a set.
 *   That matters when something removes or counts abilities individually.
 * - The five-symbol monocoloured-hybrid cost goes in `manaCost` verbatim; mana value (5) is
 *   derived by the parser, and the card is green-white regardless of how it was paid.
 */
val OversoulOfDusk = card("Oversoul of Dusk") {
    manaCost = "{G/W}{G/W}{G/W}{G/W}{G/W}"
    typeLine = "Creature — Spirit Avatar"
    power = 5
    toughness = 5
    oracleText = "Protection from blue, from black, and from red"

    keywordAbility(KeywordAbility.Protection(ProtectionScope.Color(Color.BLUE)))
    keywordAbility(KeywordAbility.Protection(ProtectionScope.Color(Color.BLACK)))
    keywordAbility(KeywordAbility.Protection(ProtectionScope.Color(Color.RED)))

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "234"
        artist = "Scott M. Fischer"
        flavorText = "\"Some say she hid the sun herself, a desperate act to save it from its ultimate extinction.\"\n" +
            "—*The Seer's Parables*"
        imageUri = "https://cards.scryfall.io/normal/front/8/8/8893842f-aa3f-45f6-8139-ca775d33792b.jpg?1783942716"
    }
}
