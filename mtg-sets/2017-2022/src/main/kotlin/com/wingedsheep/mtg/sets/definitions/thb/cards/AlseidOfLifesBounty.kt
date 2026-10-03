package com.wingedsheep.mtg.sets.definitions.thb.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Alseid of Life's Bounty
 * {W}
 * Enchantment Creature — Nymph
 * 1/1
 * Lifelink
 * {1}, Sacrifice this creature: Target creature or enchantment you control gains protection from
 * the color of your choice until end of turn.
 *
 * Sygg, River Guide's shape: [Effects.ChooseColorThen] wraps
 * [Effects.GrantProtectionFromChosenColor], so the colour is chosen on resolution, after the
 * Alseid has already been sacrificed as a cost.
 */
val AlseidOfLifesBounty = card("Alseid of Life's Bounty") {
    manaCost = "{W}"
    colorIdentity = "W"
    typeLine = "Enchantment Creature — Nymph"
    power = 1
    toughness = 1
    oracleText = "Lifelink\n" +
        "{1}, Sacrifice this creature: Target creature or enchantment you control gains protection " +
        "from the color of your choice until end of turn."

    keywords(Keyword.LIFELINK)

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{1}"), Costs.SacrificeSelf)
        val t = target(TargetFilter.CreatureOrEnchantment.youControl())
        effect = Effects.ChooseColorThen(Effects.GrantProtectionFromChosenColor(t))
        description = "Target creature or enchantment you control gains protection from the color " +
            "of your choice until end of turn."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "1"
        artist = "Magali Villeneuve"
        flavorText = "\"Dawn-kissed, he nourishes the golden grain.\"\n—Psemilla, Meletian poet"
        imageUri = "https://cards.scryfall.io/normal/front/3/6/36c8c075-9597-412e-9fc4-9d73b4405d12.jpg?1783931603"
    }
}
