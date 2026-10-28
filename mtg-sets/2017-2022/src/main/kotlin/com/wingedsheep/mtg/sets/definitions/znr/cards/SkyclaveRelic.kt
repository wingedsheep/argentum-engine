package com.wingedsheep.mtg.sets.definitions.znr.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Skyclave Relic — Zendikar Rising #252
 * {3} · Artifact
 *
 * Kicker {3}
 * Indestructible
 * When this artifact enters, if it was kicked, create two tapped tokens that are copies of this artifact.
 * {T}: Add one mana of any color.
 *
 * "If it was kicked" is an intervening if (CR 603.4) on [Conditions.WasKicked]. The token copies
 * weren't cast, so they aren't kicked and their own copy of the trigger never goes on the stack.
 */
val SkyclaveRelic = card("Skyclave Relic") {
    manaCost = "{3}"
    colorIdentity = ""
    typeLine = "Artifact"
    oracleText = "Kicker {3}\n" +
        "Indestructible\n" +
        "When this artifact enters, if it was kicked, create two tapped tokens that are copies of this artifact.\n" +
        "{T}: Add one mana of any color."

    keywordAbility(KeywordAbility.kicker("{3}"))

    keywords(Keyword.INDESTRUCTIBLE)

    triggeredAbility {
        trigger = Triggers.self.enters()
        interveningIf = Conditions.WasKicked
        effect = Effects.CreateTokenCopyOfTarget(target = EffectTarget.Self, count = 2, tapped = true)
        description = "When this artifact enters, if it was kicked, create two tapped tokens that are copies of this artifact."
    }

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddManaOfChoice()
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "252"
        artist = "Daniel Ljunggren"
        flavorText = "Skyclaves held the key to reshaping the world."
        imageUri = "https://cards.scryfall.io/normal/front/2/0/20259dff-4984-4d0b-aa49-34f3aaf31e3a.jpg?1783929310"
        ruling(
            "2024-11-08",
            "If a card or token enters as a copy of a permanent, the new permanent isn't kicked, even if the original was."
        )
    }
}
