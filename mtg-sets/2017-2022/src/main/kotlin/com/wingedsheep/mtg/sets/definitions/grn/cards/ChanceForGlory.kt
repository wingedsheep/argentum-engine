package com.wingedsheep.mtg.sets.definitions.grn.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.scripting.Duration

/**
 * Chance for Glory
 * {1}{R}{W}
 * Instant
 * Creatures you control gain indestructible. Take an extra turn after this one. At the beginning of
 * that turn's end step, you lose the game.
 *
 * The indestructible grant has no "until end of turn", so it lasts indefinitely on the creatures you
 * control as the spell resolves (a locked group — creatures arriving later don't gain it).
 */
val ChanceForGlory = card("Chance for Glory") {
    manaCost = "{1}{R}{W}"
    colorIdentity = "RW"
    typeLine = "Instant"
    oracleText = "Creatures you control gain indestructible. Take an extra turn after this one. " +
        "At the beginning of that turn's end step, you lose the game."

    spell {
        effect = Patterns.Group.grantKeywordToAll(
            Keyword.INDESTRUCTIBLE,
            Filters.Group.creaturesYouControl,
            Duration.Permanent
        ) then Effects.TakeExtraTurn(loseAtEndStep = true)
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "159"
        artist = "Bram Sels"
        flavorText = "\"It is not your time. The Legion still needs your blade.\""
        imageUri = "https://cards.scryfall.io/normal/front/8/5/85edf6f9-b863-41e2-bb51-857ea2798090.jpg?1783934140"
    }
}
