package com.wingedsheep.mtg.sets.definitions.ltr.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantTriggeredAbility
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.TriggeredAbility
import com.wingedsheep.sdk.scripting.effects.PayOrSufferEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.dsl.Costs

/**
 * Morgul-Knife Wound
 * {1}{B}
 * Enchantment — Aura
 *
 * Enchant creature
 * Enchanted creature gets -3/-0 and has "At the beginning of your upkeep, exile this creature
 * unless you pay 2 life."
 */
val MorgulKnifeWound = card("Morgul-Knife Wound") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature\n" +
        "Enchanted creature gets -3/-0 and has \"At the beginning of your upkeep, exile this creature unless you pay 2 life.\""

    auraTarget = Targets.Creature

    // Enchanted creature gets -3/-0.
    staticAbility {
        ability = ModifyStats(-3, 0)
    }

    // ...and has "At the beginning of your upkeep, exile this creature unless you pay 2 life."
    staticAbility {
        ability = GrantTriggeredAbility(
            TriggeredAbility.create(
                trigger = Triggers.YourUpkeep.event,
                binding = Triggers.YourUpkeep.binding,
                effect = PayOrSufferEffect(
                    cost = Costs.pay.PayLife(2),
                    suffer = Effects.Exile(EffectTarget.Self)
                )
            )
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "98"
        artist = "Axel Sauerwald"
        flavorText = "Frodo felt a pain like a dart of poisoned ice pierce his left shoulder."
        imageUri = "https://cards.scryfall.io/normal/front/5/a/5ae08177-48ee-4404-834d-d3cd7482ae81.jpg?1686968608"
    }
}
