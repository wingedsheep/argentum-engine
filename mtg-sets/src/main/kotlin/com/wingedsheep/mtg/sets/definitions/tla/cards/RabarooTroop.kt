package com.wingedsheep.mtg.sets.definitions.tla.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Rabaroo Troop
 * {3}{W}{W}
 * Creature — Rabbit Kangaroo
 * 3/5
 *
 * Landfall — Whenever a land you control enters, this creature gains flying
 * until end of turn and you gain 1 life.
 * Plainscycling {2} ({2}, Discard this card: Search your library for a Plains
 * card, reveal it, put it into your hand, then shuffle.)
 */
val RabarooTroop = card("Rabaroo Troop") {
    manaCost = "{3}{W}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Rabbit Kangaroo"
    power = 3
    toughness = 5
    oracleText = "Landfall — Whenever a land you control enters, this creature gains flying until end of turn and you gain 1 life.\n" +
        "Plainscycling {2} ({2}, Discard this card: Search your library for a Plains card, reveal it, put it into your hand, then shuffle.)"

    // Landfall: whenever a land you control enters, this creature gains flying
    // until end of turn and you gain 1 life.
    triggeredAbility {
        trigger = Triggers.LandYouControlEnters
        effect = Effects.Composite(
            Effects.GrantKeyword(Keyword.FLYING, EffectTarget.Self),
            Effects.GainLife(1),
        )
    }

    // Plainscycling {2}
    keywordAbility(KeywordAbility.typecycling("Plains", ManaCost.parse("{2}")))

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "32"
        artist = "Mizutametori"
        flavorText = "Rabaroos are famous enjoyers of hopping and infamous enjoyers of cabbage."
        imageUri = "https://cards.scryfall.io/normal/front/6/2/62a983b4-ac73-4949-9317-05a75a8ce164.jpg?1764120104"
    }
}
