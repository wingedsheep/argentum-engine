package com.wingedsheep.mtg.sets.definitions.fdn.cards

import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.SelfAlternativeCost
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Blasphemous Edict
 * {3}{B}{B}
 * Sorcery
 *
 * You may pay {B} rather than pay this spell's mana cost if there are thirteen or more creatures
 * on the battlefield.
 * Each player sacrifices thirteen creatures of their choice.
 *
 * The alternative cost is gated on a *global* creature count — every creature on the battlefield,
 * not just yours — hence `Player.Each` on the [DynamicAmount.AggregateBattlefield]. The gate is
 * checked when actions are enumerated and again when the cast is authorized, so it can't be paid
 * once the thirteenth creature has left in response.
 *
 * "Each player sacrifices thirteen creatures **of their choice**" fans out from
 * `EffectTarget.PlayerRef(Player.Each)` in APNAP order; players choosing later see the earlier
 * choices, per the card's ruling. A player controlling fewer than thirteen creatures sacrifices
 * all of them with no prompt (also per ruling) — the sacrifice executor already treats the count
 * as an upper bound rather than a requirement.
 */
val BlasphemousEdict = card("Blasphemous Edict") {
    manaCost = "{3}{B}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "You may pay {B} rather than pay this spell's mana cost if there are thirteen or " +
        "more creatures on the battlefield.\nEach player sacrifices thirteen creatures of their choice."

    selfAlternativeCost = SelfAlternativeCost(
        manaCost = ManaCost.parse("{B}"),
        condition = Conditions.CompareAmounts(
            DynamicAmount.AggregateBattlefield(Player.Each, GameObjectFilter.Creature),
            ComparisonOperator.GTE,
            DynamicAmount.Fixed(13)
        )
    )

    spell {
        effect = Effects.Sacrifice(
            GameObjectFilter.Creature,
            count = 13,
            target = EffectTarget.PlayerRef(Player.Each)
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "57"
        artist = "Andrew Mar"
        flavorText = "\"A demonic pact is all fun and games until the bill comes due.\"\n—Liliana Vess"
        imageUri = "https://cards.scryfall.io/normal/front/1/1/11040ecd-3153-4029-b42b-1441bc51ec34.jpg?1783909112"
        ruling("2024-11-08", "If a player controls fewer than thirteen creatures when Blasphemous Edict resolves, they will sacrifice all of their creatures.")
        ruling("2024-11-08", "Starting with the player whose turn it is, each player in turn order chooses which creatures they will sacrifice, then all the creatures chosen by all players are sacrificed at the same time. Players get to know the choices made by players who chose before them.")
    }
}
