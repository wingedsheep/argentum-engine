package com.wingedsheep.mtg.sets.definitions.mh3.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.ForEachTargetEffect
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.effects.MoveCollectionEffect
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.TargetPlayer
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Thraben Charm
 * {1}{W}
 * Instant
 * Choose one —
 * • Thraben Charm deals damage equal to twice the number of creatures you control to target creature.
 * • Destroy target enchantment.
 * • Exile any number of target players' graveyards.
 *
 * Standard `modal(chooseCount = 1)` charm shape. Mode 1's damage is
 * `DynamicAmount.Multiply(DynamicAmounts.creaturesYouControl(), 2)`. Mode 3 targets any number of
 * players (`TargetPlayer(unlimited = true)`) and iterates the chosen players with
 * [ForEachTargetEffect], gathering each one's graveyard (`CardSource.FromZone(Zone.GRAVEYARD,
 * Player.ContextPlayer(0))`) and moving it to exile — the Hollow Marauder per-target idiom.
 */
val ThrabenCharm = card("Thraben Charm") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Instant"
    oracleText = "Choose one —\n" +
        "• Thraben Charm deals damage equal to twice the number of creatures you control to target creature.\n" +
        "• Destroy target enchantment.\n" +
        "• Exile any number of target players' graveyards."

    spell {
        modal(chooseCount = 1) {
            mode("Thraben Charm deals damage equal to twice the number of creatures you control to target creature") {
                val creature = target("target creature", Targets.Creature)
                effect = Effects.DealDamage(
                    DynamicAmount.Multiply(DynamicAmounts.creaturesYouControl(), 2),
                    creature,
                )
            }
            mode("Destroy target enchantment") {
                val enchantment = target("target enchantment", Targets.Enchantment)
                effect = Effects.Destroy(enchantment)
            }
            mode("Exile any number of target players' graveyards") {
                target("any number of target players", TargetPlayer(unlimited = true))
                effect = ForEachTargetEffect(
                    listOf(
                        GatherCardsEffect(
                            source = CardSource.FromZone(Zone.GRAVEYARD, Player.ContextPlayer(0)),
                            storeAs = "tc_graveyard",
                        ),
                        MoveCollectionEffect(
                            from = "tc_graveyard",
                            destination = CardDestination.ToZone(Zone.EXILE),
                        ),
                    ),
                )
            }
        }
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "45"
        artist = "Carlos Palma Cruchaga"
        imageUri = "https://cards.scryfall.io/normal/front/d/d/dd28a646-f38f-4cdf-948c-969cd979e5e6.jpg?1783911295"
    }
}
