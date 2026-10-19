package com.wingedsheep.mtg.sets.definitions.leg.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.effects.EffectChoice
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Urborg
 * Legendary Land
 *
 * {T}: Add {B}.
 * {T}: Target creature loses first strike or swampwalk until end of turn.
 *
 * The ability's controller picks which keyword goes as the ability resolves — the
 * Dwarven Armorer "this or that" shape, each option a Scarwood Hag–style keyword loss.
 */
val Urborg = card("Urborg") {
    typeLine = "Legendary Land"
    colorIdentity = "B"
    oracleText = "{T}: Add {B}.\n{T}: Target creature loses first strike or swampwalk until end of turn."

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddMana(Color.BLACK)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        val creature = target(TargetFilter.Creature)
        cost = Costs.Tap
        effect = Effects.ChooseAction(listOf(
            EffectChoice(
                label = "It loses first strike",
                effect = Effects.RemoveKeyword(Keyword.FIRST_STRIKE, target = creature),
            ),
            EffectChoice(
                label = "It loses swampwalk",
                effect = Effects.RemoveKeyword(Keyword.SWAMPWALK, target = creature),
            )
        ))
        description = "{T}: Target creature loses first strike or swampwalk until end of turn."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "310"
        artist = "Bryon Wackwitz"
        imageUri = "https://cards.scryfall.io/normal/front/d/0/d0a261d0-7678-46f7-9285-d541486567d8.jpg?1783948022"
    }
}
