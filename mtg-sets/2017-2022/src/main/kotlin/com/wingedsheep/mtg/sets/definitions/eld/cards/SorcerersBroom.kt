package com.wingedsheep.mtg.sets.definitions.eld.cards

import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Sorcerer's Broom
 * {2}
 * Artifact Creature — Spirit
 * 2/1
 *
 * Whenever you sacrifice another permanent, you may pay {3}. If you do, create a token that's a
 * copy of this creature.
 */
val SorcerersBroom = card("Sorcerer's Broom") {
    manaCost = "{2}"
    colorIdentity = ""
    typeLine = "Artifact Creature — Spirit"
    oracleText = "Whenever you sacrifice another permanent, you may pay {3}. If you do, create a " +
        "token that's a copy of this creature."
    power = 2
    toughness = 1

    triggeredAbility {
        trigger = Triggers.you.sacrificesAnother(GameObjectFilter.Permanent)
        effect = Effects.MayPay(ManaCost.parse("{3}"), Effects.CreateTokenCopyOfSelf())
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "232"
        artist = "Jung Park"
        flavorText = "\"Reynald wished for an army to sweep away his enemies. The fae king smiled.\"\n—*Beyond the Great Henge*"
        imageUri = "https://cards.scryfall.io/normal/front/0/7/071e8f20-18b3-4bf5-a23a-adb42bf5819b.jpg"
    }
}
