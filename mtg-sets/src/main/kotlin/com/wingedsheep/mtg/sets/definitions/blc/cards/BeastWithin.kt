package com.wingedsheep.mtg.sets.definitions.blc.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CompositeEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Beast Within
 * {2}{G}
 * Instant
 *
 * Destroy target permanent. Its controller creates a 3/3 green Beast creature token.
 */
val BeastWithin = card("Beast Within") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Instant"
    oracleText = "Destroy target permanent. Its controller creates a 3/3 green Beast creature token."

    spell {
        val permanent = target("permanent", Targets.Permanent)
        effect = CompositeEffect(
            listOf(
                Effects.Destroy(permanent),
                Effects.CreateToken(
                    power = 3,
                    toughness = 3,
                    colors = setOf(Color.GREEN),
                    creatureTypes = setOf("Beast"),
                    controller = EffectTarget.TargetController,
                    imageUri = "https://cards.scryfall.io/normal/front/d/9/d93d0098-2147-4e84-af15-91dec8b98d21.jpg?1721427669"
                )
            )
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "206"
        artist = "Efflam Mercier"
        imageUri = "https://cards.scryfall.io/normal/front/6/0/601c59cf-f3df-4003-9ae9-613a1d4a620b.jpg?1721429205"
    }
}
