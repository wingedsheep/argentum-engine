package com.wingedsheep.mtg.sets.definitions.stx.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Dina, Soul Steeper
 * {B}{G}
 * Legendary Creature — Dryad Druid
 * 1/3
 * Whenever you gain life, each opponent loses 1 life.
 * {1}, Sacrifice another creature: Dina gets +X/+0 until end of turn, where X is the
 * sacrificed creature's power.
 *
 * X reads the sacrificed creature's last-known power (`DynamicAmounts.sacrificedPower()`,
 * snapshotted as the cost is paid), matching the ruling.
 */
val DinaSoulSteeper = card("Dina, Soul Steeper") {
    manaCost = "{B}{G}"
    colorIdentity = "BG"
    typeLine = "Legendary Creature — Dryad Druid"
    oracleText = "Whenever you gain life, each opponent loses 1 life.\n" +
        "{1}, Sacrifice another creature: Dina gets +X/+0 until end of turn, where X is the sacrificed creature's power."
    power = 1
    toughness = 3

    triggeredAbility {
        trigger = Triggers.you.gainsLife()
        effect = Effects.LoseLife(1, EffectTarget.PlayerRef(Player.EachOpponent))
    }

    activatedAbility {
        cost = Costs.Composite(
            Costs.Mana("{1}"),
            Costs.SacrificeAnother(GameObjectFilter.Creature),
        )
        effect = Effects.ModifyStats(
            DynamicAmounts.sacrificedPower(),
            DynamicAmounts.fixed(0),
            EffectTarget.Self
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "178"
        artist = "Chris Rahn"
        imageUri = "https://cards.scryfall.io/normal/front/9/c/9cd2b567-0cf7-4441-b3ce-e31141dd91c8.jpg?1783927318"
        ruling("2021-04-16", "X is the power the creature had when it was on the battlefield, not the power it has in the graveyard.")
        ruling("2021-04-16", "Dina's first ability triggers only once for each life gain event, no matter how much life was gained.")
    }
}
