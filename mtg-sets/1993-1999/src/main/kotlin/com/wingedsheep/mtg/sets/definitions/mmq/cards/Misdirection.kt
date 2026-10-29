package com.wingedsheep.mtg.sets.definitions.mmq.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CostZone
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.SelfAlternativeCost
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Misdirection — Mercadian Masques #87 (canonical printing)
 * {3}{U}{U} · Instant
 *
 * You may exile a blue card from your hand rather than pay this spell's mana cost.
 * Change the target of target spell with a single target.
 *
 * The pitch cost is Force of Will's [SelfAlternativeCost] shape without the life payment: {0} plus
 * one blue card exiled from hand. "With a single target" is a targeting restriction
 * (`withSingleTarget()`, as on Hydroelectric Specimen), and [Effects.ChangeTarget] asks the caster
 * for the new target on resolution from the spell's own requirement — with no other legal target
 * the target is left unchanged (ruling).
 */
val Misdirection = card("Misdirection") {
    manaCost = "{3}{U}{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "You may exile a blue card from your hand rather than pay this spell's mana cost.\n" +
        "Change the target of target spell with a single target."

    selfAlternativeCost = SelfAlternativeCost(
        manaCost = ManaCost.parse("{0}"),
        additionalCosts = listOf(
            Costs.additional.ExileCards(
                count = 1,
                filter = GameObjectFilter.Any.withColor(Color.BLUE),
                fromZone = CostZone.HAND
            )
        )
    )

    spell {
        target(TargetFilter.SpellOnStack.withSingleTarget())
        effect = Effects.ChangeTarget()
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "87"
        artist = "Paolo Parente"
        imageUri = "https://cards.scryfall.io/normal/front/5/8/581ad59c-29e9-4498-a6fd-33bf21e8e7c4.jpg?1783945965"
        ruling("2004-10-04", "This only targets the spell being changed, not the original or new target of the spell it is affecting.")
        ruling("2004-10-04", "You can't make a spell which is on the stack target itself.")
        ruling("2004-10-04", "If there is no other legal target for the spell, this does not change the target.")
        ruling("2004-10-04", "This does not check if the current target is legal. It just checks if the spell has a single target.")
        ruling("2004-10-04", "You choose the spell to target on announcement, but you pick the new target for that spell on resolution.")
    }
}
