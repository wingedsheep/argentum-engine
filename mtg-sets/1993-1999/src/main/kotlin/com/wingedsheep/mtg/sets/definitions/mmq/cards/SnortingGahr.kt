package com.wingedsheep.mtg.sets.definitions.mmq.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Snorting Gahr
 * {2}{G}{G}
 * Creature — Rhino Beast
 * 3 / 3
 *
 * Whenever this creature becomes blocked, it gets +2/+2 until end of turn.
 */
val SnortingGahr = card("Snorting Gahr") {
    manaCost = "{2}{G}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Rhino Beast"
    oracleText = "Whenever this creature becomes blocked, it gets +2/+2 until end of turn."
    power = 3
    toughness = 3

    triggeredAbility {
        trigger = Triggers.BecomesBlocked
        effect = Effects.ModifyStats(2, 2, EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "272"
        artist = "Andrew Goldhawk"
        flavorText = "There's little advantage to surprising the gahr."
        imageUri = "https://cards.scryfall.io/normal/front/e/5/e568503e-a886-4c8b-9d46-8520c2cdda48.jpg"
    }
}
