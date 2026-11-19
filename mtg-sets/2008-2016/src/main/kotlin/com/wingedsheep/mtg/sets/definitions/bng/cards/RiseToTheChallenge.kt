package com.wingedsheep.mtg.sets.definitions.bng.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Rise to the Challenge
 * {1}{R}
 * Instant
 *
 * Target creature gets +2/+0 and gains first strike until end of turn.
 *
 * Canonical printing: Born of the Gods, the card's earliest real printing.
 */
val RiseToTheChallenge = card("Rise to the Challenge") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Instant"
    oracleText = "Target creature gets +2/+0 and gains first strike until end of turn."

    spell {
        val creature = target(TargetFilter.Creature)
        effect = Effects.ModifyStats(2, 0, creature) then Effects.GrantKeyword(Keyword.FIRST_STRIKE, creature)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "107"
        artist = "Anthony Palumbo"
        flavorText = "Plidius wagered that Arissa couldn't kill a chimera with her javelins alone. She " +
            "proved him wrong in a way everyone could see for days to come."
        imageUri = "https://cards.scryfall.io/normal/front/c/6/c656ad1b-e85b-4b0c-b538-506afe4a9da5.jpg?1783939543"
    }
}
