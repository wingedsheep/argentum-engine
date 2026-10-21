package com.wingedsheep.mtg.sets.definitions.afr.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Farideh, Devil's Chosen
 * {2}{U}{R}
 * Legendary Creature — Tiefling Warlock
 * 3/3
 * Dark One's Own Luck — Whenever you roll one or more dice, Farideh gains flying and menace until
 * end of turn. If any of those results was 10 or higher, draw a card.
 *
 * "Any of those results was 10 or higher" is the roll's highest result, compared on resolution.
 */
val FaridehDevilsChosen = card("Farideh, Devil's Chosen") {
    manaCost = "{2}{U}{R}"
    colorIdentity = "UR"
    typeLine = "Legendary Creature — Tiefling Warlock"
    oracleText = "Dark One's Own Luck — Whenever you roll one or more dice, Farideh gains flying and menace " +
        "until end of turn. If any of those results was 10 or higher, draw a card."
    power = 3
    toughness = 3

    triggeredAbility {
        trigger = Triggers.you.rollsDice()
        effect = Effects.GrantKeyword(Keyword.FLYING, EffectTarget.Self) then
            Effects.GrantKeyword(Keyword.MENACE, EffectTarget.Self) then
            Effects.If(
                Conditions.CompareAmounts(DynamicAmounts.triggerHighestDieResult(), ComparisonOperator.GTE, 10),
                Effects.DrawCards(1),
            )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "221"
        artist = "Magali Villeneuve"
        flavorText = "\"A pact is a tool, not a damnation.\""
        imageUri = "https://cards.scryfall.io/normal/front/1/b/1b2ff142-029b-4446-960a-2ff200fb9aee.jpg?1783926449"
    }
}
