package com.wingedsheep.mtg.sets.definitions.trk.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.effects.CardSource

/**
 * Temporal Anomaly
 * Land
 *
 * {T}: Add {C}.
 * {3}{G}, {T}, Sacrifice this land: Mill four cards. You may put a permanent card from among them
 * into your hand. Create a tapped Planet land token named New Planet with "{T}: Add one mana of
 * any color."
 */
val TemporalAnomaly = card("Temporal Anomaly") {
    typeLine = "Land"
    oracleText = "{T}: Add {C}.\n{3}{G}, {T}, Sacrifice this land: Mill four cards. You may put a permanent card from among them into your hand. Create a tapped Planet land token named New Planet with \"{T}: Add one mana of any color.\""
    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddColorlessMana(1)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }
    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{3}{G}"), Costs.Tap, Costs.SacrificeSelf)
        effect = Effects.Pipeline {
            val milled = gather(CardSource.TopOfLibrary(4, isMill = true))
            toGraveyard(milled)
            val selected = chooseUpTo(
                1,
                from = milled,
                filter = GameObjectFilter.Permanent,
                showAllCards = true,
                prompt = "You may put a permanent card into your hand",
                selectedLabel = "Put in hand",
                remainderLabel = "Leave in graveyard"
            )
            toHand(selected)
        } then Effects.CreateNewPlanet()
    }
    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "302"
        artist = "Neil Blevins"
        flavorText = "\"Captain, it seems to be a—to be a—to be a...\"\n—Ensign Pavel Chekov"
        imageUri = "https://cards.scryfall.io/normal/front/d/2/d2000219-0a57-48e6-ab0f-d5ad0fb7bcf6.jpg?1793361445"
    }
}
