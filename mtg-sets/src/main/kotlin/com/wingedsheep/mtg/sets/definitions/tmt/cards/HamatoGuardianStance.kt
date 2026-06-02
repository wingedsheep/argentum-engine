package com.wingedsheep.mtg.sets.definitions.tmt.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.EffectPatterns
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration

/**
 * Hamato Guardian Stance
 * {W}
 * Instant
 *
 * Target creature gets +1/+3 and gains flying until end of turn. Scry 1.
 */
val HamatoGuardianStance = card("Hamato Guardian Stance") {
    manaCost = "{W}"
    colorIdentity = "W"
    typeLine = "Instant"
    oracleText = "Target creature gets +1/+3 and gains flying until end of turn. Scry 1. (Look at the top card of your library. You may put that card on the bottom.)"

    spell {
        val creature = target("target creature", Targets.Creature)
        effect = Effects.ModifyStats(1, 3, creature)
            .then(Effects.GrantKeyword(Keyword.FLYING, creature, Duration.EndOfTurn))
            .then(EffectPatterns.scry(1))
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "8"
        artist = "Jason Rainville"
        flavorText = "\"Everything you know, I have shown you, but I have not shown you everything I know.\""
        imageUri = "https://cards.scryfall.io/normal/front/7/3/735b540b-b472-46fa-a232-d444cabf6c4c.jpg?1771502490"
    }
}
