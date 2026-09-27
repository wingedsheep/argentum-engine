package com.wingedsheep.mtg.sets.definitions.chk.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Honor-Worn Shaku
 * {3}
 * Artifact
 * {T}: Add {C}.
 * Tap an untapped legendary permanent you control: Untap this artifact.
 */
val HonorWornShaku = card("Honor-Worn Shaku") {
    manaCost = "{3}"
    colorIdentity = ""
    typeLine = "Artifact"
    oracleText = "{T}: Add {C}.\nTap an untapped legendary permanent you control: Untap this artifact."

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddColorlessMana(1)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = Costs.TapPermanents(1, GameObjectFilter.Permanent.legendary())
        effect = Effects.Untap(EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "254"
        artist = "Tony Szczudlo"
        flavorText = "Before being presented to its lord, each shaku is blessed by every hero of every region within that lord's domain."
        imageUri = "https://cards.scryfall.io/normal/front/b/a/babe91f2-06be-4501-a95b-20968e906e1b.jpg?1783944279"
    }
}
