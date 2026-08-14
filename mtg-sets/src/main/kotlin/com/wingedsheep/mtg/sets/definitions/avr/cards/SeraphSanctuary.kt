package com.wingedsheep.mtg.sets.definitions.avr.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.TriggerBinding

/**
 * Seraph Sanctuary
 * Land
 * When this land enters, you gain 1 life.
 * Whenever an Angel you control enters, you gain 1 life.
 * {T}: Add {C}.
 */
val SeraphSanctuary = card("Seraph Sanctuary") {
    manaCost = ""
    colorIdentity = ""
    typeLine = "Land"
    oracleText =
        "When this land enters, you gain 1 life.\n" +
            "Whenever an Angel you control enters, you gain 1 life.\n" +
            "{T}: Add {C}."

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = Effects.GainLife(1)
    }

    triggeredAbility {
        trigger = Triggers.entersBattlefield(
            filter = GameObjectFilter.Creature.withSubtype(Subtype.ANGEL).youControl(),
            binding = TriggerBinding.ANY,
        )
        effect = Effects.GainLife(1)
    }

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddColorlessMana(1)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "228"
        artist = "David Palumbo"
        imageUri =
            "https://cards.scryfall.io/normal/front/f/9/f903b04a-2733-4ce7-9d83-9db8d5e1e10d.jpg?1783940648"
    }
}
