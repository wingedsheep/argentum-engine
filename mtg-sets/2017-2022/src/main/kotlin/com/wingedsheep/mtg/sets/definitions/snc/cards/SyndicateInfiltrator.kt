package com.wingedsheep.mtg.sets.definitions.snc.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Syndicate Infiltrator
 * {2}{U}{B}
 * Creature — Vampire Wizard
 * 3/3
 *
 * Flying
 * As long as there are five or more mana values among cards in your graveyard, this creature
 * gets +2/+2.
 *
 * The gate is [Conditions.DistinctManaValuesInGraveyard] — distinct mana values, so two cards
 * sharing a mana value count once (lands count as mana value 0).
 */
val SyndicateInfiltrator = card("Syndicate Infiltrator") {
    manaCost = "{2}{U}{B}"
    colorIdentity = "UB"
    typeLine = "Creature — Vampire Wizard"
    power = 3
    toughness = 3
    oracleText = "Flying\n" +
        "As long as there are five or more mana values among cards in your graveyard, this creature gets +2/+2."

    keywords(Keyword.FLYING)

    staticAbility {
        ability = ModifyStats(powerBonus = 2, toughnessBonus = 2, filter = GroupFilter.source())
        condition = Conditions.DistinctManaValuesInGraveyard(5)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "226"
        artist = "Mila Pesic"
        flavorText = "The mansion's vaulted ceilings provided the perfect cover for an ambush."
        imageUri = "https://cards.scryfall.io/normal/front/b/f/bf86c5f2-0caf-48ca-b7ad-a1cdcd014539.jpg?1783923067"
    }
}
