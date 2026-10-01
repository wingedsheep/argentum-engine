package com.wingedsheep.mtg.sets.definitions.one.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Molten Rebuke
 * {4}{R}
 * Sorcery
 * Choose one or both —
 * • Molten Rebuke deals 5 damage to target creature or planeswalker.
 * • Destroy target Equipment.
 */
val MoltenRebuke = card("Molten Rebuke") {
    manaCost = "{4}{R}"
    colorIdentity = "R"
    typeLine = "Sorcery"
    oracleText = "Choose one or both —\n• Molten Rebuke deals 5 damage to target creature or planeswalker.\n• Destroy target Equipment."

    spell {
        // "Choose one or both" is the count (CR 700.2), not a third "both" mode.
        modal(chooseCount = 2, minChooseCount = 1) {
            mode("Molten Rebuke deals 5 damage to target creature or planeswalker") {
                val t = target(Targets.CreatureOrPlaneswalker)
                effect = Effects.DealDamage(5, t)
            }
            mode("Destroy target Equipment") {
                val t = target(TargetFilter(GameObjectFilter.Artifact.withSubtype(Subtype.EQUIPMENT)))
                effect = Effects.Destroy(t)
            }
        }
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "141"
        artist = "Eli Minaya"
        flavorText = "\"Perhaps this will show the Orthodoxy what we think of their attempts at indoctrination.\""
        imageUri = "https://cards.scryfall.io/normal/front/d/2/d2e3291b-de5c-4a10-9915-2cd2d84a815c.jpg?1783918027"
    }
}
