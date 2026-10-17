package com.wingedsheep.mtg.sets.definitions.snc.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Unlicensed Hearse
 * {2}
 * Artifact — Vehicle
 * * / *
 * {T}: Exile up to two target cards from a single graveyard.
 * Unlicensed Hearse's power and toughness are each equal to the number of cards exiled with it.
 * Crew 2
 *
 * "Single graveyard" is `sameOwner = true` (the Digsite Conservator shape). Each exiled card is
 * linked to the Hearse (`linkToSource`), and the characteristic-defining P/T reads the size of that
 * linked-exile pile, which drops cards that later leave exile and resets when the Hearse changes zones.
 */
val UnlicensedHearse = card("Unlicensed Hearse") {
    manaCost = "{2}"
    typeLine = "Artifact — Vehicle"
    oracleText = "{T}: Exile up to two target cards from a single graveyard.\n" +
        "Unlicensed Hearse's power and toughness are each equal to the number of cards exiled with it.\n" +
        "Crew 2 (Tap any number of creatures you control with total power 2 or more: This Vehicle becomes an artifact creature until end of turn.)"
    dynamicStats(DynamicAmounts.linkedExileCardCount())

    activatedAbility {
        cost = Costs.Tap
        targets(TargetFilter.CardInGraveyard, count = 2, optional = true, sameOwner = true)
        effect = Effects.ForEachTarget(
            Effects.Move(EffectTarget.ContextTarget(0), Zone.EXILE, linkToSource = true)
        )
        description = "{T}: Exile up to two target cards from a single graveyard."
    }

    keywordAbility(KeywordAbility.crew(2))

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "246"
        artist = "Chris Seaman"
        imageUri = "https://cards.scryfall.io/normal/front/9/3/93ee60f7-31dd-4bc6-b71f-57a1a0d19d20.jpg?1783923059"
        ruling(
            "2022-04-29",
            "If you activated the crew ability of Unlicensed Hearse before exiling any cards with its first ability, its power and toughness will be 0/0. Unless there is another effect increasing its toughness, it will be put into its owner's graveyard as a state-based action."
        )
    }
}
