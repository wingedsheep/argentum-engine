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
 * Lantern-Lit Graveyard
 * Land
 *
 * {T}: Add {C}.
 * {T}: Add {B} or {R}. This land doesn't untap during your next untap step.
 *
 * The freeze is part of the coloured ability's *effect*, so it also lands when the mana solver
 * auto-taps the land (ManaAbilitySideEffectExecutor runs a mana ability's non-mana leaves).
 */
val LanternLitGraveyard = card("Lantern-Lit Graveyard") {
    typeLine = "Land"
    colorIdentity = "BR"
    oracleText = "{T}: Add {C}.\n{T}: Add {B} or {R}. This land doesn't untap during your next untap step."

    activatedAbility {
        cost = AbilityCost.Tap
        effect = Effects.AddColorlessMana(1)
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

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "278"
        artist = "John Avon"
        imageUri = "https://cards.scryfall.io/normal/front/4/8/484a7675-787f-49be-9b44-edd0a7d73812.jpg?1783944273"
    }
}
