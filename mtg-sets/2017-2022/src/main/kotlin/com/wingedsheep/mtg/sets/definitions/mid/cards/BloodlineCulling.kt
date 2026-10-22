package com.wingedsheep.mtg.sets.definitions.mid.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Bloodline Culling
 * {1}{B}{B}
 * Instant
 * Choose one —
 * • Target creature gets -5/-5 until end of turn.
 * • Creature tokens get -2/-2 until end of turn.
 *
 * The second mode is untargeted and hits every creature token — yours included.
 */
val BloodlineCulling = card("Bloodline Culling") {
    manaCost = "{1}{B}{B}"
    colorIdentity = "B"
    typeLine = "Instant"
    oracleText = "Choose one —\n• Target creature gets -5/-5 until end of turn.\n" +
        "• Creature tokens get -2/-2 until end of turn."

    spell {
        modal(chooseCount = 1) {
            mode("Target creature gets -5/-5 until end of turn") {
                val t = target(TargetFilter.Creature)
                effect = Effects.ModifyStats(-5, -5, t)
            }
            mode("Creature tokens get -2/-2 until end of turn") {
                effect = Patterns.Group.modifyStatsForAll(
                    -2, -2, GroupFilter(GameObjectFilter.Creature.token())
                )
            }
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "89"
        artist = "Liiga Smilshkalne"
        flavorText = "\"Forget your human ancestors. You're part of my family now.\""
        imageUri = "https://cards.scryfall.io/normal/front/f/a/fac827f7-a587-4adf-8408-2d9ccd9c1343.jpg?1783925624"
    }
}
