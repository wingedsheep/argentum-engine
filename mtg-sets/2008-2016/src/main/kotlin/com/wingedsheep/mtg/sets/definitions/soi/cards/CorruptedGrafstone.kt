package com.wingedsheep.mtg.sets.definitions.soi.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersTapped
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TimingRule

/**
 * Corrupted Grafstone
 * {2}
 * Artifact
 * This artifact enters tapped.
 * {T}: Choose a color of a card in your graveyard. Add one mana of that color.
 *
 * The mana ability is `Effects.AddManaOfColorAmongGraveyard(GameObjectFilter.Any)` — the colors
 * among every card in your graveyard (The Grey Havens' primitive, unfiltered). Colorless cards
 * contribute no color, so an empty or all-colorless graveyard produces no mana (and never {C}).
 */
val CorruptedGrafstone = card("Corrupted Grafstone") {
    manaCost = "{2}"
    colorIdentity = ""
    typeLine = "Artifact"
    oracleText = "This artifact enters tapped.\n" +
        "{T}: Choose a color of a card in your graveyard. Add one mana of that color."

    replacementEffect(EntersTapped())

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddManaOfColorAmongGraveyard(GameObjectFilter.Any)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "253"
        artist = "Bastien L. Deharme"
        flavorText = "It is all too easy to disturb the Blessed Sleep."
        imageUri = "https://cards.scryfall.io/normal/front/1/f/1ffa34cc-4042-4d2b-a7a0-eca0b593eeb6.jpg?1783937708"
        ruling("2016-04-08", "Colorless cards have no color, so you can't add {C} this way.")
    }
}
