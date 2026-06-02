package com.wingedsheep.mtg.sets.definitions.tdm.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetPermanent

/**
 * Coordinated Maneuver — Tarkir: Dragonstorm #6
 * {1}{W} · Instant · Common
 *
 * Choose one —
 * • Coordinated Maneuver deals damage equal to the number of creatures you control to
 *   target creature or planeswalker.
 * • Destroy target enchantment.
 */
val CoordinatedManeuver = card("Coordinated Maneuver") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Instant"
    oracleText = "Choose one —\n" +
        "• Coordinated Maneuver deals damage equal to the number of creatures you control to " +
        "target creature or planeswalker.\n" +
        "• Destroy target enchantment."

    spell {
        modal(chooseCount = 1) {
            mode("Deal damage equal to the number of creatures you control to target creature or planeswalker") {
                val t = target("target creature or planeswalker", Targets.CreatureOrPlaneswalker)
                effect = Effects.DealDamage(DynamicAmounts.creaturesYouControl(), t)
            }
            mode("Destroy target enchantment") {
                val t = target("target enchantment", TargetPermanent(filter = TargetFilter.Enchantment))
                effect = Effects.Move(t, Zone.GRAVEYARD, byDestruction = true)
            }
        }
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "6"
        artist = "Wisnu Tan"
        flavorText = "\"We follow the dragonstorms, and we take down any threats that emerge from them!\"\n" +
            "—Zurgo, khan of the Mardu"
        imageUri = "https://cards.scryfall.io/normal/front/c/6/c6569487-53c5-4b91-877d-e4e31bfa90c0.jpg?1743203976"
    }
}
