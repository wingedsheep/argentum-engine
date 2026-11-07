package com.wingedsheep.mtg.sets.definitions.kld.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ConditionalStaticAbility
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.conditions.Exists
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Renegade Firebrand
 * {2}{R}
 * Creature — Human Warrior
 * 3/2
 * As long as you control a Chandra planeswalker, this creature gets +1/+0 and has first strike.
 * (It deals combat damage before creatures without first strike.)
 *
 * One printed sentence, two [ConditionalStaticAbility] rows: the stat bonus and the keyword grant
 * are separate static abilities that happen to share a condition, because the layer system applies
 * them in different layers. The condition is [Exists] over a battlefield planeswalker with the
 * Chandra subtype — [Player.You] carries the "you control" half, so the filter itself has no
 * controller predicate.
 */
val RenegadeFirebrand = card("Renegade Firebrand") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Human Warrior"
    oracleText = "As long as you control a Chandra planeswalker, this creature gets +1/+0 and has first strike. (It deals combat damage before creatures without first strike.)"
    power = 3
    toughness = 2

    staticAbility {
        ability = ConditionalStaticAbility(
            ability = ModifyStats(1, 0, Filters.Self),
            condition = Exists(
                Player.You,
                Zone.BATTLEFIELD,
                GameObjectFilter.Planeswalker.withSubtype("Chandra")
            )
        )
    }

    staticAbility {
        ability = ConditionalStaticAbility(
            ability = GrantKeyword(Keyword.FIRST_STRIKE, Filters.Self),
            condition = Exists(
                Player.You,
                Zone.BATTLEFIELD,
                GameObjectFilter.Planeswalker.withSubtype("Chandra")
            )
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "268"
        artist = "John Stanko"
        flavorText = "\"For freedom! For fame! For fire!\""
        imageUri = "https://cards.scryfall.io/normal/front/4/b/4b633884-867c-4b51-bd5b-5f246d0ecd4e.jpg?1783937137"
        inBooster = false
    }
}
