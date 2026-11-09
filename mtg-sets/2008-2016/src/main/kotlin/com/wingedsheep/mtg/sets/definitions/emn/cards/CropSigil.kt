package com.wingedsheep.mtg.sets.definitions.emn.cards

import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ActivationRestriction
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Crop Sigil (Eldritch Moon #153)
 * {G}
 * Enchantment
 *
 * At the beginning of your upkeep, you may mill a card.
 * Delirium — {2}{G}, Sacrifice this enchantment: Return up to one target creature card and up to
 * one target land card from your graveyard to your hand. Activate only if there are four or more
 * card types among cards in your graveyard.
 *
 * The upkeep mill is a `May` (Patchwork Beastie idiom). The delirium ability takes two independent
 * optional graveyard slots — a creature card and a land card you own — each returned to hand
 * (Retrieve idiom), gated by `OnlyIfCondition(Delirium())`, which is checked as the ability is
 * activated, before Crop Sigil is sacrificed for its cost.
 */
val CropSigil = card("Crop Sigil") {
    manaCost = "{G}"
    colorIdentity = "G"
    typeLine = "Enchantment"
    oracleText = "At the beginning of your upkeep, you may mill a card.\nDelirium — {2}{G}, Sacrifice " +
        "this enchantment: Return up to one target creature card and up to one target land card from " +
        "your graveyard to your hand. Activate only if there are four or more card types among cards " +
        "in your graveyard."

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.UPKEEP)
        effect = Effects.May(
            effect = Patterns.Library.mill(1),
            descriptionOverride = "You may mill a card.",
        )
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{2}{G}"), Costs.SacrificeSelf)
        restrictions = listOf(ActivationRestriction.OnlyIfCondition(Conditions.Delirium()))
        val creature = target(TargetFilter.CreatureInYourGraveyard, optional = true)
        val land = target(
            TargetFilter(GameObjectFilter.Land.ownedByYou(), zone = Zone.GRAVEYARD),
            optional = true,
        )
        effect = Effects.ReturnToHand(creature) then Effects.ReturnToHand(land)
        description = "Delirium — {2}{G}, Sacrifice this enchantment: Return up to one target creature " +
            "card and up to one target land card from your graveyard to your hand."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "153"
        artist = "Titus Lunter"
        imageUri = "https://cards.scryfall.io/normal/front/8/6/861e8c51-0cda-497a-b289-a9f678f0e328.jpg?1783937449"
    }
}
