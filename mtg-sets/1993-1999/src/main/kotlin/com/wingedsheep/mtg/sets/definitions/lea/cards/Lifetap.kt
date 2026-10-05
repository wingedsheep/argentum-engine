package com.wingedsheep.mtg.sets.definitions.lea.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

val Lifetap = card("Lifetap") {
    manaCost = "{U}{U}"
    typeLine = "Enchantment"
    oracleText = "Whenever a Forest an opponent controls becomes tapped, you gain 1 life."
    colorIdentity = "U"
    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Land.withSubtype(Subtype.FOREST).opponentControls()).becomesTapped()
        effect = Effects.GainLife(1)
    }
    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "61"
        artist = "Anson Maddocks"
        imageUri = "https://cards.scryfall.io/normal/front/1/1/11add837-7ee4-4104-b031-c161bce459ae.jpg?1783948705"
        ruling("2004-10-04", "Gives one life for each and every Forest tapped.")
        ruling("2004-10-04", "In multi-player games it affects all opponents.")
    }
}
