package com.wingedsheep.mtg.sets.definitions.fin.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Ice Magic
 * {1}{U}
 * Instant
 *
 * Tiered (Choose one additional cost.)
 * • Blizzard — {0} — Return target creature to its owner's hand.
 * • Blizzara — {2} — Target creature's owner puts it on their choice of the top or bottom of their library.
 * • Blizzaga — {5}{U} — Target creature's owner shuffles it into their library.
 *
 * Tiered (CR 702.183): a choose-one modal spell where the chosen tier's additional mana cost is
 * paid at cast. Each tier removes target creature with an escalating destination.
 */
val IceMagic = card("Ice Magic") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Tiered (Choose one additional cost.)\n" +
        "• Blizzard — {0} — Return target creature to its owner's hand.\n" +
        "• Blizzara — {2} — Target creature's owner puts it on their choice of the top or bottom of their library.\n" +
        "• Blizzaga — {5}{U} — Target creature's owner shuffles it into their library."

    spell {
        tiered {
            tier("Blizzard", "{0}", "Return target creature to its owner's hand.") {
                effect = Effects.ReturnToHand(EffectTarget.ContextTarget(0))
                target = Targets.Creature
            }
            tier(
                "Blizzara", "{2}",
                "Target creature's owner puts it on their choice of the top or bottom of their library."
            ) {
                effect = Effects.PutOnTopOrBottomOfLibrary(EffectTarget.ContextTarget(0))
                target = Targets.Creature
            }
            tier("Blizzaga", "{5}{U}", "Target creature's owner shuffles it into their library.") {
                effect = Effects.ShuffleIntoLibrary(EffectTarget.ContextTarget(0))
                target = Targets.Creature
            }
        }
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "56"
        artist = "Masateru Ikeda"
        imageUri = "https://cards.scryfall.io/normal/front/9/d/9dabd626-7ec3-4913-babb-d5d3fd5e32d5.jpg?1748705962"
    }
}
