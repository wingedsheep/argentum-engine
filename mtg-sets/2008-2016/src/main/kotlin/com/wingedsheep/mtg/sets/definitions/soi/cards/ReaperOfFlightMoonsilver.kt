package com.wingedsheep.mtg.sets.definitions.soi.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ActivationRestriction
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Reaper of Flight Moonsilver (Shadows over Innistrad #36)
 * {3}{W}{W}
 * Creature — Angel
 * 3/3
 *
 * Flying
 * Delirium — Sacrifice another creature: This creature gets +2/+1 until end of turn. Activate only
 * if there are four or more card types among cards in your graveyard.
 */
val ReaperOfFlightMoonsilver = card("Reaper of Flight Moonsilver") {
    manaCost = "{3}{W}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Angel"
    power = 3
    toughness = 3
    oracleText = "Flying\n" +
        "Delirium — Sacrifice another creature: This creature gets +2/+1 until end of turn. Activate only " +
        "if there are four or more card types among cards in your graveyard."

    keywords(Keyword.FLYING)

    activatedAbility {
        cost = Costs.SacrificeAnother(GameObjectFilter.Creature)
        effect = Effects.ModifyStats(2, 1, EffectTarget.Self)
        restrictions = listOf(ActivationRestriction.OnlyIfCondition(Conditions.Delirium()))
        description = "Sacrifice another creature: This creature gets +2/+1 until end of turn."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "36"
        artist = "Magali Villeneuve"
        imageUri = "https://cards.scryfall.io/normal/front/5/8/58cf3ac8-db59-4acc-abb6-e63b95b849ac.jpg"
    }
}
