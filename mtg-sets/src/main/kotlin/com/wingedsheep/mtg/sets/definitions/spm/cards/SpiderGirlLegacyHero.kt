package com.wingedsheep.mtg.sets.definitions.spm.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ConditionalStaticAbility
import com.wingedsheep.sdk.scripting.GrantKeyword

/**
 * Spider-Girl, Legacy Hero
 * {G}{W}
 * Legendary Creature — Spider Human Hero, 2/2
 *
 * During your turn, Spider-Girl has flying.
 * When Spider-Girl leaves the battlefield, create a 1/1 green and white Human Citizen creature token.
 *
 * The conditional flying is a time-restricted static keyword grant to self
 * ([ConditionalStaticAbility] over [GrantKeyword] on [Filters.Self], gated by
 * [Conditions.IsYourTurn]) — same shape as Shocker, Unshakable's "during your turn, first strike".
 * The leaves-the-battlefield trigger reuses the broader [Triggers.LeavesBattlefield] (fires on
 * death, exile, or bounce) and the shared [Effects.CreateToken] for the 1/1 GW Human Citizen token
 * (cf. News Helicopter's identical token).
 */
val SpiderGirlLegacyHero = card("Spider-Girl, Legacy Hero") {
    manaCost = "{G}{W}"
    colorIdentity = "GW"
    typeLine = "Legendary Creature — Spider Human Hero"
    oracleText = "During your turn, Spider-Girl has flying.\n" +
        "When Spider-Girl leaves the battlefield, create a 1/1 green and white Human Citizen creature token."
    power = 2
    toughness = 2

    staticAbility {
        ability = ConditionalStaticAbility(
            ability = GrantKeyword(Keyword.FLYING, Filters.Self),
            condition = Conditions.IsYourTurn,
        )
    }

    triggeredAbility {
        trigger = Triggers.LeavesBattlefield
        effect = Effects.CreateToken(
            power = 1,
            toughness = 1,
            colors = setOf(Color.GREEN, Color.WHITE),
            creatureTypes = setOf("Human", "Citizen"),
        )
        description = "When Spider-Girl leaves the battlefield, create a 1/1 green and white Human Citizen creature token."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "149"
        artist = "Lixin Yin"
        flavorText = "On Earth-982, May \"Mayday\" Parker, the first child of Peter and Mary Jane, continues her father's legacy."
        imageUri = "https://cards.scryfall.io/normal/front/d/1/d1f3196a-fe48-446f-ab07-00c66b7816c8.jpg?1783905311"
    }
}
