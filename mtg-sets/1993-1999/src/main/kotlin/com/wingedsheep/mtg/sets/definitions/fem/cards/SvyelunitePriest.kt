package com.wingedsheep.mtg.sets.definitions.fem.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ActivationRestriction
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetCreature

/**
 * Svyelunite Priest
 * {1}{U}
 * Creature — Merfolk Cleric
 * 1/1
 * {U}{U}, {T}: Target creature gains shroud until end of turn. Activate only during your upkeep.
 */
val SvyelunitePriest = card("Svyelunite Priest") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Merfolk Cleric"
    oracleText = "{U}{U}, {T}: Target creature gains shroud until end of turn. Activate only " +
        "during your upkeep. (It can't be the target of spells or abilities.)"
    power = 1
    toughness = 1

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{U}{U}"), Costs.Tap)
        restrictions = listOf(
            ActivationRestriction.All(
                ActivationRestriction.OnlyDuringYourTurn,
                ActivationRestriction.DuringStep(Step.UPKEEP)
            )
        )
        val t = target("target creature", TargetCreature(filter = TargetFilter.Creature))
        effect = Effects.GrantKeyword(Keyword.SHROUD, t)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "26"
        artist = "Ron Spencer"
        flavorText = "\"Early Vodalians worshipped Svyelun, goddess of the Pearl Moon. Later she became a more abstract figure.\"\n—*Sarpadian Empires, vol. V*"
        imageUri = "https://cards.scryfall.io/normal/front/3/1/316d25ae-7ac6-4f5b-93ab-0e0e28ec104b.jpg?1783947912"
    }
}
