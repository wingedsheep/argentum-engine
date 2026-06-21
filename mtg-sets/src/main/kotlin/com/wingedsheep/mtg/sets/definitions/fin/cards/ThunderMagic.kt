package com.wingedsheep.mtg.sets.definitions.fin.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Thunder Magic
 * {R}
 * Instant
 *
 * Tiered (Choose one additional cost.)
 * • Thunder — {0} — Thunder Magic deals 2 damage to target creature.
 * • Thundara — {3} — Thunder Magic deals 4 damage to target creature.
 * • Thundaga — {5}{R} — Thunder Magic deals 8 damage to target creature.
 *
 * Tiered (CR 702.183): a choose-one modal spell where the chosen tier's additional mana cost is
 * paid at cast. Each tier targets a creature and scales the damage.
 */
val ThunderMagic = card("Thunder Magic") {
    manaCost = "{R}"
    colorIdentity = "R"
    typeLine = "Instant"
    oracleText = "Tiered (Choose one additional cost.)\n" +
        "• Thunder — {0} — Thunder Magic deals 2 damage to target creature.\n" +
        "• Thundara — {3} — Thunder Magic deals 4 damage to target creature.\n" +
        "• Thundaga — {5}{R} — Thunder Magic deals 8 damage to target creature."

    spell {
        tiered {
            tier("Thunder", "{0}", "Thunder Magic deals 2 damage to target creature.") {
                effect = Effects.DealDamage(2, EffectTarget.ContextTarget(0))
                target = Targets.Creature
            }
            tier("Thundara", "{3}", "Thunder Magic deals 4 damage to target creature.") {
                effect = Effects.DealDamage(4, EffectTarget.ContextTarget(0))
                target = Targets.Creature
            }
            tier("Thundaga", "{5}{R}", "Thunder Magic deals 8 damage to target creature.") {
                effect = Effects.DealDamage(8, EffectTarget.ContextTarget(0))
                target = Targets.Creature
            }
        }
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "165"
        artist = "Josephine Chang"
        imageUri = "https://cards.scryfall.io/normal/front/9/f/9f2b202c-91da-40ec-8324-6b6be7cb3bc8.jpg?1748706380"
    }
}
