package com.wingedsheep.mtg.sets.definitions.rna.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetPlayer

/**
 * Priest of Forgotten Gods — Ravnica Allegiance #83
 * {1}{B} · Creature — Human Cleric · 1/2
 * {T}, Sacrifice two other creatures: Any number of target players each lose 2 life and sacrifice
 * a creature of their choice. You add {B}{B} and draw a card. (Activate only as an instant.)
 *
 * "Any number of target players" is an unlimited, possibly empty player target
 * ([TargetPlayer] with `unlimited = true`, Officious Interrogation); [Effects.ForEachTarget] runs
 * the life loss and the forced sacrifice once per still-legal target, each iteration seeing its
 * player as `ContextTarget(0)` (Reward the Faithful). The mana and the draw sit outside the loop, so
 * they happen even with zero targets (ruling). Because the ability can target, it is not a mana
 * ability — it uses the stack, and the reminder text is all "activate only as an instant" means.
 */
val PriestOfForgottenGods = card("Priest of Forgotten Gods") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Human Cleric"
    power = 1
    toughness = 2
    oracleText = "{T}, Sacrifice two other creatures: Any number of target players each lose 2 life " +
        "and sacrifice a creature of their choice. You add {B}{B} and draw a card. " +
        "(Activate only as an instant.)"

    activatedAbility {
        cost = Costs.Composite(
            Costs.Tap,
            Costs.SacrificeMultiple(2, GameObjectFilter.Creature.notSourceItself()),
        )
        target(TargetPlayer(unlimited = true))
        effect = Effects.ForEachTarget(
            Effects.LoseLife(2, EffectTarget.ContextTarget(0)),
            Effects.Sacrifice(GameObjectFilter.Creature, 1, EffectTarget.ContextTarget(0)),
        ) then Effects.AddMana(Color.BLACK, 2) then Effects.DrawCards(1)
        description = "{T}, Sacrifice two other creatures: Any number of target players each lose 2 life " +
            "and sacrifice a creature of their choice. You add {B}{B} and draw a card."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "83"
        artist = "Zack Stella"
        flavorText = "The Orzhov are not the only religious tradition on Ravnica, nor the oldest."
        imageUri = "https://cards.scryfall.io/normal/front/e/3/e3378fe8-3355-48aa-90d4-9cb739200160.jpg?1783933689"
        ruling("2019-01-25", "You may target players who can't sacrifice a creature. Those players still lose 2 life.")
        ruling(
            "2019-01-25",
            "Because it may have targets, the ability of Priest of Forgotten Gods isn't a mana ability. " +
                "It uses the stack and can be responded to, even if no targets were chosen."
        )
        ruling(
            "2019-01-25",
            "You can activate the ability of Priest of Forgotten Gods without choosing any targets if you " +
                "wish. You'll still add {B}{B} and draw a card."
        )
    }
}
