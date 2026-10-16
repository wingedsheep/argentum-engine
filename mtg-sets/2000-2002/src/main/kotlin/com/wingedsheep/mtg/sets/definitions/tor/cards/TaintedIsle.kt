package com.wingedsheep.mtg.sets.definitions.tor.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.ActivationRestriction
import com.wingedsheep.sdk.scripting.TimingRule

/**
 * Tainted Isle
 * Land
 *
 * {T}: Add {C}.
 * {T}: Add {U} or {B}. Activate only if you control a Swamp.
 *
 * "Add {U} or {B}" is spelled as two mana abilities (the Tainted Wood shape), each gated by
 * the same "control a Swamp" restriction — any permanent with the Swamp subtype counts.
 */
val TaintedIsle = card("Tainted Isle") {
    typeLine = "Land"
    colorIdentity = "UB"
    oracleText = "{T}: Add {C}.\n{T}: Add {U} or {B}. Activate only if you control a Swamp."

    activatedAbility {
        cost = AbilityCost.Tap
        effect = Effects.AddColorlessMana(1)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = AbilityCost.Tap
        effect = Effects.AddMana(Color.BLUE)
        manaAbility = true
        timing = TimingRule.ManaAbility
        restrictions = listOf(ActivationRestriction.OnlyIfCondition(Conditions.ControlPermanentOfType(Subtype.SWAMP)))
    }

    activatedAbility {
        cost = AbilityCost.Tap
        effect = Effects.AddMana(Color.BLACK)
        manaAbility = true
        timing = TimingRule.ManaAbility
        restrictions = listOf(ActivationRestriction.OnlyIfCondition(Conditions.ControlPermanentOfType(Subtype.SWAMP)))
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "141"
        artist = "Alan Pollack"
        imageUri = "https://cards.scryfall.io/normal/front/b/4/b462e121-015c-49c4-838a-ab788f213322.jpg?1783945139"
    }
}
