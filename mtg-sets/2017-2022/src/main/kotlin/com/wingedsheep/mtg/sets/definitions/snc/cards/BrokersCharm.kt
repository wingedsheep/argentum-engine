package com.wingedsheep.mtg.sets.definitions.snc.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Brokers Charm
 * {G}{W}{U}
 * Instant
 * Choose one —
 * • Target creature you control gets +1/+0 until end of turn. It deals damage equal to its power to
 *   target creature or planeswalker an opponent controls.
 * • Destroy target enchantment.
 * • Draw two cards.
 *
 * Mode one is Huatli's Final Strike's bite: the pump resolves first, so the damage reads the pumped
 * power. With `damageSource` set to your creature, an illegal creature target skips the damage
 * (no fallback to the spell as source), while an illegal opposing target still leaves the +1/+0.
 */
val BrokersCharm = card("Brokers Charm") {
    manaCost = "{G}{W}{U}"
    colorIdentity = "GUW"
    typeLine = "Instant"
    oracleText = "Choose one —\n" +
        "• Target creature you control gets +1/+0 until end of turn. It deals damage equal to its power " +
        "to target creature or planeswalker an opponent controls.\n" +
        "• Destroy target enchantment.\n" +
        "• Draw two cards."

    spell {
        modal(chooseCount = 1) {
            mode("Your creature gets +1/+0, then deals damage equal to its power to a creature or planeswalker an opponent controls") {
                val yours = target(TargetFilter.Creature.youControl())
                val theirs = target(TargetFilter(GameObjectFilter.CreatureOrPlaneswalker.opponentControls()))
                effect = Effects.ModifyStats(1, 0, yours) then
                    Effects.DealDamage(DynamicAmounts.powerOf(yours), theirs, damageSource = yours)
            }
            mode("Destroy target enchantment") {
                val enchantment = target(TargetFilter.Enchantment)
                effect = Effects.Destroy(enchantment)
            }
            mode("Draw two cards") {
                effect = Effects.DrawCards(2)
            }
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "171"
        artist = "Steve Argyle"
        imageUri = "https://cards.scryfall.io/normal/front/7/4/74c9c315-1cf4-468e-a74a-b5f3be4a63a1.jpg?1783923094"
        ruling(
            "2022-04-29",
            "You can't choose the first mode for Brokers Charm unless your opponent has a creature or " +
                "planeswalker to target."
        )
        ruling(
            "2022-04-29",
            "If you choose the first mode and your creature is not a legal target as Brokers Charm resolves, " +
                "no damage will be dealt."
        )
        ruling(
            "2022-04-29",
            "If you choose the first mode and your opponent's creature or planeswalker is not a legal target " +
                "as Brokers Charm resolves, your creature will still get +1/+0 until end of turn."
        )
    }
}
