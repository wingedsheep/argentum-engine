package com.wingedsheep.mtg.sets.definitions.pip.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.*
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

val ContaminatedDrink = card("Contaminated Drink") {
    manaCost = "{X}{U}{B}"
    typeLine = "Instant"
    oracleText = "Draw X cards, then you get half X rad counters, rounded up."
    spell {
        effect = Effects.DrawCards(DynamicAmounts.xValue()) then
            Effects.AddDynamicCounters(CounterType.RAD, DynamicAmounts.xValue() divRoundedUp 2, EffectTarget.Controller)
    }
    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "99"
        artist = "Craig Elliott"
        imageUri = "https://cards.scryfall.io/normal/front/b/d/bde9eb15-544a-462a-8ab8-86067840c083.jpg?1783912391"
        ruling("2024-03-08", "In a game using the shared team turns option, such as an Archenemy or Two-Headed Giant game, the inherent triggered ability associated with rad counters triggers once for each player on the active team that has rad counters. Each instance of that ability is controlled by one of those players.")
        ruling("2024-03-08", "Any effects (such as proliferate) that interact with counters a player gets, has, or loses can interact with rad counters.")
        ruling("2024-03-08", "The cards are milled all at once, which means abilities that trigger \u201cwhenever one or more nonland cards are milled\u201d will trigger exactly once as long as at least one nonland card was milled.")
        ruling("2024-03-08", "If a player has fewer cards remaining in their library than the number of rad counters they have when the triggered ability resolves, they\u2019ll mill as many cards as they can.")
    }
}
