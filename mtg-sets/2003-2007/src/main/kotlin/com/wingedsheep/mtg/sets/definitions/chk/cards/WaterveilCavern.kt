package com.wingedsheep.mtg.sets.definitions.chk.cards

import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Waterveil Cavern
 * Land
 *
 * {T}: Add {C}.
 * {T}: Add {U} or {B}. This land doesn't untap during your next untap step.
 *
 * The freeze is part of the coloured ability's *effect*, so it also lands when the mana solver
 * auto-taps the land (ManaAbilitySideEffectExecutor runs a mana ability's non-mana leaves).
 */
val WaterveilCavern = card("Waterveil Cavern") {
    typeLine = "Land"
    colorIdentity = "UB"
    oracleText = "{T}: Add {C}.\n{T}: Add {U} or {B}. This land doesn't untap during your next untap step."

    activatedAbility {
        cost = AbilityCost.Tap
        effect = Effects.AddColorlessMana(1)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = AbilityCost.Tap
        effect = Effects.AddMana(Color.BLUE) then Effects.GrantKeyword(
            AbilityFlag.DOESNT_UNTAP,
            EffectTarget.Self,
            Duration.UntilAfterAffectedControllersNextUntap,
        )
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = AbilityCost.Tap
        effect = Effects.AddMana(Color.BLACK) then Effects.GrantKeyword(
            AbilityFlag.DOESNT_UNTAP,
            EffectTarget.Self,
            Duration.UntilAfterAffectedControllersNextUntap,
        )
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "286"
        artist = "John Avon"
        imageUri = "https://cards.scryfall.io/normal/front/4/0/405805e0-07f6-420f-86f9-5bde822caa5c.jpg?1783944271"
    }
}
