package com.wingedsheep.mtg.sets.definitions.khans.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.TargetCreature

/**
 * Dragonscale Boon
 * {3}{G}
 * Instant
 * Put two +1/+1 counters on target creature and untap it.
 */
val DragonscaleBoon = card("Dragonscale Boon") {
    manaCost = "{3}{G}"
    typeLine = "Instant"
    oracleText = "Put two +1/+1 counters on target creature and untap it."

    spell {
        val t = target("target", TargetCreature())
        effect = Effects.AddCounters("+1/+1", 2, t)
            .then(Effects.Untap(t))
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "131"
        artist = "Mark Winters"
        flavorText = "\"When we were lost and weary, the ainok showed us how to survive. They have earned the right to call themselves Abzan, and to wear the Scale.\" —Anafenza, khan of the Abzan"
        imageUri = "https://cards.scryfall.io/normal/front/5/a/5aadb382-f912-4ccb-98bc-1abdef733126.jpg?1562787056"
    }
}
