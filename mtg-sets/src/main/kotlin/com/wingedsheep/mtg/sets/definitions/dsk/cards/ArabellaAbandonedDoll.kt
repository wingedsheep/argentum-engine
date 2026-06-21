package com.wingedsheep.mtg.sets.definitions.dsk.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Arabella, Abandoned Doll
 * {R}{W}
 * Legendary Artifact Creature — Toy
 * 1/3
 * Whenever Arabella attacks, it deals X damage to each opponent and you gain X life,
 * where X is the number of creatures you control with power 2 or less.
 */
val ArabellaAbandonedDoll = card("Arabella, Abandoned Doll") {
    manaCost = "{R}{W}"
    colorIdentity = "WR"
    typeLine = "Legendary Artifact Creature — Toy"
    power = 1
    toughness = 3
    oracleText = "Whenever Arabella attacks, it deals X damage to each opponent and you gain X " +
        "life, where X is the number of creatures you control with power 2 or less."

    // X = number of creatures you control with power 2 or less. The same X feeds both the
    // damage (sourced from Arabella) and the life gain, so it's a single resolution-time count
    // reused by both effects in the composite.
    val x = DynamicAmounts.battlefield(
        Player.You,
        GameObjectFilter.Creature.powerAtMost(2),
    ).count()

    triggeredAbility {
        trigger = Triggers.Attacks
        effect = Effects.Composite(
            Effects.DealDamage(x, EffectTarget.PlayerRef(Player.EachOpponent)),
            Effects.GainLife(x),
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "208"
        artist = "J.P. Targete"
        flavorText = "Every night, Cori threw the unsettling doll away. Every morning, it was " +
            "back on the shelf, its eyes following her every move."
        imageUri = "https://cards.scryfall.io/normal/front/f/6/f683d5a1-b8bf-446f-9fe3-88a4398bf3cf.jpg?1726286645"
    }
}
