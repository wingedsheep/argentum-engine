package com.wingedsheep.mtg.sets.definitions.dsk.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Sawblade Skinripper
 * {1}{B}{R}
 * Creature — Human Assassin
 * 3/2
 * Menace
 * {2}, Sacrifice another creature or enchantment: Put a +1/+1 counter on this creature.
 * At the beginning of your end step, if you sacrificed one or more permanents this turn,
 * this creature deals that much damage to any target.
 */
val SawbladeSkinripper = card("Sawblade Skinripper") {
    manaCost = "{1}{B}{R}"
    colorIdentity = "BR"
    typeLine = "Creature — Human Assassin"
    power = 3
    toughness = 2
    oracleText = "Menace\n" +
        "{2}, Sacrifice another creature or enchantment: Put a +1/+1 counter on this creature.\n" +
        "At the beginning of your end step, if you sacrificed one or more permanents this turn, " +
        "this creature deals that much damage to any target."

    keywords(Keyword.MENACE)

    activatedAbility {
        cost = Costs.Composite(
            Costs.Mana("{2}"),
            Costs.SacrificeAnother(GameObjectFilter.CreatureOrEnchantment),
        )
        effect = Effects.AddCounters(Counters.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)
    }

    // Intervening-if on the per-player "permanents sacrificed this turn" counter. "That much"
    // is the same controller-scoped count, dealt by Sawblade itself to any target.
    triggeredAbility {
        trigger = Triggers.YourEndStep
        triggerCondition = Conditions.YouSacrificedPermanentsThisTurn()
        val any = target("any target", Targets.Any)
        effect = Effects.DealDamage(
            DynamicAmounts.permanentsSacrificedThisTurn(),
            any,
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "231"
        artist = "Zezhou Chen"
        imageUri = "https://cards.scryfall.io/normal/front/c/b/cbe51964-50d2-49b0-9c3c-81751dcbeade.jpg?1726286730"
    }
}
