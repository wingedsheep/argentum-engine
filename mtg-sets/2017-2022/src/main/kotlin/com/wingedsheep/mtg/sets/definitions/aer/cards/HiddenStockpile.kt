package com.wingedsheep.mtg.sets.definitions.aer.cards

import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Hidden Stockpile — Aether Revolt #129 (canonical / earliest real printing, 2017)
 * {W}{B} · Enchantment
 *
 * Revolt — At the beginning of your end step, if a permanent left the battlefield under your
 * control this turn, create a 1/1 colorless Servo artifact creature token.
 * {1}, Sacrifice a creature: Scry 1.
 *
 * - Revolt is an intervening-if on the end-step trigger, read off the per-player
 *   [Conditions.YouHadPermanentLeaveBattlefieldThisTurn] tracker (same shape as Call for Unity).
 * - The Servo is [Effects.CreateToken] with `artifactToken = true`; art comes from the set's
 *   `tokenArt` Servo entry.
 * - Sacrificing a creature to the scry ability itself satisfies revolt for that turn.
 */
val HiddenStockpile = card("Hidden Stockpile") {
    manaCost = "{W}{B}"
    colorIdentity = "WB"
    typeLine = "Enchantment"
    oracleText = "Revolt — At the beginning of your end step, if a permanent left the battlefield " +
        "under your control this turn, create a 1/1 colorless Servo artifact creature token.\n" +
        "{1}, Sacrifice a creature: Scry 1."

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.END)
        interveningIf = Conditions.YouHadPermanentLeaveBattlefieldThisTurn
        effect = Effects.CreateToken(
            power = 1,
            toughness = 1,
            creatureTypes = setOf("Servo"),
            artifactToken = true,
        )
        description = "Revolt — At the beginning of your end step, if a permanent left the battlefield " +
            "under your control this turn, create a 1/1 colorless Servo artifact creature token."
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{1}"), Costs.Sacrifice(GameObjectFilter.Creature))
        effect = Effects.Scry(1)
        description = "{1}, Sacrifice a creature: Scry 1."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "129"
        artist = "Darek Zabrocki"
        imageUri = "https://cards.scryfall.io/normal/front/8/d/8d2176a5-66f8-4a21-bbe2-969c82ca36bd.jpg?1783936737"
        ruling("2020-08-07", "If a permanent you controlled didn't leave the battlefield before your end step begins, Hidden Stockpile's first ability doesn't trigger at all. The permanent may have left before Hidden Stockpile entered the battlefield, however.")
        ruling("2020-08-07", "Revolt abilities check only whether a permanent you controlled left the battlefield this turn or not. They don't apply multiple times if more than one permanent you controlled left the battlefield. They don't check whether the permanent that left the battlefield is still in the zone it moved to.")
        ruling("2020-08-07", "Revolt abilities don't care why the permanent left the battlefield, who caused it to move, or where it moved to. They're equally satisfied by an artifact you sacrificed to pay a cost, a creature you controlled that was destroyed by Cast Down, or an enchantment you returned to your hand with Cyclonic Rift.")
        ruling("2020-08-07", "Tokens that leave the battlefield will satisfy a revolt ability.")
    }
}
