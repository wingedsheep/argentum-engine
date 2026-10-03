package com.wingedsheep.mtg.sets.definitions.mh3.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Accursed Marauder
 * {1}{B}
 * Creature — Zombie Warrior
 * 2/1
 *
 * When this creature enters, each player sacrifices a nontoken creature of their choice.
 *
 * Canonical printing: Modern Horizons 3, the card's earliest real printing.
 *
 * Slum Reaper's edict narrowed to nontoken creatures: a `ForceSacrificeEffect` over [Player.Each].
 * The Marauder is itself a nontoken creature, so its controller may have to feed it in.
 */
val AccursedMarauder = card("Accursed Marauder") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Zombie Warrior"
    oracleText = "When this creature enters, each player sacrifices a nontoken creature of their choice."
    power = 2
    toughness = 1

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.Sacrifice(
            GameObjectFilter.Creature.nontoken(),
            1,
            EffectTarget.PlayerRef(Player.Each),
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "80"
        artist = "Paolo Parente"
        flavorText = "The Accursed are ever drawn to the Hekma, as though its presence provides a brief respite in their lives of eternal torment."
        imageUri = "https://cards.scryfall.io/normal/front/5/d/5da14d86-0780-4821-a799-96f64b377df4.jpg?1783911284"
    }
}
