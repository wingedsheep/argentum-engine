package com.wingedsheep.mtg.sets.definitions.tdm.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.conditions.Compare
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardOrder
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.ConditionalEffect
import com.wingedsheep.sdk.scripting.effects.ZonePlacement
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Stillness in Motion — Tarkir: Dragonstorm #59
 * {1}{U} · Enchantment
 *
 * At the beginning of your upkeep, mill three cards. Then if your library has no cards in
 * it, exile this enchantment and put five cards from your graveyard on top of your library
 * in any order.
 *
 * Mill three resolves first; the intervening payoff is gated on the post-mill library being
 * empty (`Compare(Count(LIBRARY) == 0)`). When empty: exile this enchantment, then choose
 * five cards from your graveyard (or all of them, if fewer) and put them on top of your
 * library in a chosen order via the Gather → Select → Move pipeline.
 */
val StillnessInMotion = card("Stillness in Motion") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Enchantment"
    oracleText = "At the beginning of your upkeep, mill three cards. Then if your library has no " +
        "cards in it, exile this enchantment and put five cards from your graveyard on top of your " +
        "library in any order."

    triggeredAbility {
        trigger = Triggers.YourUpkeep
        effect = Patterns.Library.mill(3).then(
            ConditionalEffect(
                condition = Compare(
                    DynamicAmount.Count(Player.You, Zone.LIBRARY),
                    ComparisonOperator.EQ,
                    DynamicAmount.Fixed(0)
                ),
                effect = Effects.Pipeline {
                    run(Effects.Exile(EffectTarget.Self))
                    val graveyardCards = gather(
                        CardSource.FromZone(Zone.GRAVEYARD, Player.You),
                        name = "graveyardCards"
                    )
                    val toTop = chooseExactly(
                        5, from = graveyardCards,
                        name = "toTop"
                    )
                    move(
                        toTop,
                        destination = CardDestination.ToZone(Zone.LIBRARY, placement = ZonePlacement.Top),
                        order = CardOrder.ControllerChooses
                    )
                }
            )
        )
        description = "At the beginning of your upkeep, mill three cards. Then if your library has " +
            "no cards in it, exile this enchantment and put five cards from your graveyard on top of " +
            "your library in any order."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "59"
        artist = "Kai Carpenter"
        imageUri = "https://cards.scryfall.io/normal/front/a/6/a6289251-17e4-4987-96b9-2fb1a8f90e2a.jpg?1743697642"
    }
}
