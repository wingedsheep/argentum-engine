package com.wingedsheep.mtg.sets.definitions.edgeofeternities.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.scripting.targets.TargetPermanent
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Exosuit Savior
 * {2}{W}
 * Creature — Human Soldier
 * Flying
 * When this creature enters, return up to one other target permanent you control to its owner's hand.
 */
val ExosuitSavior = card("Exosuit Savior") {
    manaCost = "{2}{W}"
    typeLine = "Creature — Human Soldier"
    power = 2
    toughness = 2
    oracleText = "Flying\nWhen this creature enters, return up to one other target permanent you control to its owner's hand."

    // Flying keyword
    keywords(Keyword.FLYING)

    // When this creature enters, return up to one other target permanent you control to hand
    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        val permanent = target(
            "other permanent you control",
            TargetPermanent(optional = true, filter = TargetFilter.PermanentYouControl.other())
        )
        effect = Effects.ReturnToHand(permanent)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "16"
        artist = "Benjamin Ee"
        flavorText = "\"An Astelli once saved me just like this. I aspire to live up to their example every day.\""
        imageUri = "https://cards.scryfall.io/normal/front/8/2/826c0455-a6ce-43ad-bd5c-0a5df169da90.jpg?1752946614"
    }
}
