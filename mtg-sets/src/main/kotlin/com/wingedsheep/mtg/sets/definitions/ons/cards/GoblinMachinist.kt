package com.wingedsheep.mtg.sets.definitions.ons.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardOrder
import com.wingedsheep.sdk.scripting.effects.GatherUntilMatchEffect
import com.wingedsheep.sdk.scripting.effects.ModifyStatsEffect
import com.wingedsheep.sdk.scripting.effects.MoveCollectionEffect
import com.wingedsheep.sdk.scripting.effects.RevealCollectionEffect
import com.wingedsheep.sdk.scripting.effects.ZonePlacement
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import com.wingedsheep.sdk.dsl.Effects

/**
 * Goblin Machinist
 * {4}{R}
 * Creature — Goblin
 * 0/5
 * {2}{R}: Reveal cards from the top of your library until you reveal a nonland card.
 * Goblin Machinist gets +X/+0 until end of turn, where X is that card's mana value.
 * Put the revealed cards on the bottom of your library in any order.
 */
val GoblinMachinist = card("Goblin Machinist") {
    manaCost = "{4}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Goblin"
    power = 0
    toughness = 5
    oracleText = "{2}{R}: Reveal cards from the top of your library until you reveal a nonland card. Goblin Machinist gets +X/+0 until end of turn, where X is that card's mana value. Put the revealed cards on the bottom of your library in any order."

    activatedAbility {
        cost = Costs.Mana("{2}{R}")
        effect = Effects.Composite(
            listOf(
                GatherUntilMatchEffect(
                    filter = GameObjectFilter.Nonland,
                    storeMatch = "nonland",
                    storeRevealed = "allRevealed"
                ),
                RevealCollectionEffect(from = "allRevealed"),
                ModifyStatsEffect(
                    powerModifier = DynamicAmount.StoredCardManaValue("nonland"),
                    toughnessModifier = DynamicAmount.Fixed(0),
                    target = EffectTarget.Self
                ),
                MoveCollectionEffect(
                    from = "allRevealed",
                    destination = CardDestination.ToZone(
                        Zone.LIBRARY,
                        placement = ZonePlacement.Bottom
                    ),
                    order = CardOrder.ControllerChooses
                )
            )
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "204"
        artist = "Doug Chaffee"
        imageUri = "https://cards.scryfall.io/normal/front/5/8/5874e312-1010-43f2-b330-82bc9fcc9f53.jpg?1562915797"
    }
}
