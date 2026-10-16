package com.wingedsheep.mtg.sets.definitions.snc.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Snooping Newsie
 * {U}{B}
 * Creature — Human Rogue
 * 2/2
 *
 * When this creature enters, mill two cards.
 * As long as there are five or more mana values among cards in your graveyard, this creature
 * gets +1/+1 and has lifelink.
 *
 * Both halves of the static share the [Conditions.DistinctManaValuesInGraveyard] gate.
 */
val SnoopingNewsie = card("Snooping Newsie") {
    manaCost = "{U}{B}"
    colorIdentity = "UB"
    typeLine = "Creature — Human Rogue"
    power = 2
    toughness = 2
    oracleText = "When this creature enters, mill two cards. (Put the top two cards of your library into your graveyard.)\n" +
        "As long as there are five or more mana values among cards in your graveyard, this creature gets +1/+1 and has lifelink."

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Patterns.Library.mill(2)
    }

    staticAbility {
        ability = ModifyStats(powerBonus = 1, toughnessBonus = 1, filter = GroupFilter.source())
        condition = Conditions.DistinctManaValuesInGraveyard(5)
    }

    staticAbility {
        ability = GrantKeyword(Keyword.LIFELINK, GroupFilter.source())
        condition = Conditions.DistinctManaValuesInGraveyard(5)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "222"
        artist = "E. M. Gist"
        imageUri = "https://cards.scryfall.io/normal/front/b/e/bed1e957-3df1-4640-9980-783817826602.jpg?1783923068"
    }
}
