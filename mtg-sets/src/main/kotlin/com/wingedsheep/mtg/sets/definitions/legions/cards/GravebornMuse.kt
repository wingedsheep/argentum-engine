package com.wingedsheep.mtg.sets.definitions.legions.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Graveborn Muse
 * {2}{B}{B}
 * Creature — Zombie Spirit
 * 3/3
 * At the beginning of your upkeep, you draw X cards and you lose X life, where X is the number of Zombies you control.
 */
val GravebornMuse = card("Graveborn Muse") {
    manaCost = "{2}{B}{B}"
    typeLine = "Creature — Zombie Spirit"
    power = 3
    toughness = 3
    oracleText = "At the beginning of your upkeep, you draw X cards and you lose X life, where X is the number of Zombies you control."

    triggeredAbility {
        trigger = Triggers.YourUpkeep
        effect = Effects.DrawCards(
            count = DynamicAmount.AggregateBattlefield(Player.You, GameObjectFilter.Creature.withSubtype("Zombie")),
            target = EffectTarget.Controller
        ).then(
            Effects.LoseLife(
                amount = DynamicAmount.AggregateBattlefield(Player.You, GameObjectFilter.Creature.withSubtype("Zombie")),
                target = EffectTarget.Controller
            )
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "73"
        artist = "Kev Walker"
        flavorText = "\"Her voice is damnation, unyielding and certain.\" —Phage the Untouchable"
        imageUri = "https://cards.scryfall.io/normal/front/a/a/aa432e4e-ff23-4ad2-8d0a-403efee86f11.jpg?1562929311"
    }
}
