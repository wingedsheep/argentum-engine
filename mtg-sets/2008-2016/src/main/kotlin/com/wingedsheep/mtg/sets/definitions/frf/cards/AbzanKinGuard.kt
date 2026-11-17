package com.wingedsheep.mtg.sets.definitions.frf.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ConditionalStaticAbility
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeyword

/**
 * Abzan Kin-Guard — Fate Reforged #120
 * {3}{G} · Creature — Human Warrior · 3 / 3
 *
 * This creature has lifelink as long as you control a white or black permanent.
 *
 * A [GrantKeyword] over [Filters.Self] gated on an `Exists` over your battlefield for any
 * permanent that is white or black (projected colours, so a permanent recoloured by an effect
 * counts). The grant is re-evaluated on every projection, so lifelink switches on and off as the
 * board changes. Same shape as [CliffrunnerBehemoth] and the colour check of [AbzanRunemark].
 */
val AbzanKinGuard = card("Abzan Kin-Guard") {
    manaCost = "{3}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Human Warrior"
    power = 3
    toughness = 3
    oracleText = "This creature has lifelink as long as you control a white or black permanent."

    staticAbility {
        ability = ConditionalStaticAbility(
            ability = GrantKeyword(Keyword.LIFELINK, Filters.Self),
            condition = Conditions.YouControl(GameObjectFilter.Permanent.withAnyColor(Color.WHITE, Color.BLACK)),
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "120"
        artist = "Craig J Spearing"
        flavorText = "\"The Mardu rush headlong toward extinction. We ensure the longevity of our clan by " +
            "protecting our territory, not rampaging through it.\""
        imageUri = "https://cards.scryfall.io/normal/front/c/e/cec0ef55-57fd-404f-b9f6-b5d41e77b58c.jpg?1783938682"
    }
}
