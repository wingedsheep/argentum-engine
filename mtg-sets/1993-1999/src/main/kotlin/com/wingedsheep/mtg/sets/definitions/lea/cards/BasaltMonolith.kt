package com.wingedsheep.mtg.sets.definitions.lea.cards

import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Basalt Monolith
 * {3}
 * Artifact
 * This artifact doesn't untap during your untap step.
 * {T}: Add {C}{C}{C}.
 * {3}: Untap this artifact.
 *
 * "Doesn't untap" is the self-suppression flag [AbilityFlag.DOESNT_UNTAP], which the untap step
 * filters on (cf. Colossus of Sardia). The mana ability is [Effects.AddColorlessMana] for 3 on
 * [Costs.Tap] with [TimingRule.ManaAbility]; the pay-to-untap ability is a plain [Effects.Untap]
 * on [EffectTarget.Self] for [Costs.Mana] "{3}" — unlike Colossus it carries no activation
 * restriction, which is why it can be looped.
 */
val BasaltMonolith = card("Basalt Monolith") {
    manaCost = "{3}"
    colorIdentity = ""
    typeLine = "Artifact"
    oracleText = "This artifact doesn't untap during your untap step.\n" +
        "{T}: Add {C}{C}{C}.\n" +
        "{3}: Untap this artifact."

    flags(AbilityFlag.DOESNT_UNTAP)

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddColorlessMana(3)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = Costs.Mana("{3}")
        effect = Effects.Untap(EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "231"
        artist = "Jesper Myrfors"
        imageUri = "https://cards.scryfall.io/normal/front/6/6/66a74c89-6f86-4ec8-af17-391cd5026054.jpg"
        ruling("2020-08-07", "Basalt Monolith's last ability can untap it as often as you can pay for it. If you believe you've found a way to generate an unbounded amount of mana with it, you're probably right.")
    }
}
