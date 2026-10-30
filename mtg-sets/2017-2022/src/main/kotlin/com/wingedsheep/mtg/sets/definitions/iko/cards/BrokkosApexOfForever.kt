package com.wingedsheep.mtg.sets.definitions.iko.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.MayCastSelfFromZones

/**
 * Brokkos, Apex of Forever
 * {2}{B}{G}{U}
 * Legendary Creature — Nightmare Beast Elemental
 * 6/6
 * Mutate {2}{U/B}{G}{G}
 * Trample
 * You may cast this card from your graveyard using its mutate ability.
 *
 * The graveyard permission is restricted to the mutate cast (`castUsing = Keyword.MUTATE`), so from
 * the graveyard Brokkos must be cast for its mutate cost onto a non-Human creature you own — never
 * for its mana cost (2020-04-17 ruling). Timing is unchanged: creature-spell timing.
 */
val BrokkosApexOfForever = card("Brokkos, Apex of Forever") {
    manaCost = "{2}{B}{G}{U}"
    colorIdentity = "BGU"
    typeLine = "Legendary Creature — Nightmare Beast Elemental"
    power = 6
    toughness = 6
    oracleText = "Mutate {2}{U/B}{G}{G} (If you cast this spell for its mutate cost, put it over or under target non-Human creature you own. They mutate into the creature on top plus all abilities from under it.)\n" +
        "Trample\n" +
        "You may cast this card from your graveyard using its mutate ability."

    keywordAbility(KeywordAbility.mutate("{2}{U/B}{G}{G}"))
    keywords(Keyword.TRAMPLE)

    staticAbility {
        ability = MayCastSelfFromZones(listOf(Zone.GRAVEYARD), castUsing = Keyword.MUTATE)
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "179"
        artist = "Filip Burburan"
        imageUri = "https://cards.scryfall.io/normal/front/c/9/c9f07625-fbd8-4581-8568-eb3cfb2a4c1e.jpg?1783931028"
        ruling("2020-04-17", "Casting Brokkos with the permission granted by its last ability doesn't change when you can cast it.")
        ruling("2020-04-17", "If you cast Brokkos with the permission granted by its last ability, you must pay its mutate cost to cast it. You can't choose to pay its mana cost or another alternative cost.")
        ruling("2020-04-17", "If another effect gives you permission to cast Brokkos from your graveyard, you may cast it for any cost associated with that permission.")
    }
}
