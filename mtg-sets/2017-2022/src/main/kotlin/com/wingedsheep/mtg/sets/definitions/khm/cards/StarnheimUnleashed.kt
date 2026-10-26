package com.wingedsheep.mtg.sets.definitions.khm.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility

/**
 * Starnheim Unleashed
 * {2}{W}{W}
 * Sorcery
 * Create a 4/4 white Angel Warrior creature token with flying and vigilance. If this spell was foretold, create X of those tokens instead.
 * Foretell {X}{X}{W} (During your turn, you may pay {2} and exile this card from your hand face down. Cast it on a later turn for its foretell cost.)
 *
 * The X lives only in the foretell cost: casting from exile announces it, and the resolving spell
 * reads it as [DynamicAmounts.xValue]. [Conditions.WasForetold] (CR 702.143c) picks the X-token
 * branch; a cast from hand makes one token.
 */
val StarnheimUnleashed = card("Starnheim Unleashed") {
    manaCost = "{2}{W}{W}"
    colorIdentity = "W"
    typeLine = "Sorcery"
    oracleText = "Create a 4/4 white Angel Warrior creature token with flying and vigilance. " +
        "If this spell was foretold, create X of those tokens instead.\n" +
        "Foretell {X}{X}{W} (During your turn, you may pay {2} and exile this card from your hand " +
        "face down. Cast it on a later turn for its foretell cost.)"

    spell {
        effect = Effects.If(
            Conditions.WasForetold,
            then = Effects.CreateToken(
                count = DynamicAmounts.xValue(),
                power = 4,
                toughness = 4,
                colors = setOf(Color.WHITE),
                creatureTypes = setOf("Angel", "Warrior"),
                keywords = setOf(Keyword.FLYING, Keyword.VIGILANCE),
            ),
            otherwise = Effects.CreateToken(
                power = 4,
                toughness = 4,
                colors = setOf(Color.WHITE),
                creatureTypes = setOf("Angel", "Warrior"),
                keywords = setOf(Keyword.FLYING, Keyword.VIGILANCE),
            ),
        )
    }

    keywordAbility(KeywordAbility.foretell("{X}{X}{W}"))

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "33"
        artist = "Johannes Voss"
        imageUri = "https://cards.scryfall.io/normal/front/e/a/ea660d42-a441-48d8-98c1-00933cde94a4.jpg?1783928275"
    }
}
