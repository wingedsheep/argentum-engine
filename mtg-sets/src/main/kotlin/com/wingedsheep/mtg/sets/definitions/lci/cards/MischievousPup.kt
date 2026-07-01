package com.wingedsheep.mtg.sets.definitions.lci.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetPermanent

/**
 * Mischievous Pup
 * {2}{W}
 * Creature — Dog
 * 3/1
 * Flash
 * When this creature enters, return up to one other target permanent you control
 * to its owner's hand.
 */
val MischievousPup = card("Mischievous Pup") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Dog"
    power = 3
    toughness = 1
    oracleText = "Flash (You may cast this spell any time you could cast an instant.)\n" +
        "When this creature enters, return up to one other target permanent you control to its owner's hand."

    keywords(Keyword.FLASH)

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        val permanent = target(
            "other permanent you control",
            TargetPermanent(optional = true, filter = TargetFilter.PermanentYouControl.other())
        )
        effect = Effects.ReturnToHand(permanent)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "25"
        artist = "Devin Platts"
        flavorText = "\"Either he made a gnome very angry when he took that, or he'll make one very happy " +
            "when he brings it back.\"\n—Ihio, Oteclan market vendor"
        imageUri = "https://cards.scryfall.io/normal/front/7/c/7c219861-f808-456f-b551-37512764062d.jpg?1782694591"
    }
}
