package com.wingedsheep.mtg.sets.definitions.drk.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * The Fallen
 * {1}{B}{B}{B}
 * Creature — Zombie
 * 2/3
 * At the beginning of your upkeep, this creature deals 1 damage to each opponent and planeswalker
 * it has dealt damage to this game.
 *
 * A grudge with a memory. The card needs the engine to remember, per source, everything it has
 * ever damaged — not per turn, which is all the existing damage markers track — so this adds
 * `DealtDamageToThisGameComponent`, written at the two choke points every damage instance already
 * passes through (`DamageUtils.trackDamageReceivedByPlayer` for players, the loyalty-removal sites
 * for planeswalkers) and never cleared at cleanup.
 *
 * It *is* stripped on a zone change with the rest of the damage memory, which is the printed
 * behaviour rather than an accident: a Fallen that dies and comes back is a new object (CR 400.7)
 * that has dealt damage to nobody, so its grudge starts over.
 *
 * The upkeep clause then reads that set back through the [EffectTarget.EachDamagedBySourceThisGame]
 * multi-entity target — one target reference feeding the ordinary `DealDamage`, rather than a
 * bespoke effect. It filters to recipients still in the game and, per the printed "each opponent",
 * to opponents of the Fallen's controller.
 */
val TheFallen = card("The Fallen") {
    manaCost = "{1}{B}{B}{B}"
    typeLine = "Creature — Zombie"
    power = 2
    toughness = 3
    oracleText = "At the beginning of your upkeep, this creature deals 1 damage to each opponent " +
        "and planeswalker it has dealt damage to this game."

    triggeredAbility {
        trigger = Triggers.YourUpkeep
        effect = Effects.DealDamage(1, EffectTarget.EachDamagedBySourceThisGame)
        description = "At the beginning of your upkeep, this creature deals 1 damage to each " +
            "opponent and planeswalker it has dealt damage to this game."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "53"
        artist = "Jesper Myrfors"
        flavorText = "Magic often masters those who cannot master it."
        imageUri = "https://cards.scryfall.io/normal/front/f/4/f4a176e1-b22b-4f36-ba7b-c506cb4e1bed.jpg?1783947938"
    }
}
