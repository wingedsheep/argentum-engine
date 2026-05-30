package com.wingedsheep.mtg.sets.definitions.tdm.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Cori Mountain Stalwart
 * {1}{R}{W}
 * Creature — Human Monk
 * 3/3
 *
 * Flurry — Whenever you cast your second spell each turn, this creature deals 2 damage
 * to each opponent and you gain 2 life.
 */
val CoriMountainStalwart = card("Cori Mountain Stalwart") {
    manaCost = "{1}{R}{W}"
    colorIdentity = "RW"
    typeLine = "Creature — Human Monk"
    power = 3
    toughness = 3
    oracleText = "Flurry — Whenever you cast your second spell each turn, this creature deals 2 damage to each opponent and you gain 2 life."

    flurry {
        effect = Effects.DealDamage(2, EffectTarget.PlayerRef(Player.EachOpponent), damageSource = EffectTarget.Self)
            .then(Effects.GainLife(2))
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "175"
        artist = "Josiah \"Jo\" Cameron"
        flavorText = "Zhao had resolved to hold the bridge, defending the village behind him. But the villagers decided Zhao would not stand alone."
        imageUri = "https://cards.scryfall.io/normal/front/b/6/b6cbf54e-f30e-4e7b-b17c-217fa424971c.jpg?1743204677"
    }
}
