package com.wingedsheep.mtg.sets.definitions.m20.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.ZonePlacement

/**
 * Risen Reef — Core Set 2020 #217
 * {1}{G}{U} · Creature — Elemental 1/1
 *
 * Whenever this creature or another Elemental you control enters, look at the top card of your
 * library. If it's a land card, you may put it onto the battlefield tapped. If you don't put the
 * card onto the battlefield, put it into your hand.
 *
 * "This creature or another Elemental" is the default any-binding over an Elemental-permanent filter
 * (Risen Reef is itself an Elemental). The look is Fecund Greenshell's pipeline: split the top card
 * into land / nonland, offer up-to-one land onto the battlefield tapped, and everything not put onto
 * the battlefield goes to hand.
 */
val RisenReef = card("Risen Reef") {
    manaCost = "{1}{G}{U}"
    colorIdentity = "GU"
    typeLine = "Creature — Elemental"
    power = 1
    toughness = 1
    oracleText = "Whenever this creature or another Elemental you control enters, look at the top card of your library. " +
        "If it's a land card, you may put it onto the battlefield tapped. If you don't put the card onto the " +
        "battlefield, put it into your hand."

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Permanent.withSubtype(Subtype.ELEMENTAL).youControl()).enters()
        effect = Effects.Pipeline {
            val looked = gather(CardSource.TopOfLibrary(1))
            val (landCards, nonLandCards) = filterSplit(looked, GameObjectFilter.Land)
            val (toBattlefield, landsToHand) = chooseUpToSplit(
                1,
                from = landCards,
                selectedLabel = "Put onto the battlefield tapped",
                remainderLabel = "Put into your hand"
            )
            move(toBattlefield, CardDestination.ToZone(Zone.BATTLEFIELD, placement = ZonePlacement.Tapped))
            toHand(landsToHand)
            toHand(nonLandCards)
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "217"
        artist = "Johan Grenier"
        flavorText = "\"No, not 'washed' ashore. It walked ashore.\"\n—Shafring Hulm, beachcomber"
        imageUri = "https://cards.scryfall.io/normal/front/8/2/82389aaa-9f32-4169-a71c-1aea5af9e935.jpg?1783932948"
        ruling("2019-07-12", "If you put the card into your hand, you don't have to reveal it. You also don't have to tell your opponent whether it's a nonland card or a land card you decided not to put onto the battlefield tapped.")
    }
}
