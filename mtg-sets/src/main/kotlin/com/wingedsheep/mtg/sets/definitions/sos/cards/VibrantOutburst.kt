package com.wingedsheep.mtg.sets.definitions.sos.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.DealDamageEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.AnyTarget
import com.wingedsheep.sdk.scripting.targets.TargetCreature

/**
 * Vibrant Outburst
 * {U}{R}
 * Instant
 * Vibrant Outburst deals 3 damage to any target. Tap up to one target creature.
 */
val VibrantOutburst = card("Vibrant Outburst") {
    manaCost = "{U}{R}"
    colorIdentity = "UR"
    typeLine = "Instant"
    oracleText = "Vibrant Outburst deals 3 damage to any target. Tap up to one target creature."
    spell {
        val t1 = target("t1", AnyTarget())
        val t2 = target("t2", TargetCreature(optional = true, filter = TargetFilter.Creature))
        effect = Effects.Composite(
            DealDamageEffect(3, t1),
            Effects.Tap(t2)
        )
    }
    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "240"
        artist = "Eelis Kyttanen"
        flavorText = "\"I'll show you a 'colorful insult'!\""
        imageUri = "https://cards.scryfall.io/normal/front/f/9/f9ba68ef-6efc-4249-8b74-e33f47173902.jpg"
    }
}
