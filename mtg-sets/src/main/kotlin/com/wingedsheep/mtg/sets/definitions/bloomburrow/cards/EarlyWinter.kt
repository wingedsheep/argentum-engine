package com.wingedsheep.mtg.sets.definitions.bloomburrow.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Early Winter
 * {4}{B}
 * Instant
 *
 * Choose one —
 * - Exile target creature.
 * - Target opponent exiles an enchantment they control.
 *
 * Mode 2 is approximated by targeting an enchantment an opponent controls
 * and exiling it (caster chooses rather than opponent choosing).
 */
val EarlyWinter = card("Early Winter") {
    manaCost = "{4}{B}"
    typeLine = "Instant"
    oracleText = "Choose one —\n• Exile target creature.\n• Target opponent exiles an enchantment they control."

    spell {
        modal(chooseCount = 1) {
            mode("Exile target creature") {
                val t = target("creature", Targets.Creature)
                effect = Effects.Exile(t)
            }
            mode("Exile target enchantment an opponent controls") {
                val t = target("enchantment", TargetObject(
                    filter = TargetFilter.Enchantment.opponentControls()
                ))
                effect = Effects.Exile(t)
            }
        }
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "93"
        artist = "Andrew Mar"
        flavorText = "\"The climate changed and the world suffered.\""
        imageUri = "https://cards.scryfall.io/normal/front/5/0/5030e6ac-211d-4145-8c87-998a8351a467.jpg?1721426407"
    }
}
