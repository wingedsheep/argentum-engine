package com.wingedsheep.mtg.sets.definitions.lea.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

val Sacrifice = card("Sacrifice") {
    manaCost = "{B}"
    colorIdentity = "B"
    typeLine = "Instant"
    oracleText = "As an additional cost to cast this spell, sacrifice a creature.\n" +
        "Add an amount of {B} equal to the sacrificed creature's mana value."
    additionalCost(Costs.additional.SacrificePermanent(GameObjectFilter.Creature))
    spell { effect = Effects.AddMana(Color.BLACK, DynamicAmounts.sacrificedManaValue()) }
    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "124"
        artist = "Dan Frazier"
        imageUri = "https://cards.scryfall.io/normal/front/1/2/12164aee-6a27-4246-8d15-2d6dd20d92e9.jpg?1783948692"
        ruling("2013-04-15", "You must sacrifice exactly one creature to cast this spell; you cannot cast it without sacrificing a creature, and you cannot sacrifice additional creatures.")
        ruling("2013-04-15", "Players can only respond once this spell has been cast and all its costs have been paid. No one can try to destroy the creature you sacrificed to prevent you from casting this spell.")
        ruling("2004-10-04", "Sacrificing an animated land gives no mana since the converted mana cost was zero.")
    }
}
