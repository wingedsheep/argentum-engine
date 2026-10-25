package com.wingedsheep.mtg.sets.definitions.afr.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Icingdeath, Frost Tyrant
 * {2}{W}{W}
 * Legendary Creature — Dragon
 * 4/3
 * Flying, vigilance
 * When Icingdeath, Frost Tyrant dies, create Icingdeath, Frost Tongue, a legendary white Equipment
 * artifact token with "Equipped creature gets +2/+0," "Whenever equipped creature attacks, tap target
 * creature defending player controls," and equip {2}.
 *
 * The token is the predefined `Icingdeath, Frost Tongue` in `PredefinedTokens`.
 */
val IcingdeathFrostTyrant = card("Icingdeath, Frost Tyrant") {
    manaCost = "{2}{W}{W}"
    colorIdentity = "W"
    typeLine = "Legendary Creature — Dragon"
    power = 4
    toughness = 3
    oracleText = "Flying, vigilance\n" +
        "When Icingdeath, Frost Tyrant dies, create Icingdeath, Frost Tongue, a legendary white Equipment " +
        "artifact token with \"Equipped creature gets +2/+0,\" \"Whenever equipped creature attacks, tap " +
        "target creature defending player controls,\" and equip {2}."

    keywords(Keyword.FLYING, Keyword.VIGILANCE)

    triggeredAbility {
        trigger = Triggers.self.dies()
        effect = Effects.CreatePredefinedToken("Icingdeath, Frost Tongue")
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "20"
        artist = "Matt Stewart"
        imageUri = "https://cards.scryfall.io/normal/front/2/6/26b0e68a-9d4e-42ea-8dac-c47cbed0c6ad.jpg?1783926531"
    }
}
