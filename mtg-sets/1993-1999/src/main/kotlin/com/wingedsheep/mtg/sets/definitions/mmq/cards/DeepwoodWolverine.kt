package com.wingedsheep.mtg.sets.definitions.mmq.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Deepwood Wolverine
 * {G}
 * Creature — Wolverine
 * 1 / 1
 *
 * Whenever this creature becomes blocked, it gets +2/+0 until end of turn.
 */
val DeepwoodWolverine = card("Deepwood Wolverine") {
    manaCost = "{G}"
    colorIdentity = "G"
    typeLine = "Creature — Wolverine"
    oracleText = "Whenever this creature becomes blocked, it gets +2/+0 until end of turn."
    power = 1
    toughness = 1

    triggeredAbility {
        trigger = Triggers.BecomesBlocked
        effect = Effects.ModifyStats(2, 0, EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "242"
        artist = "Ray Lago"
        flavorText = "The jhovalls are depleting its food sources, the Mercadians are eroding its home, and you're wondering why it's angry?"
        imageUri = "https://cards.scryfall.io/normal/front/d/b/db9a9a76-741a-4ba3-bd4b-0eb87d678253.jpg"
    }
}
