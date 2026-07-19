package com.wingedsheep.mtg.sets.definitions.msh.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * H.E.R.B.I.E. Scout Unit
 * {4}
 * Artifact Creature — Robot Scout
 * 2/1
 *
 * Flying
 * When this creature enters, draw a card, then you may put a land card from your hand onto the
 * battlefield tapped.
 *
 * The land drop is [Patterns.Hand.putFromHand] (Gather → Select up-to-1 → Move), whose
 * `ChooseUpTo(1)` selection models the "you may" — declining leaves the land in hand.
 * `entersTapped = true` sets the tapped placement.
 */
val HerbieScoutUnit = card("H.E.R.B.I.E. Scout Unit") {
    manaCost = "{4}"
    colorIdentity = ""
    typeLine = "Artifact Creature — Robot Scout"
    power = 2
    toughness = 1
    oracleText = "Flying\n" +
        "When this creature enters, draw a card, then you may put a land card from your hand onto the battlefield tapped."

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = Effects.DrawCards(1) then
            Patterns.Hand.putFromHand(filter = GameObjectFilter.Land, entersTapped = true)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "247"
        artist = "David Álvarez"
        flavorText = "One can never have too many Highly Engineered Robots Built for Interdimensional Exploration around."
        imageUri = "https://cards.scryfall.io/normal/front/3/4/3496d0d4-3fab-4ea4-8986-afe5a7155ec6.jpg?1783902891"
    }
}
