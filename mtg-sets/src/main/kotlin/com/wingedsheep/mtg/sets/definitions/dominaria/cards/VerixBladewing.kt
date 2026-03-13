package com.wingedsheep.mtg.sets.definitions.dominaria.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.conditions.WasKicked
import com.wingedsheep.sdk.scripting.effects.ConditionalEffect
import com.wingedsheep.sdk.scripting.effects.CreateTokenEffect

/**
 * Verix Bladewing
 * {2}{R}{R}
 * Legendary Creature — Dragon
 * 4/4
 * Kicker {3}
 * Flying
 * When Verix Bladewing enters, if it was kicked, create Karox Bladewing,
 * a legendary 4/4 red Dragon creature token with flying.
 */
val VerixBladewing = card("Verix Bladewing") {
    manaCost = "{2}{R}{R}"
    typeLine = "Legendary Creature — Dragon"
    power = 4
    toughness = 4
    oracleText = "Kicker {3}\nFlying\nWhen Verix Bladewing enters, if it was kicked, create Karox Bladewing, a legendary 4/4 red Dragon creature token with flying."

    keywordAbility(KeywordAbility.Kicker(ManaCost.parse("{3}")))

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = ConditionalEffect(
            condition = WasKicked,
            effect = CreateTokenEffect(
                count = 1,
                power = 4,
                toughness = 4,
                colors = setOf(Color.RED),
                creatureTypes = setOf("Dragon"),
                keywords = setOf(Keyword.FLYING),
                name = "Karox Bladewing",
                legendary = true
            )
        )
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "149"
        artist = "XiaoDi Jin"
        imageUri = "https://cards.scryfall.io/normal/front/1/6/16db785c-cf82-4caa-aef6-8c61d9bec7c6.jpg?1562731856"
        ruling("2018-04-27", "Verix Bladewing's triggered ability is functionally identical to \"create a legendary 4/4 red Dragon creature token with flying named Karox Bladewing.\"")
    }
}
