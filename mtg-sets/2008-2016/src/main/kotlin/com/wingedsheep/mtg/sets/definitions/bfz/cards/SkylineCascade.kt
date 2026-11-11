package com.wingedsheep.mtg.sets.definitions.bfz.cards

import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.EntersTapped
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Skyline Cascade
 * Land
 * This land enters tapped.
 * When this land enters, target creature an opponent controls doesn't untap during its controller's
 * next untap step.
 * {T}: Add {U}.
 */
val SkylineCascade = card("Skyline Cascade") {
    manaCost = ""
    colorIdentity = "U"
    typeLine = "Land"
    oracleText = "This land enters tapped.\n" +
        "When this land enters, target creature an opponent controls doesn't untap during its controller's next untap step.\n" +
        "{T}: Add {U}."

    replacementEffect(EntersTapped())

    triggeredAbility {
        trigger = Triggers.self.enters()
        val creature = target(TargetFilter.CreatureOpponentControls)
        effect = Effects.GrantKeyword(
            AbilityFlag.DOESNT_UNTAP,
            creature,
            Duration.UntilAfterAffectedControllersNextUntap(),
        )
    }

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddMana(Color.BLUE)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "246"
        artist = "Philip Straub"
        imageUri = "https://cards.scryfall.io/normal/front/2/9/29b0027d-c232-4cdd-89c4-75947687aa71.jpg?1783938172"
    }
}
