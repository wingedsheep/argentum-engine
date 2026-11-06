package com.wingedsheep.mtg.sets.definitions.akh.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Tattered Mummy
 * {1}{B}
 * Creature — Zombie Jackal
 * 1/2
 * When this creature dies, each opponent loses 2 life.
 */
val TatteredMummy = card("Tattered Mummy") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Zombie Jackal"
    power = 1
    toughness = 2
    oracleText = "When this creature dies, each opponent loses 2 life."

    triggeredAbility {
        trigger = Triggers.self.dies()
        effect = Effects.LoseLife(2, EffectTarget.PlayerRef(Player.EachOpponent))
    }

    metadata {
        inBooster = false
        rarity = Rarity.COMMON
        collectorNumber = "278"
        artist = "Slawomir Maniak"
        flavorText = "The dead who wander beyond the safety of the city crave only to spread their curse."
        imageUri = "https://cards.scryfall.io/normal/front/7/f/7f9a5267-eb90-43bc-bf92-fcfb06821bae.jpg"
    }
}
