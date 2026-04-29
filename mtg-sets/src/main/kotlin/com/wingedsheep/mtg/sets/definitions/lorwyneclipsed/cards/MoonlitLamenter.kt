package com.wingedsheep.mtg.sets.definitions.lorwyneclipsed.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersWithCounters
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.events.CounterTypeFilter

/**
 * Moonlit Lamenter
 * {2}{W}
 * Creature — Treefolk Cleric
 * 2/5
 *
 * This creature enters with a -1/-1 counter on it.
 * {1}{W}, Remove a counter from this creature: Draw a card. Activate only as a sorcery.
 */
val MoonlitLamenter = card("Moonlit Lamenter") {
    manaCost = "{2}{W}"
    typeLine = "Creature — Treefolk Cleric"
    power = 2
    toughness = 5
    oracleText = "This creature enters with a -1/-1 counter on it.\n" +
        "{1}{W}, Remove a counter from this creature: Draw a card. Activate only as a sorcery."

    replacementEffect(EntersWithCounters(
        counterType = CounterTypeFilter.MinusOneMinusOne,
        count = 1,
        selfOnly = true
    ))

    activatedAbility {
        cost = Costs.Composite(
            Costs.Mana("{1}{W}"),
            Costs.RemoveCounterFromSelf(Counters.MINUS_ONE_MINUS_ONE)
        )
        effect = Effects.DrawCards(1)
        timing = TimingRule.SorcerySpeed
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "26"
        artist = "Steve Ellis"
        flavorText = "Isilu's adherents mourn the dead, purging grief through keening rituals."
        imageUri = "https://cards.scryfall.io/normal/front/f/b/fb8fc509-cff6-470f-abf6-b07f6c3f94e1.jpg?1767956948"
    }
}
