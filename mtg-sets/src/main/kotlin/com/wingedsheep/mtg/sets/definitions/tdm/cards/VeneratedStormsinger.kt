package com.wingedsheep.mtg.sets.definitions.tdm.cards

import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.GainLifeEffect
import com.wingedsheep.sdk.scripting.effects.LoseLifeEffect
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.dsl.Effects

/**
 * Venerated Stormsinger — Tarkir: Dragonstorm #97
 * {3}{B} · Creature — Orc Cleric · 3/3
 *
 * Mobilize 1 (Whenever this creature attacks, create a tapped and attacking 1/1 red Warrior
 * creature token. Sacrifice it at the beginning of the next end step.)
 * Whenever this creature or another creature you control dies, each opponent loses 1 life and
 * you gain 1 life.
 *
 * "This creature or another creature you control dies" is exactly "a creature you control
 * dies", so the trigger is [Triggers.YourCreatureDies] (ANY binding + Creature.youControl
 * filter), which also fires when the source itself dies via last-known control information.
 */
val VeneratedStormsinger = card("Venerated Stormsinger") {
    manaCost = "{3}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Orc Cleric"
    power = 3
    toughness = 3
    oracleText = "Mobilize 1 (Whenever this creature attacks, create a tapped and attacking 1/1 red Warrior creature token. Sacrifice it at the beginning of the next end step.)\n" +
        "Whenever this creature or another creature you control dies, each opponent loses 1 life and you gain 1 life."

    mobilize(1)

    triggeredAbility {
        trigger = Triggers.YourCreatureDies
        effect = Effects.Composite(
            listOf(
                LoseLifeEffect(1, EffectTarget.PlayerRef(Player.EachOpponent)),
                GainLifeEffect(1, EffectTarget.Controller)
            )
        )
        description = "Whenever this creature or another creature you control dies, each opponent loses 1 life and you gain 1 life."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "97"
        artist = "Elizabeth Peiró"
        imageUri = "https://cards.scryfall.io/normal/front/a/4/a4a4e985-facd-47e6-b680-3023c82c2957.jpg?1743204350"
    }
}
