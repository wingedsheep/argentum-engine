package com.wingedsheep.mtg.sets.definitions.mom.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.mode
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.events.Recipient
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.dsl.DynamicAmounts

/**
 * Rankle and Torbran — {1}{B}{B}{R}{R} Legendary Creature — Faerie Dwarf 3/4 (March of the Machine #252).
 *
 * "Choose any number" is a three-mode modal with `minChooseCount = 0`, chosen as the trigger goes on
 * the stack. The third mode is a floating [Effects.AmplifyDamageThisTurn]: it applies to every source,
 * yours or not, and to every player or battle — the controller included — until end of turn, and it
 * outlives Rankle and Torbran leaving the battlefield.
 */
val RankleAndTorbran = card("Rankle and Torbran") {
    manaCost = "{1}{B}{B}{R}{R}"
    colorIdentity = "BR"
    typeLine = "Legendary Creature — Faerie Dwarf"
    power = 3
    toughness = 4
    oracleText = "Flying, first strike, haste\n" +
        "Whenever Rankle and Torbran deals combat damage to a player or battle, choose any number —\n" +
        "• Each player creates a Treasure token.\n" +
        "• Each player sacrifices a creature of their choice.\n" +
        "• If a source would deal damage to a player or battle this turn, it deals that much damage " +
        "plus 2 instead."

    keywords(Keyword.FLYING, Keyword.FIRST_STRIKE, Keyword.HASTE)

    triggeredAbility {
        trigger = Triggers.self.dealsCombatDamage(Recipient.AnyPlayerOrBattle)
        effect = Effects.Modal(
            modes = listOf(
                mode("Each player creates a Treasure token") {
                    effect = Effects.ForEachPlayer(Player.ActivePlayerFirst, Effects.CreateTreasure())
                },
                mode("Each player sacrifices a creature of their choice") {
                    effect = Effects.Sacrifice(GameObjectFilter.Creature, 1, EffectTarget.PlayerRef(Player.Each))
                },
                mode("If a source would deal damage to a player or battle this turn, it deals that much damage plus 2 instead") {
                    effect = Effects.AmplifyDamageThisTurn(
                        DynamicAmounts.fixed(2),
                        EventPattern.DamageEvent(recipient = Recipient.AnyPlayerOrBattle),
                    )
                },
            ),
            chooseCount = 3,
            minChooseCount = 0,
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "252"
        artist = "Viko Menezes"
        imageUri = "https://cards.scryfall.io/normal/front/d/9/d92c191e-329c-41f5-ae4f-1bb91fc001a0.jpg?1783916938"

        ruling("2023-04-14", "You may choose none of the modes, some of them, or all of them. Any modes chosen will happen in order.")
        ruling(
            "2023-04-14",
            "The additional 2 damage is dealt by the same source as the original source of damage. The damage " +
                "isn't dealt by Rankle and Torbran unless Rankle and Torbran is the original source of damage."
        )
    }
}
