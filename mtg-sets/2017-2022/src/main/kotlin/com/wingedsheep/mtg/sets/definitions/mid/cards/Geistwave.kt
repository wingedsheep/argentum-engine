package com.wingedsheep.mtg.sets.definitions.mid.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Geistwave
 * {1}{U}
 * Instant
 *
 * Return target nonland permanent to its owner's hand. If you controlled that permanent, draw a card.
 *
 * The control test only holds while the permanent is still on the battlefield, so it is
 * evaluated up front (as Boomerang Basics does): both branches bounce, the true branch also draws.
 */
val Geistwave = card("Geistwave") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Return target nonland permanent to its owner's hand. If you controlled that permanent, draw a card."

    spell {
        val permanent = target(TargetFilter.NonlandPermanent)
        effect = Effects.If(
            condition = Conditions.TargetMatchesFilter(GameObjectFilter.NonlandPermanent.youControl(), permanent),
            then = Effects.ReturnToHand(permanent) then Effects.DrawCards(1),
            otherwise = Effects.ReturnToHand(permanent),
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "56"
        artist = "Olena Richards"
        flavorText = "Necro-alchemists gained popular support during the Travails, when geist-fueled weapons helped defend towns against mad cultists and Eldrazi monstrosities."
        imageUri = "https://cards.scryfall.io/normal/front/f/0/f0e3d1ac-bec2-4bba-a6cf-a823996dde2d.jpg?1783925641"
    }
}
