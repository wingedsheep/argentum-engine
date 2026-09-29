package com.wingedsheep.mtg.sets.definitions.mh3.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.*
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

val JoltedAwake = card("Jolted Awake") {
    manaCost = "{W}"
    typeLine = "Sorcery"
    oracleText = "Choose up to one target artifact or creature card in your graveyard. You get {E}{E} (two energy counters). Then you may pay an amount of {E} equal to that card's mana value. If you do, return it from your graveyard to the battlefield.\nCycling {2} ({2}, Discard this card: Draw a card.)"

    keywordAbility(KeywordAbility.cycling("{2}"))
    spell {
        val card = target(TargetFilter(Filters.Unified.artifact.or(Filters.Creature).ownedByYou(), zone = Zone.GRAVEYARD), optional = true)
        effect = Effects.GetEnergy(2) then Effects.If(
            Conditions.TargetMatchesFilter(Filters.Unified.artifact.or(Filters.Creature), target = card),
            Effects.MayPay(
                cost = Effects.PayExactCounters(CounterType.ENERGY, DynamicAmounts.manaValueOf(card)),
                then = Effects.PutOntoBattlefieldFromGraveyard(card)
            )
        )
    }
    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "33"
        artist = "Steven Russell Black"
        imageUri = "https://cards.scryfall.io/normal/front/f/7/f773a79f-ad0f-45c9-bcf5-44ba5e992729.jpg?1783911298"
        ruling("2024-06-07", "If a card in your graveyard has {X} in its mana cost, X is 0 when determining its mana value.")
    }
}
