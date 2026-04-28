package com.wingedsheep.mtg.sets.definitions.lorwyneclipsed.cards

import com.wingedsheep.sdk.dsl.EffectPatterns
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CreatePredefinedTokenEffect
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject
import com.wingedsheep.sdk.scripting.targets.TargetPlayer

/**
 * Ashling's Command
 * {3}{U}{R}
 * Kindred Instant — Elemental
 *
 * Choose two —
 * • Create a token that's a copy of target Elemental you control.
 * • Target player draws two cards.
 * • Ashling's Command deals 2 damage to each creature target player controls.
 * • Target player creates two Treasure tokens.
 */
val AshlingsCommand = card("Ashling's Command") {
    manaCost = "{3}{U}{R}"
    typeLine = "Kindred Instant — Elemental"
    oracleText = "Choose two —\n" +
            "• Create a token that's a copy of target Elemental you control.\n" +
            "• Target player draws two cards.\n" +
            "• Ashling's Command deals 2 damage to each creature target player controls.\n" +
            "• Target player creates two Treasure tokens."

    spell {
        modal(chooseCount = 2) {
            mode("Create a token that's a copy of target Elemental you control") {
                val elemental = target(
                    "target Elemental you control",
                    TargetObject(filter = TargetFilter(GameObjectFilter.Creature.youControl().withSubtype("Elemental")))
                )
                effect = Effects.CreateTokenCopyOfTarget(elemental)
            }
            mode("Target player draws two cards") {
                val player = target("target player", TargetPlayer())
                effect = Effects.DrawCards(2, player)
            }
            mode("Ashling's Command deals 2 damage to each creature target player controls") {
                val player = target("target player", TargetPlayer())
                effect = EffectPatterns.dealDamageToAll(
                    amount = 2,
                    filter = GroupFilter(GameObjectFilter.Creature.targetPlayerControls(player))
                )
            }
            mode("Target player creates two Treasure tokens") {
                val player = target("target player", TargetPlayer())
                effect = CreatePredefinedTokenEffect("Treasure", count = 2, controller = player)
            }
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "205"
        artist = "Iris Compiet"
        imageUri = "https://cards.scryfall.io/normal/front/f/e/fe3a421d-07b6-4b94-b177-aec44c7fe689.jpg?1759144840"

        ruling("2025-11-17", "The token created by the first mode of Ashling's Command copies exactly what was printed on the original permanent and nothing else (unless that permanent is copying something else or is a token).")
        ruling("2025-11-17", "If the copied permanent is copying something else, then the token enters as whatever that creature copied.")
        ruling("2025-11-17", "If the copied permanent is a token, the token that's created copies the original characteristics of that token as stated by the effect that created that token.")
        ruling("2025-11-17", "Any enters abilities of the copied permanent will trigger when the token enters. Any \"as [this permanent] enters\" or \"[this permanent] enters with\" abilities of the copied permanent will also work.")
        ruling("2025-11-17", "If the copied permanent has {X} in its mana cost, X is 0.")
        ruling("2025-11-17", "If all of Ashling's Command's targets are illegal as it tries to resolve, it will do nothing. If at least one target is still legal, it will resolve and do as much as it can.")
    }
}
