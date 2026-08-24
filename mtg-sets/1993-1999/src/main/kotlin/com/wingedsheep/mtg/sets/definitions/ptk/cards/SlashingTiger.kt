package com.wingedsheep.mtg.sets.definitions.ptk.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Slashing Tiger
 * {2}{G}{G}
 * Creature — Cat
 * 3 / 3
 *
 * Whenever this creature becomes blocked, it gets +2/+2 until end of turn.
 */
val SlashingTiger = card("Slashing Tiger") {
    manaCost = "{2}{G}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Cat"
    power = 3
    toughness = 3
    oracleText = "Whenever this creature becomes blocked, it gets +2/+2 until end of turn."

    triggeredAbility {
        trigger = Triggers.BecomesBlocked
        effect = Effects.ModifyStats(2, 2, EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "145"
        artist = "Yang Jun Kwon"
        flavorText = "\"Unless you enter the tiger's lair, you cannot get hold of the tiger's cubs.\"\n—Sun Tzu, *Art of War* (trans. Giles)"
        imageUri = "https://cards.scryfall.io/normal/front/4/f/4fbedd66-457a-4e1c-a9f3-fa37dec81c7a.jpg"
    }
}
