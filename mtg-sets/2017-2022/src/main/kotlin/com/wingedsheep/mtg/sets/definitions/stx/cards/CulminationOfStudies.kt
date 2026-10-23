package com.wingedsheep.mtg.sets.definitions.stx.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Culmination of Studies — Strixhaven: School of Mages #173 (canonical printing)
 * {X}{U}{R} · Sorcery
 *
 * Exile the top X cards of your library. For each land card exiled this way, create a Treasure
 * token. For each blue card exiled this way, draw a card. For each red card exiled this way,
 * Culmination of Studies deals 1 damage to each opponent.
 *
 * One pipeline: every card is exiled first (ruling), then the exiled collection is split three
 * ways by [filter] and each count feeds its payoff. A blue-and-red card counts in both piles
 * (ruling). The red count is one damage event of that size to each opponent (ruling), dealt by the
 * spell itself.
 */
val CulminationOfStudies = card("Culmination of Studies") {
    manaCost = "{X}{U}{R}"
    colorIdentity = "UR"
    typeLine = "Sorcery"
    oracleText = "Exile the top X cards of your library. For each land card exiled this way, create " +
        "a Treasure token. For each blue card exiled this way, draw a card. For each red card " +
        "exiled this way, Culmination of Studies deals 1 damage to each opponent."

    spell {
        effect = Effects.Pipeline {
            val exiled = gather(CardSource.TopOfLibrary(DynamicAmounts.xValue()))
            exile(exiled)
            val lands = filter(exiled, GameObjectFilter.Land)
            val blueCards = filter(exiled, GameObjectFilter.Any.withColor(Color.BLUE))
            val redCards = filter(exiled, GameObjectFilter.Any.withColor(Color.RED))
            run(Effects.CreateTreasure(lands.count))
            run(Effects.DrawCards(blueCards.count))
            run(
                Effects.DealDamage(
                    redCards.count,
                    EffectTarget.PlayerRef(Player.EachOpponent),
                    damageSource = EffectTarget.Self
                )
            )
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "173"
        artist = "Bryan Sola"
        flavorText = "Will's quick thinking meshed with Rowan's raw power to bring down the Blood Avatar."
        imageUri = "https://cards.scryfall.io/normal/front/2/4/2483060e-9d3f-48ae-80ea-0119bf6b4d67.jpg?1783927319"
        ruling("2021-04-16", "You will exile all X cards from the top of your library before taking any of the listed actions.")
        ruling("2021-04-16", "If an exiled card is both blue and red, Culmination of Studies deals 1 damage to each opponent and you draw a card.")
        ruling("2021-04-16", "If multiple red cards are exiled, all of the damage is dealt as a single event.")
    }
}
