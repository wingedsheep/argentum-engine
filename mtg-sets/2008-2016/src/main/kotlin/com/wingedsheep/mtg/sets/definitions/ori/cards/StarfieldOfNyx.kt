package com.wingedsheep.mtg.sets.definitions.ori.cards

import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantCardType
import com.wingedsheep.sdk.scripting.SetBasePowerToughnessDynamicStatic
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Starfield of Nyx — Magic Origins #33
 * {4}{W} · Enchantment
 *
 * At the beginning of your upkeep, you may return target enchantment card from your graveyard to
 * the battlefield.
 * As long as you control five or more enchantments, each other non-Aura enchantment you control is
 * a creature in addition to its other types and has base power and base toughness each equal to
 * its mana value.
 *
 * - The upkeep trigger is Archon of Falling Stars' optional graveyard-to-battlefield return; an
 *   Aura returned this way is attached as it enters without targeting (rulings).
 * - The animation is March of the Machines' two group statics — Layer 4 [GrantCardType] CREATURE
 *   and Layer 7b [SetBasePowerToughnessDynamicStatic] fed each permanent's own mana value — over
 *   the other non-Aura enchantments you control, both gated on
 *   [Conditions.YouControlAtLeast] five enchantments (Starfield itself counts toward the five).
 *   Enchantments that are already creatures are covered too: they get base P/T equal to their
 *   mana value.
 */
val StarfieldOfNyx = card("Starfield of Nyx") {
    manaCost = "{4}{W}"
    colorIdentity = "W"
    typeLine = "Enchantment"
    oracleText = "At the beginning of your upkeep, you may return target enchantment card from your " +
        "graveyard to the battlefield.\n" +
        "As long as you control five or more enchantments, each other non-Aura enchantment you control " +
        "is a creature in addition to its other types and has base power and base toughness each equal " +
        "to its mana value."

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.UPKEEP)
        optional = true
        val enchantment = target(TargetFilter(GameObjectFilter.Enchantment.ownedByYou(), zone = Zone.GRAVEYARD))
        effect = Effects.PutOntoBattlefieldFromGraveyard(enchantment)
        description = "At the beginning of your upkeep, you may return target enchantment card from your " +
            "graveyard to the battlefield."
    }

    val fiveEnchantments = Conditions.YouControlAtLeast(5, GameObjectFilter.Enchantment)
    val otherNonAuraEnchantments = GroupFilter(
        GameObjectFilter.Enchantment.notSubtype(Subtype.AURA).youControl(),
        excludeSelf = true,
    )
    val manaValue: DynamicAmount = DynamicAmounts.manaValueOf(EffectTarget.AffectedEntity)

    staticAbility {
        condition = fiveEnchantments
        ability = GrantCardType(cardType = "CREATURE", filter = otherNonAuraEnchantments)
    }
    staticAbility {
        condition = fiveEnchantments
        ability = SetBasePowerToughnessDynamicStatic(
            power = manaValue,
            toughness = manaValue,
            filter = otherNonAuraEnchantments,
        )
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "33"
        artist = "Tyler Jacobson"
        imageUri = "https://cards.scryfall.io/normal/front/8/6/861dc1a5-4e84-47bc-83a2-f289804086d8.jpg?1783938358"
        ruling(
            "2015-06-22",
            "If the first ability returns an Aura card to the battlefield, you choose what that Aura will " +
                "enchant as it enters the battlefield. If the Aura can't legally enchant anything, it stays " +
                "in the graveyard."
        )
        ruling(
            "2015-06-22",
            "A noncreature permanent that turns into a creature can attack, and its {T} abilities can be " +
                "activated, only if its controller has continuously controlled that permanent since the " +
                "beginning of their most recent turn. It doesn't matter how long the permanent has been a creature."
        )
    }
}
