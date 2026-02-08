package com.wingedsheep.mtg.sets.definitions.onslaught.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.BecomeCreatureTypeEffect
import com.wingedsheep.sdk.scripting.EffectTarget
import com.wingedsheep.sdk.scripting.LookAtTopAndReorderEffect
import com.wingedsheep.sdk.targeting.TargetCreature

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

    spell {
        modal(chooseCount = 1) {
            mode("Target creature gains flying until end of turn") {
                target = TargetCreature()
                effect = Effects.GrantKeyword(Keyword.FLYING, EffectTarget.ContextTarget(0))
            }
            mode("Target creature becomes the creature type of your choice until end of turn") {
                target = TargetCreature()
                effect = BecomeCreatureTypeEffect(target = EffectTarget.ContextTarget(0))
            }
            mode("Look at the top four cards of your library, then put them back in any order") {
                effect = LookAtTopAndReorderEffect(4)
            }
        }
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "119"
        artist = "David Martin"
    }
}
