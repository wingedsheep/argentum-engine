package com.wingedsheep.mtg.sets.definitions.m20.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.SelfAlternativeCost
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Sephara, Sky's Blade — Core Set 2020 #36 (canonical printing)
 * {4}{W}{W}{W} · Legendary Creature — Angel 7/7
 *
 * You may pay {W} and tap four untapped creatures you control with flying rather than pay this
 * spell's mana cost.
 * Flying, lifelink
 * Other creatures you control with flying have indestructible.
 *
 * The alternative cost is Zahid's shape: a `{W}` [SelfAlternativeCost] plus a tap-four additional
 * cost over flying creatures. Summoning-sick flyers may be tapped for it (ruling) — it isn't `{T}`.
 */
val SepharaSkysBlade = card("Sephara, Sky's Blade") {
    manaCost = "{4}{W}{W}{W}"
    colorIdentity = "W"
    typeLine = "Legendary Creature — Angel"
    power = 7
    toughness = 7
    oracleText = "You may pay {W} and tap four untapped creatures you control with flying rather than pay " +
        "this spell's mana cost.\n" +
        "Flying, lifelink\n" +
        "Other creatures you control with flying have indestructible. (Damage and effects that say " +
        "\"destroy\" don't destroy them.)"

    selfAlternativeCost = SelfAlternativeCost(
        manaCost = ManaCost.parse("{W}"),
        additionalCosts = listOf(
            Costs.additional.TapPermanents(
                count = 4,
                filter = GameObjectFilter.Creature.withKeyword(Keyword.FLYING)
            )
        )
    )

    keywords(Keyword.FLYING, Keyword.LIFELINK)

    staticAbility {
        ability = GrantKeyword(
            keyword = Keyword.INDESTRUCTIBLE,
            filter = GroupFilter(
                GameObjectFilter.Creature.withKeyword(Keyword.FLYING).youControl(),
                excludeSelf = true
            )
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "36"
        artist = "Livia Prima"
        imageUri = "https://cards.scryfall.io/normal/front/b/c/bc95881b-8fbf-4d82-b631-5e4404ccc28a.jpg?1783933020"
        ruling(
            "2019-07-12",
            "To pay Sephara's alternative cost, you may tap any untapped creatures you control with flying, " +
                "including ones you haven't controlled continuously since the beginning of your most recent turn."
        )
        ruling(
            "2019-07-12",
            "Because damage remains marked on a creature until it's removed as the turn ends, lethal damage " +
                "dealt to other creatures you control with flying may cause them to be destroyed if Sephara " +
                "leaves the battlefield during that turn."
        )
    }
}
