package com.wingedsheep.mtg.sets.definitions.akh.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Hazoret's Favor — Amonkhet #137 (canonical printing)
 * {2}{R} · Enchantment
 *
 * At the beginning of combat on your turn, you may have target creature you control get +2/+0 and
 * gain haste until end of turn. If you do, sacrifice it at the beginning of the next end step.
 *
 * The target is chosen when the trigger goes on the stack; the "may" is decided on resolution, and
 * only a "yes" creates the delayed sacrifice trigger ("If you do").
 */
val HazoretsFavor = card("Hazoret's Favor") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Enchantment"
    oracleText = "At the beginning of combat on your turn, you may have target creature you control get +2/+0 " +
        "and gain haste until end of turn. If you do, sacrifice it at the beginning of the next end step."

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.BEGIN_COMBAT)
        val creature = target(TargetFilter.CreatureYouControl)
        effect = Effects.May(
            Effects.ModifyStats(2, 0, creature) then
                Effects.GrantKeyword(Keyword.HASTE, creature) then
                Effects.CreateDelayedTrigger(
                    step = Step.END,
                    effect = Effects.SacrificeTarget(creature),
                )
        )
        description = "At the beginning of combat on your turn, you may have target creature you control get " +
            "+2/+0 and gain haste until end of turn. If you do, sacrifice it at the beginning of the next end step."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "137"
        artist = "Raymond Swanland"
        flavorText = "Hazoret's gift is bestowed only once."
        imageUri = "https://cards.scryfall.io/normal/front/8/b/8b467412-c06b-4cd9-a877-7722051fb758.jpg?1783936488"
    }
}
