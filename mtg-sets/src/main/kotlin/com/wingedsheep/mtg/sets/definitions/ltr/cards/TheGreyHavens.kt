package com.wingedsheep.mtg.sets.definitions.ltr.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TimingRule

/**
 * The Grey Havens
 * Legendary Land
 *
 * When The Grey Havens enters, scry 1.
 * {T}: Add {C}.
 * {T}: Add one mana of any color among legendary creature cards in your graveyard.
 *
 * The third ability uses the new `Effects.AddManaOfColorAmongGraveyard(filter)` (backed by
 * `ManaColorSet.AmongCardsInGraveyard`), which reads the colors among matching cards in your
 * graveyard.
 */
val TheGreyHavens = card("The Grey Havens") {
    typeLine = "Legendary Land"
    oracleText = "When The Grey Havens enters, scry 1.\n" +
        "{T}: Add {C}.\n" +
        "{T}: Add one mana of any color among legendary creature cards in your graveyard."

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = Patterns.Library.scry(1)
    }

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddColorlessMana(1)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddManaOfColorAmongGraveyard(GameObjectFilter.Creature.legendary())
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "255"
        artist = "Alayna Danner"
        flavorText = "There dwelt Círdan the Shipwright, and some say he dwells there still, until the Last Ship sets sail into the West."
        imageUri = "https://cards.scryfall.io/normal/front/d/d/dd698f10-b0fc-42fc-84ec-f5a0d96bfa1d.jpg?1687694648"
    }
}
