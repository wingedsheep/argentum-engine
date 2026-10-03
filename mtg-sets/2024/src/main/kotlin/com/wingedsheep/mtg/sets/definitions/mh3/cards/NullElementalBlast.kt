package com.wingedsheep.mtg.sets.definitions.mh3.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Null Elemental Blast
 * {C}
 * Instant
 * Choose one —
 * • Counter target multicolored spell.
 * • Destroy target multicolored permanent.
 *
 * Multicolored means two or more colors (CR 105.2b); the filter reads projected colors on the
 * battlefield, so a permanent painted multicolored by an effect is a legal target.
 */
val NullElementalBlast = card("Null Elemental Blast") {
    manaCost = "{C}"
    typeLine = "Instant"
    oracleText = "Choose one —\n• Counter target multicolored spell.\n• Destroy target multicolored permanent."
    spell {
        modal(chooseCount = 1) {
            mode("Counter target multicolored spell") {
                target(TargetFilter(GameObjectFilter.Multicolored, zone = Zone.STACK))
                effect = Effects.CounterSpell()
            }
            mode("Destroy target multicolored permanent") {
                val t = target(TargetFilter(GameObjectFilter.Permanent and GameObjectFilter.Multicolored))
                effect = Effects.Destroy(t)
            }
        }
    }
    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "12"
        artist = "Milivoj Ćeran"
        flavorText = "\"Despite my years, I am still but a student of the Multiverse. How arrogant to think " +
            "yourself its master.\"\n—Ugin, to Nicol Bolas"
        imageUri = "https://cards.scryfall.io/normal/front/8/e/8e259868-d29a-4c03-8ec3-49e914f849fb.jpg"
    }
}
