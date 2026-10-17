package com.wingedsheep.mtg.sets.definitions.snc.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Aven Heartstabber
 * {U}{B}
 * Creature — Bird Assassin
 * 1/1
 *
 * Flying
 * As long as there are five or more mana values among cards in your graveyard, this creature
 * gets +2/+2 and has deathtouch.
 * When this creature dies, mill two cards, then draw a card.
 *
 * Both halves of the static share the [Conditions.DistinctManaValuesInGraveyard] gate.
 */
val AvenHeartstabber = card("Aven Heartstabber") {
    manaCost = "{U}{B}"
    colorIdentity = "UB"
    typeLine = "Creature — Bird Assassin"
    power = 1
    toughness = 1
    oracleText = "Flying\n" +
        "As long as there are five or more mana values among cards in your graveyard, this creature gets +2/+2 and has deathtouch.\n" +
        "When this creature dies, mill two cards, then draw a card."

    keywords(Keyword.FLYING)

    staticAbility {
        ability = ModifyStats(powerBonus = 2, toughnessBonus = 2, filter = GroupFilter.source())
        condition = Conditions.DistinctManaValuesInGraveyard(5)
    }

    staticAbility {
        ability = GrantKeyword(Keyword.DEATHTOUCH, GroupFilter.source())
        condition = Conditions.DistinctManaValuesInGraveyard(5)
    }

    triggeredAbility {
        trigger = Triggers.self.dies()
        effect = Patterns.Library.mill(2) then Effects.DrawCards(1)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "166"
        artist = "Dan Murayama Scott"
        imageUri = "https://cards.scryfall.io/normal/front/a/c/acef329c-61e6-41c1-8d89-ff2417949839.jpg?1783923095"
    }
}
