package com.wingedsheep.mtg.sets.definitions.`40k`.cards

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
 * Commissar Severina Raine
 * {1}{W}{B}
 * Legendary Creature — Human Soldier
 * 2/2
 * Leading from the Front — Whenever Commissar Severina Raine attacks, each opponent loses X life,
 * where X is the number of other attacking creatures.
 * Summary Execution — {2}, Sacrifice another creature: You gain 2 life and draw a card.
 *
 * X counts every other attacking creature, including ones controlled by other players
 * (2022-10-07 ruling), so the count is over `Player.Each`.
 */
val CommissarSeverinaRaine = card("Commissar Severina Raine") {
    manaCost = "{1}{W}{B}"
    colorIdentity = "WB"
    typeLine = "Legendary Creature — Human Soldier"
    power = 2
    toughness = 2
    oracleText = "Leading from the Front — Whenever Commissar Severina Raine attacks, each opponent loses " +
        "X life, where X is the number of other attacking creatures.\n" +
        "Summary Execution — {2}, Sacrifice another creature: You gain 2 life and draw a card."

    triggeredAbility {
        trigger = Triggers.self.attacks()
        effect = Effects.LoseLife(
            DynamicAmounts.battlefield(
                Player.Each, GameObjectFilter.Creature.attacking(), excludeSelf = true
            ).count(),
            EffectTarget.PlayerRef(Player.EachOpponent),
        )
    }

    activatedAbility {
        cost = Costs.Composite(
            Costs.Mana("{2}"),
            Costs.SacrificeAnother(GameObjectFilter.Creature),
        )
        effect = Effects.GainLife(2) then Effects.DrawCards(1)
        description = "{2}, Sacrifice another creature: You gain 2 life and draw a card."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "112"
        artist = "Jake Murray"
        imageUri = "https://cards.scryfall.io/normal/front/9/4/94ba8b76-8de7-46da-b641-2503903f0f8a.jpg?1783920859"
        ruling("2022-10-07", "In Two-Headed Giant and other formats where multiple players can attack at the same time, Commissar Severina Raine's triggered ability counts all attacking creatures, even those controlled by other players.")
    }
}
