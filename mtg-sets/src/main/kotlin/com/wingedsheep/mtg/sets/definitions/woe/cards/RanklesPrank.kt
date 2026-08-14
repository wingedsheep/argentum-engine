package com.wingedsheep.mtg.sets.definitions.woe.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Rankle's Prank
 * {2}{B}{B}
 * Sorcery
 *
 * Choose one or more —
 * • Each player discards two cards.
 * • Each player loses 4 life.
 * • Each player sacrifices two creatures of their choice.
 *
 * A choose-one-or-more modal spell — `modal(chooseCount = 3, minChooseCount = 1)`, the sorcery
 * twin of Rankle, Master of Pranks' combat trigger. Every mode is symmetric over [Player.Each]
 * and none of them targets, so the spell has no target requirements and always resolves; a mode
 * may be chosen even when it will do nothing to anyone (2023-09-01 ruling).
 *
 * Chosen modes resolve in printed order (CR 608.2c), so a player who discards their hand to the
 * first mode has nothing left when the third mode asks for creatures. Both the discard and the
 * edict make their choices per player in turn order rather than fully simultaneously — see
 * [com.wingedsheep.sdk.dsl.HandPatterns.eachPlayerDiscards] for that deviation.
 */
val RanklesPrank = card("Rankle's Prank") {
    manaCost = "{2}{B}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "Choose one or more —\n" +
        "• Each player discards two cards.\n" +
        "• Each player loses 4 life.\n" +
        "• Each player sacrifices two creatures of their choice."

    spell {
        modal(chooseCount = 3, minChooseCount = 1) {
            mode("Each player discards two cards") {
                effect = Effects.EachPlayerDiscards(2)
            }
            mode("Each player loses 4 life") {
                effect = Effects.LoseLife(4, EffectTarget.PlayerRef(Player.Each))
            }
            mode("Each player sacrifices two creatures of their choice") {
                effect = Effects.Sacrifice(
                    GameObjectFilter.Creature,
                    count = 2,
                    target = EffectTarget.PlayerRef(Player.Each)
                )
            }
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "102"
        artist = "Tyler Walpole"
        flavorText = "The louder they scream, the harder he laughs."
        imageUri = "https://cards.scryfall.io/normal/front/e/8/e8a9bdcf-160a-48c9-9750-778c910b805d.jpg?1783915104"
        ruling("2023-09-01", "You can choose a mode even if some or all players will be entirely or partially unaffected.")
        ruling(
            "2023-09-01",
            "As the first mode is performed, first the player whose turn it is chooses two cards in hand " +
                "without revealing them, then each other player in turn order does the same. Then the chosen " +
                "cards are discarded at the same time."
        )
        ruling(
            "2023-09-01",
            "As the third mode is performed, first the player whose turn it is chooses two creatures they " +
                "control, then each other player in turn order does the same, knowing the choices made before " +
                "them. Then all the chosen creatures are sacrificed at the same time."
        )
    }
}
