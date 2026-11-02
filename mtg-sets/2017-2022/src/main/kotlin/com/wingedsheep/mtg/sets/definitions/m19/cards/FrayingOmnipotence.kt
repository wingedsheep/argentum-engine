package com.wingedsheep.mtg.sets.definitions.m19.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.divRoundedUp
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Fraying Omnipotence
 * {3}{B}{B}
 * Sorcery
 * Each player loses half their life, then discards half the cards in their hand, then sacrifices
 * half the creatures they control of their choice. Round up each time.
 *
 * Pox Plague's shape with creatures instead of permanents and rounding up: three sequenced
 * [Effects.ForEachPlayer] stages over [Player.ActivePlayerFirst] (APNAP order), each rebinding
 * [Player.You] / [EffectTarget.Controller] to the iterated player so the "half, rounded up" amount
 * reads that player's own life / hand / creatures, computed when its stage is performed.
 */
val FrayingOmnipotence = card("Fraying Omnipotence") {
    manaCost = "{3}{B}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "Each player loses half their life, then discards half the cards in their hand, " +
        "then sacrifices half the creatures they control of their choice. Round up each time."

    spell {
        effect = Effects.ForEachPlayer(
            Player.ActivePlayerFirst,
            Effects.LoseLife(
                amount = DynamicAmounts.lifeTotal(Player.You) divRoundedUp 2,
                target = EffectTarget.Controller,
            ),
        ) then Effects.ForEachPlayer(
            Player.ActivePlayerFirst,
            Effects.Discard(
                count = DynamicAmounts.zone(Player.You, Zone.HAND).count() divRoundedUp 2,
                target = EffectTarget.Controller,
            ),
        ) then Effects.ForEachPlayer(
            Player.ActivePlayerFirst,
            Effects.Sacrifice(
                filter = GameObjectFilter.Creature,
                count = DynamicAmounts.battlefield(
                    Player.You,
                    GameObjectFilter.Creature,
                ).count() divRoundedUp 2,
                target = EffectTarget.Controller,
            ),
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "97"
        artist = "Svetlin Velinov"
        flavorText = "The Great Mending that healed the Multiverse also unraveled the threads of " +
            "Nicol Bolas's power."
        imageUri = "https://cards.scryfall.io/normal/front/f/0/f0fa0ad8-854a-4b4e-9f87-e1dfaa11253c.jpg?1783934570"

        ruling(
            "2018-07-13",
            "The amounts that are lost are rounded up, not the amounts kept. For example, a player " +
                "with 5 life, five cards in hand, and five creatures would lose 3 life, discard " +
                "three cards, and sacrifice three creatures."
        )
        ruling(
            "2018-07-13",
            "Because the instructions are followed in order, abilities of the creatures a player " +
                "will sacrifice may modify or trigger on losing life, discarding cards, and/or " +
                "sacrificing creatures. Abilities that trigger this way will resolve after Fraying " +
                "Omnipotence is done resolving."
        )
        ruling(
            "2018-07-13",
            "When Fraying Omnipotence resolves, first each player loses the appropriate amount of " +
                "life at once. Next, the player whose turn it is chooses an appropriate number of " +
                "cards in hand without revealing them, then each other player in turn order does " +
                "the same, and then those cards are all discarded simultaneously. Then, the player " +
                "whose turn it is chooses an appropriate number of creatures they control. Each " +
                "other player in turn order does the same, knowing choices made by previous " +
                "players. Finally, all of the chosen creatures are sacrificed simultaneously."
        )
        ruling(
            "2018-07-13",
            "In a Two-Headed Giant game, Fraying Omnipotence causes each player to lose half of " +
                "their team's life, rounded up. This means that teams at an even life total will " +
                "have 0 life, and teams at an odd life total will have -1 life. The game then ends " +
                "in a draw."
        )
    }
}
