package com.wingedsheep.mtg.sets.definitions.onslaught.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.EffectPatterns
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.BecomeCreatureTypeEffect
import com.wingedsheep.sdk.scripting.targets.TargetCreature

/**
 * Trickery Charm
 * {U}
 * Instant
 * Choose one —
 * • Target creature gains flying until end of turn.
 * • Target creature becomes the creature type of your choice until end of turn.
 * • Look at the top four cards of your library, then put them back in any order.
 */
val TrickeryCharm = card("Trickery Charm") {
    manaCost = "{U}"
    typeLine = "Instant"
    oracleText = "Choose one —\n• Target creature gains flying until end of turn.\n• Target creature becomes the creature type of your choice until end of turn.\n• Look at the top four cards of your library, then put them back in any order."

    spell {
        modal(chooseCount = 1) {
            mode("Target creature gains flying until end of turn") {
                val t = target("target", TargetCreature())
                effect = Effects.GrantKeyword(Keyword.FLYING, t)
            }
            mode("Target creature becomes the creature type of your choice until end of turn") {
                val t = target("target", TargetCreature())
                effect = BecomeCreatureTypeEffect(target = t)
            }
            mode("Look at the top four cards of your library, then put them back in any order") {
                effect = EffectPatterns.lookAtTopAndReorder(4)
            }
        }
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "119"
        artist = "David Martin"
        imageUri = "https://cards.scryfall.io/normal/front/3/2/32a2ee45-7f1d-40a8-82b4-ab3b705417ea.jpg?1562906840"
    }
}
