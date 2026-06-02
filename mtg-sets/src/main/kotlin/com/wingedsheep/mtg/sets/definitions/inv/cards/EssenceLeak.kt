package com.wingedsheep.mtg.sets.definitions.inv.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.scripting.ConditionalStaticAbility
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantTriggeredAbility
import com.wingedsheep.sdk.scripting.TriggeredAbility
import com.wingedsheep.sdk.scripting.effects.PayOrSufferEffect
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.dsl.Costs

/**
 * Essence Leak
 * {U}
 * Enchantment — Aura
 * Enchant permanent
 * As long as enchanted permanent is red or green, it has "At the beginning of your
 * upkeep, sacrifice this permanent unless you pay its mana cost."
 *
 * Modeled as a [ConditionalStaticAbility] gating a [GrantTriggeredAbility] over the
 * attached permanent ([GroupFilter.attachedCreature], which is scope-by-attachment and
 * works for any permanent type). The condition [Conditions.EnchantedPermanentMatches]
 * checks the enchanted permanent's color in projected state. The granted upkeep trigger
 * fires on the enchanted permanent's controller ([Triggers.YourUpkeep]) and uses a
 * [PayOrSufferEffect] with [Costs.pay.OwnManaCost] — "pay its mana cost" reads the enchanted
 * permanent's own mana cost — otherwise that permanent ([EffectTarget.Self]) is sacrificed.
 */
val EssenceLeak = card("Essence Leak") {
    manaCost = "{U}"
    colorIdentity = "U"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant permanent\n" +
        "As long as enchanted permanent is red or green, it has \"At the beginning of your " +
        "upkeep, sacrifice this permanent unless you pay its mana cost.\""

    auraTarget = Targets.Permanent

    staticAbility {
        ability = ConditionalStaticAbility(
            ability = GrantTriggeredAbility(
                ability = TriggeredAbility.create(
                    trigger = Triggers.YourUpkeep.event,
                    binding = Triggers.YourUpkeep.binding,
                    effect = PayOrSufferEffect(
                        cost = Costs.pay.OwnManaCost,
                        suffer = Effects.SacrificeTarget(EffectTarget.Self)
                    )
                ),
                filter = GroupFilter.attachedCreature()
            ),
            condition = Conditions.EnchantedPermanentMatches(
                GameObjectFilter.Permanent.withAnyColor(Color.RED, Color.GREEN)
            )
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "55"
        artist = "Adam Rex"
        imageUri = "https://cards.scryfall.io/normal/front/9/0/9099b2e6-9ed8-4a9c-97ca-77cc47678228.jpg?1562924181"
    }
}
