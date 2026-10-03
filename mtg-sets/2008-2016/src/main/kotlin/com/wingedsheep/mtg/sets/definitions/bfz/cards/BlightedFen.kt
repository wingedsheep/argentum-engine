package com.wingedsheep.mtg.sets.definitions.bfz.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TimingRule

/**
 * Blighted Fen
 * Land
 * {T}: Add {C}.
 * {4}{B}, {T}, Sacrifice this land: Target opponent sacrifices a creature of their choice.
 *
 * The Blighted cycle shape (see Blighted Gorge): a colorless mana ability plus a sacrifice-self
 * activated ability whose effect is the Cruel Edict [Effects.Sacrifice] aimed at a target opponent.
 */
val BlightedFen = card("Blighted Fen") {
    manaCost = ""
    colorIdentity = "B"
    typeLine = "Land"
    oracleText = "{T}: Add {C}.\n" +
        "{4}{B}, {T}, Sacrifice this land: Target opponent sacrifices a creature of their choice."

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddColorlessMana(1)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{4}{B}"), Costs.Tap, Costs.SacrificeSelf)
        val opponent = target(Targets.Opponent)
        effect = Effects.Sacrifice(GameObjectFilter.Creature, 1, opponent)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "230"
        artist = "Jonas De Ro"
        flavorText = "\"We came to a place where the skin of Zendikar was peeled back and its bones lay bare to the sky.\"\n—Greenweaver Mina"
        imageUri = "https://cards.scryfall.io/normal/front/2/9/29d02950-cd50-4662-97af-3106598dc3c4.jpg?1783938175"
    }
}
