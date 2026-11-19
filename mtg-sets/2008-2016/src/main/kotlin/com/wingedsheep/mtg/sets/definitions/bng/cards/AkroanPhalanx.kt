package com.wingedsheep.mtg.sets.definitions.bng.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Akroan Phalanx
 * {3}{W}
 * Creature — Human Soldier
 * 3/3
 *
 * Vigilance
 * {2}{R}: Creatures you control get +1/+0 until end of turn.
 *
 * Canonical printing: Born of the Gods, the card's earliest real printing.
 */
val AkroanPhalanx = card("Akroan Phalanx") {
    manaCost = "{3}{W}"
    colorIdentity = "RW"
    typeLine = "Creature — Human Soldier"
    power = 3
    toughness = 3
    oracleText = "Vigilance\n" +
        "{2}{R}: Creatures you control get +1/+0 until end of turn."

    keywords(Keyword.VIGILANCE)

    activatedAbility {
        cost = Costs.Mana("{2}{R}")
        effect = Patterns.Group.modifyStatsForAll(1, 0, GroupFilter.AllCreaturesYouControl)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "2"
        artist = "Steve Prescott"
        flavorText = "Shields up, spears out, heels set, hearts firm."
        imageUri = "https://cards.scryfall.io/normal/front/f/e/fe23019a-e432-4a27-8acb-b17728a1e8b0.jpg?1783939586"
    }
}
