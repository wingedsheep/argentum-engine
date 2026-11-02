package com.wingedsheep.mtg.sets.definitions.grn.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Disinformation Campaign
 * {1}{U}{B}
 * Enchantment
 * When this enchantment enters, you draw a card and each opponent discards a card.
 * Whenever you surveil, return this enchantment to its owner's hand.
 */
val DisinformationCampaign = card("Disinformation Campaign") {
    manaCost = "{1}{U}{B}"
    colorIdentity = "UB"
    typeLine = "Enchantment"
    oracleText = "When this enchantment enters, you draw a card and each opponent discards a card.\n" +
        "Whenever you surveil, return this enchantment to its owner's hand."

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.DrawCards(1) then Effects.EachOpponentDiscards(1)
    }

    triggeredAbility {
        trigger = Triggers.you.surveils()
        effect = Effects.Move(EffectTarget.Self, Zone.HAND)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "167"
        artist = "Anthony Palumbo"
        flavorText = "\"People would sooner savor sweet lies than chew plain truths.\"\n—Lazav"
        imageUri = "https://cards.scryfall.io/normal/front/6/9/69a79ff3-58ed-4cc2-9ebc-0edbb86cd6fb.jpg?1783934137"
    }
}
