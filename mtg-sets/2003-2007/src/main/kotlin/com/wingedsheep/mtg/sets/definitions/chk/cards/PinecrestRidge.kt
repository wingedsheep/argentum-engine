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
 * Pinecrest Ridge
 * Land
 *
 * {T}: Add {C}.
 * {T}: Add {R} or {G}. This land doesn't untap during your next untap step.
 *
 * The freeze is part of the coloured ability's *effect*, so it also lands when the mana solver
 * auto-taps the land (ManaAbilitySideEffectExecutor runs a mana ability's non-mana leaves).
 */
val PinecrestRidge = card("Pinecrest Ridge") {
    typeLine = "Land"
    colorIdentity = "RG"
    oracleText = "{T}: Add {C}.\n{T}: Add {R} or {G}. This land doesn't untap during your next untap step."

    activatedAbility {
        cost = AbilityCost.Tap
        effect = Effects.AddColorlessMana(1)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = AbilityCost.Tap
        effect = Effects.AddMana(Color.RED) then Effects.GrantKeyword(
            AbilityFlag.DOESNT_UNTAP,
            EffectTarget.Self,
            Duration.UntilAfterAffectedControllersNextUntap,
        )
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = AbilityCost.Tap
        effect = Effects.AddMana(Color.GREEN) then Effects.GrantKeyword(
            AbilityFlag.DOESNT_UNTAP,
            EffectTarget.Self,
            Duration.UntilAfterAffectedControllersNextUntap,
        )
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "281"
        artist = "John Avon"
        imageUri = "https://cards.scryfall.io/normal/front/7/9/7900552f-6147-49ec-8c02-f253e4896c4d.jpg?1783944272"
    }
}
