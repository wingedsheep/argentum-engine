package com.wingedsheep.mtg.sets.definitions.one.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Charge of the Mites — Phyrexia: All Will Be One #6
 * {2}{W}
 * Instant
 * Choose one —
 * • Charge of the Mites deals damage equal to the number of creatures you control to target
 *   creature or planeswalker.
 * • Create two 1/1 colorless Phyrexian Mite artifact creature tokens with toxic 1 and
 *   "This token can't block."
 *
 * The damage amount is counted on resolution (`DynamicAmounts.creaturesYouControl()`).
 */
val ChargeOfTheMites = card("Charge of the Mites") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Instant"
    oracleText = "Choose one —\n" +
        "• Charge of the Mites deals damage equal to the number of creatures you control to target creature or planeswalker.\n" +
        "• Create two 1/1 colorless Phyrexian Mite artifact creature tokens with toxic 1 and \"This token can't block.\" (Players dealt combat damage by them also get a poison counter.)"

    spell {
        modal(chooseCount = 1) {
            mode("Deal damage equal to the number of creatures you control to target creature or planeswalker") {
                val victim = target(Targets.CreatureOrPlaneswalker)
                effect = Effects.DealDamage(DynamicAmounts.creaturesYouControl(), victim)
            }
            mode("Create two 1/1 Phyrexian Mite tokens with toxic 1 that can't block") {
                effect = Effects.CreatePhyrexianMite(2)
            }
        }
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "6"
        artist = "Vladimir Krisetskiy"
        imageUri = "https://cards.scryfall.io/normal/front/4/2/42060b9d-de58-485e-817a-1e64839943aa.jpg?1783918085"
    }
}
