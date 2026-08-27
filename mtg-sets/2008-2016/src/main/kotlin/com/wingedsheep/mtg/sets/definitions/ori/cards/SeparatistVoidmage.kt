package com.wingedsheep.mtg.sets.definitions.ori.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.MayEffect

/**
 * Separatist Voidmage
 * {3}{U}
 * Creature — Human Wizard
 * 2/2
 * When this creature enters, you may return target creature to its owner's hand.
 */
val SeparatistVoidmage = card("Separatist Voidmage") {
    manaCost = "{3}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Human Wizard"
    power = 2
    toughness = 2
    oracleText = "When this creature enters, you may return target creature to its owner's hand."

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        val t = target("target creature", Targets.Creature)
        effect = MayEffect(Effects.ReturnToHand(t))
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "72"
        artist = "Jason Rainville"
        flavorText = "\"As long as each side thinks it can win, the balance holds, and the mage-rings stand.\"\n—Alhammarret"
        imageUri = "https://cards.scryfall.io/normal/front/e/5/e5634d1a-ca4b-4528-9e0e-b88f1025d434.jpg?1783938348"

        ruling("2015-06-22", "Separatist Voidmage's ability can target itself.")
    }
}
