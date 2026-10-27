package com.wingedsheep.mtg.sets.definitions.avr.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Exquisite Blood — Avacyn Restored #102
 * {4}{B} · Enchantment
 *
 * Whenever an opponent loses life, you gain that much life.
 *
 * The trigger fires once per opponent life-loss event (`Triggers.anOpponent.losesLife()`); the
 * amount lost is read from that event ([DynamicAmounts.triggerLifeLost]) — the Bloodthirsty
 * Conqueror shape.
 */
val ExquisiteBlood = card("Exquisite Blood") {
    manaCost = "{4}{B}"
    colorIdentity = "B"
    typeLine = "Enchantment"
    oracleText = "Whenever an opponent loses life, you gain that much life."

    triggeredAbility {
        trigger = Triggers.anOpponent.losesLife()
        effect = Effects.GainLife(DynamicAmounts.triggerLifeLost())
        description = "Whenever an opponent loses life, you gain that much life."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "102"
        artist = "Cynthia Sheppard"
        flavorText = "Even as humans regained the upper hand, some still willingly traded their lives for a chance at immortality."
        imageUri = "https://cards.scryfall.io/normal/front/4/c/4cd54279-57f5-4cd9-b524-4f094bd2fc36.jpg?1783940698"
        ruling("2012-05-01", "Damage dealt to an opponent usually causes that opponent to lose life. An opponent paying life also causes loss of life.")
        ruling(
            "2012-05-01",
            "If you and an opponent both lose life simultaneously, and this causes your life total to become 0 or less, you'll lose the game before Exquisite Blood's triggered ability can resolve."
        )
    }
}
