package com.wingedsheep.mtg.sets.definitions.blb.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.GainLifeEffect
import com.wingedsheep.sdk.scripting.effects.LoseLifeEffect
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Valley Rotcaller
 * {1}{B}
 * Creature — Squirrel Warlock
 * 1/3
 *
 * Menace
 * Whenever this creature attacks, each opponent loses X life and you gain X life,
 * where X is the number of other Squirrels, Bats, Lizards, and Rats you control.
 */
val ValleyRotcaller = card("Valley Rotcaller") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Squirrel Warlock"
    power = 1
    toughness = 3
    oracleText = "Menace\nWhenever this creature attacks, each opponent loses X life and you gain X life, where X is the number of other Squirrels, Bats, Lizards, and Rats you control."

    keywords(Keyword.MENACE)

    val relevantCreatures = (
        GameObjectFilter.Creature.withSubtype("Squirrel") or
        GameObjectFilter.Creature.withSubtype("Bat") or
        GameObjectFilter.Creature.withSubtype("Lizard") or
        GameObjectFilter.Creature.withSubtype("Rat")
    ).youControl()

    val xAmount = DynamicAmount.AggregateBattlefield(
        player = Player.You,
        filter = relevantCreatures,
        excludeSelf = true
    )

    triggeredAbility {
        trigger = Triggers.Attacks
        effect = Effects.Composite(
            listOf(
                LoseLifeEffect(xAmount, EffectTarget.PlayerRef(Player.EachOpponent)),
                GainLifeEffect(xAmount, EffectTarget.Controller)
            )
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "119"
        artist = "Valera Lutfullina"
        flavorText = "We all share one life and one death.\n—Rotcaller saying"
        imageUri = "https://cards.scryfall.io/normal/front/4/d/4da80a9a-b1d5-4fc5-92f7-36946195d0c7.jpg?1721639479"
    }
}
