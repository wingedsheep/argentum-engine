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
 * Tainted Wood
 * Land
 *
 * {T}: Add {C}.
 * {T}: Add {B} or {G}. Activate only if you control a Swamp.
 *
 * "Add {B} or {G}" is spelled as two mana abilities (the Llanowar Wastes shape), each gated by
 * the same "control a Swamp" restriction — any permanent with the Swamp subtype counts.
 */
val TaintedWood = card("Tainted Wood") {
    typeLine = "Land"
    colorIdentity = "BG"
    oracleText = "{T}: Add {C}.\n{T}: Add {B} or {G}. Activate only if you control a Swamp."

    activatedAbility {
        cost = AbilityCost.Tap
        effect = Effects.AddColorlessMana(1)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = AbilityCost.Tap
        effect = Effects.AddMana(Color.BLACK)
        manaAbility = true
        timing = TimingRule.ManaAbility
        restrictions = listOf(ActivationRestriction.OnlyIfCondition(Conditions.ControlPermanentOfType(Subtype.SWAMP)))
    }

    activatedAbility {
        cost = AbilityCost.Tap
        effect = Effects.AddMana(Color.GREEN)
        manaAbility = true
        timing = TimingRule.ManaAbility
        restrictions = listOf(ActivationRestriction.OnlyIfCondition(Conditions.ControlPermanentOfType(Subtype.SWAMP)))
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "143"
        artist = "Rob Alexander"
        imageUri = "https://cards.scryfall.io/normal/front/a/2/a20a35cc-69e5-42b8-b28c-ae5147451150.jpg?1783945138"
    }
}
