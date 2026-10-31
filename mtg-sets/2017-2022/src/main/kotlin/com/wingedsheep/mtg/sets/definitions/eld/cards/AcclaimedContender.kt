package com.wingedsheep.mtg.sets.definitions.eld.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Acclaimed Contender
 * {2}{W}
 * Creature — Human Knight
 * 3/3
 * When this creature enters, if you control another Knight, look at the top five cards of your
 * library. You may reveal a Knight, Aura, Equipment, or legendary artifact card from among them
 * and put it into your hand. Put the rest on the bottom of your library in a random order.
 */
val AcclaimedContender = card("Acclaimed Contender") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Knight"
    oracleText = "When this creature enters, if you control another Knight, look at the top five cards of your library. You may reveal a Knight, Aura, Equipment, or legendary artifact card from among them and put it into your hand. Put the rest on the bottom of your library in a random order."
    power = 3
    toughness = 3

    triggeredAbility {
        trigger = Triggers.self.enters()
        interveningIf = Conditions.YouControl(
            GameObjectFilter.Permanent.withSubtype(Subtype.KNIGHT),
            excludeSelf = true
        )
        effect = Patterns.Library.lookAtTopRevealMatchingToHand(
            count = 5,
            filter = GameObjectFilter.Any.withSubtype(Subtype.KNIGHT) or
                GameObjectFilter.Any.withSubtype(Subtype.AURA) or
                GameObjectFilter.Any.withSubtype(Subtype.EQUIPMENT) or
                GameObjectFilter.Artifact.legendary(),
            prompt = "You may reveal a Knight, Aura, Equipment, or legendary artifact card and put it into your hand"
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "1"
        artist = "David Gaillet"
        imageUri = "https://cards.scryfall.io/normal/front/f/b/fb6b12e7-bb93-4eb6-bad1-b256a6ccff4e.jpg?1783932679"
    }
}
