package com.wingedsheep.mtg.sets.definitions.lorwyneclipsed.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility

/**
 * Stratosoarer
 * {4}{U}
 * Creature — Elemental
 * 3/5
 *
 * Flying
 * When this creature enters, target creature gains flying until end of turn.
 * Basic landcycling {1}{U} ({1}{U}, Discard this card: Search your library for a basic land card,
 * reveal it, put it into your hand, then shuffle.)
 */
val Stratosoarer = card("Stratosoarer") {
    manaCost = "{4}{U}"
    typeLine = "Creature — Elemental"
    power = 3
    toughness = 5
    oracleText = "Flying\nWhen this creature enters, target creature gains flying until end of turn.\nBasic landcycling {1}{U}"

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        val creature = target("creature", Targets.Creature)
        effect = Effects.GrantKeyword(Keyword.FLYING, creature)
    }

    keywordAbility(KeywordAbility.BasicLandcycling(ManaCost.parse("{1}{U}")))

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "72"
        artist = "John Tedrick"
        flavorText = "Wise travelers take advantage of the wind that always blows in its wake."
        imageUri = "https://cards.scryfall.io/normal/front/4/f/4f607889-6f3f-4511-8920-88a39f3b28ca.jpg?1767871852"
    }
}
