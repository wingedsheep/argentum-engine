package com.wingedsheep.mtg.sets.definitions.xln.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Entrancing Melody
 * {X}{U}{U}
 * Sorcery
 * Gain control of target creature with mana value X.
 */
val EntrancingMelody = card("Entrancing Melody") {
    manaCost = "{X}{U}{U}"
    colorIdentity = "U"
    typeLine = "Sorcery"
    oracleText = "Gain control of target creature with mana value X."

    spell {
        val t = target(TargetFilter.Creature.manaValueEqualsX())
        effect = Effects.GainControl(t, Duration.Permanent)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "55"
        artist = "Winona Nelson"
        flavorText = "The stronger the will, the more bewitching the song."
        imageUri = "https://cards.scryfall.io/normal/front/1/5/15732049-7e56-432f-881f-215e45b7a70e.jpg?1783935782"
        ruling("2017-09-29", "The control-change effect of Entrancing Melody lasts indefinitely. It doesn't wear off during the cleanup step.")
        ruling("2017-09-29", "If a permanent has {X} in its mana cost, X is considered to be 0.")
    }
}
