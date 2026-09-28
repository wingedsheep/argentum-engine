package com.wingedsheep.mtg.sets.definitions.mom.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Shatter the Source
 * {5}{R}
 * Instant
 * Convoke
 * Choose one —
 * • Shatter the Source deals 6 damage to target creature, planeswalker, or battle.
 * • Destroy target artifact.
 */
val ShatterTheSource = card("Shatter the Source") {
    manaCost = "{5}{R}"
    colorIdentity = "R"
    typeLine = "Instant"
    oracleText = "Convoke (Your creatures can help cast this spell. Each creature you tap while " +
        "casting this spell pays for {1} or one mana of that creature's color.)\n" +
        "Choose one —\n" +
        "• Shatter the Source deals 6 damage to target creature, planeswalker, or battle.\n" +
        "• Destroy target artifact."

    keywords(Keyword.CONVOKE)

    spell {
        modal {
            mode("Shatter the Source deals 6 damage to target creature, planeswalker, or battle") {
                val t = target(
                    TargetFilter(GameObjectFilter.Creature or GameObjectFilter.Planeswalker or GameObjectFilter.Battle)
                )
                effect = Effects.DealDamage(6, t)
            }
            mode("Destroy target artifact") {
                val t = target(TargetFilter.Artifact)
                effect = Effects.Destroy(t)
            }
        }
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "164"
        artist = "Artur Nakhodkin"
        imageUri = "https://cards.scryfall.io/normal/front/0/3/032720a1-410b-4638-9294-35fd8e27375f.jpg?1783916981"
    }
}
