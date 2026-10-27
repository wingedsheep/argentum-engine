package com.wingedsheep.mtg.sets.definitions.fut.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersTapped
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.TimingRule

/**
 * Dakmor Salvage — Future Sight #169
 * Land
 *
 * This land enters tapped.
 * {T}: Add {B}.
 * Dredge 2
 *
 * A tapped Swamp with the Ravnica dredge keyword ([KeywordAbility.dredge]).
 */
val DakmorSalvage = card("Dakmor Salvage") {
    manaCost = ""
    colorIdentity = "B"
    typeLine = "Land"
    oracleText = "This land enters tapped.\n" +
        "{T}: Add {B}.\n" +
        "Dredge 2 (If you would draw a card, you may mill two cards instead. If you do, return this card " +
        "from your graveyard to your hand.)"

    replacementEffect(EntersTapped())

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddMana(Color.BLACK)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    keywordAbility(KeywordAbility.dredge(2))

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "169"
        artist = "John Avon"
        imageUri = "https://cards.scryfall.io/normal/front/8/f/8f7f06e8-d8ee-435c-965c-8e1302a8ec10.jpg?1783943090"
    }
}
