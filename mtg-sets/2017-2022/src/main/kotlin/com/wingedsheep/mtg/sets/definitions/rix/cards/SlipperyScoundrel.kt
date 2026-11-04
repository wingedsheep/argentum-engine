package com.wingedsheep.mtg.sets.definitions.rix.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantBeBlocked
import com.wingedsheep.sdk.scripting.ConditionalStaticAbility
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Slippery Scoundrel — Rivals of Ixalan #55
 * {2}{U} · Creature — Human Pirate · 2/2
 *
 * Ascend (If you control ten or more permanents, you get the city's blessing for the rest of the game.)
 * As long as you have the city's blessing, this creature has hexproof and can't be blocked.
 *
 * The Ascend keyword plus two [ConditionalStaticAbility]s sharing
 * [Conditions.YouHaveCitysBlessing]: a self [GrantKeyword] for hexproof and [CantBeBlocked].
 */
val SlipperyScoundrel = card("Slippery Scoundrel") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Human Pirate"
    power = 2
    toughness = 2
    oracleText = "Ascend (If you control ten or more permanents, you get the city's blessing for the rest of the game.)\n" +
        "As long as you have the city's blessing, this creature has hexproof and can't be blocked."

    keywords(Keyword.ASCEND)

    staticAbility {
        ability = ConditionalStaticAbility(
            ability = GrantKeyword(Keyword.HEXPROOF, GroupFilter.source()),
            condition = Conditions.YouHaveCitysBlessing
        )
    }

    staticAbility {
        ability = ConditionalStaticAbility(
            ability = CantBeBlocked(),
            condition = Conditions.YouHaveCitysBlessing
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "55"
        artist = "Dan Murayama Scott"
        flavorText = "\"I'd rather be caught red-handed than leave empty-handed!\""
        imageUri = "https://cards.scryfall.io/normal/front/2/6/262f3c10-f29a-494f-88c6-0d44b2cbdc82.jpg?1783935320"
        ruling(
            "2018-01-19",
            "Once Slippery Scoundrel has become blocked, getting the city's blessing won't cause it to become unblocked."
        )
        ruling(
            "2018-01-19",
            "Once you have the city's blessing, you have it for the rest of the game, even if you lose control of some or all of your permanents. " +
                "The city's blessing isn't a permanent itself and can't be removed by any effect."
        )
    }
}
