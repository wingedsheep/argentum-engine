package com.wingedsheep.mtg.sets.definitions.khans.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.EntersTapped
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.effects.AddManaEffect

/**
 * Wind-Scarred Crag
 * Land
 * Wind-Scarred Crag enters the battlefield tapped.
 * When Wind-Scarred Crag enters the battlefield, you gain 1 life.
 * {T}: Add {R} or {W}.
 */
val WindScarredCrag = card("Wind-Scarred Crag") {
    typeLine = "Land"
    oracleText = "This land enters tapped.\nWhen this land enters, you gain 1 life.\n{T}: Add {R} or {W}."

    replacementEffect(EntersTapped())

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = Effects.GainLife(1)
    }

    activatedAbility {
        cost = AbilityCost.Tap
        effect = AddManaEffect(Color.RED)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = AbilityCost.Tap
        effect = AddManaEffect(Color.WHITE)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "247"
        artist = "Eytan Zana"
        imageUri = "https://cards.scryfall.io/normal/front/3/b/3b296781-78ac-411f-88fc-2d924ad22986.jpg?1562785094"
    }
}
