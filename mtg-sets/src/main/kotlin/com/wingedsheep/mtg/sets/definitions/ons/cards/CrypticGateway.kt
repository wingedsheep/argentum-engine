package com.wingedsheep.mtg.sets.definitions.ons.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.effects.GatherSubtypesEffect
import com.wingedsheep.sdk.scripting.effects.MoveCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectFromCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectionMode
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import com.wingedsheep.sdk.dsl.Effects

/**
 * Cryptic Gateway
 * {5}
 * Artifact
 * Tap two untapped creatures you control: You may put a creature card from your hand
 * that shares a creature type with each creature tapped this way onto the battlefield.
 */
val CrypticGateway = card("Cryptic Gateway") {
    manaCost = "{5}"
    colorIdentity = ""
    typeLine = "Artifact"
    oracleText = "Tap two untapped creatures you control: You may put a creature card from your hand that shares a creature type with each creature tapped this way onto the battlefield."

    activatedAbility {
        cost = Costs.TapPermanents(2, GameObjectFilter.Creature)
        effect = Effects.Composite(listOf(
            GatherCardsEffect(source = CardSource.TappedAsCost, storeAs = "tappedPermanents"),
            GatherSubtypesEffect(from = "tappedPermanents", storeAs = "tappedSubtypes"),
            GatherCardsEffect(source = CardSource.FromZone(zone = Zone.HAND, player = Player.You, filter = GameObjectFilter.Creature.withSubtypeInEachStoredGroup("tappedSubtypes")), storeAs = "candidates"),
            SelectFromCollectionEffect(from = "candidates", selection = SelectionMode.ChooseUpTo(DynamicAmount.Fixed(1)), storeSelected = "chosen", prompt = "You may put a creature card from your hand onto the battlefield"),
            MoveCollectionEffect(from = "chosen", destination = CardDestination.ToZone(Zone.BATTLEFIELD))
        ))
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "306"
        artist = "Mark Tedin"
        flavorText = "\"Its lock changes to fit each key.\""
        imageUri = "https://cards.scryfall.io/normal/front/7/f/7f379966-6a0a-434c-8682-1cf528a9a4a1.jpg?1562925013"
    }
}
