package com.wingedsheep.mtg.sets.definitions.bloomburrow.cards

import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.conditions.WasKicked
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Tender Wildguide
 * {1}{G}
 * Creature — Possum Druid
 * 2/2
 *
 * Offspring {2}
 * {T}: Add one mana of any color.
 * {T}: Put a +1/+1 counter on this creature.
 *
 * Offspring is modeled as Kicker with a conditional ETB token copy.
 * Note: token copy will be 3/3 instead of 1/1 (P/T override not yet supported).
 */
val TenderWildguide = card("Tender Wildguide") {
    manaCost = "{1}{G}"
    typeLine = "Creature — Possum Druid"
    power = 2
    toughness = 2
    oracleText = "Offspring {2} (You may pay an additional {2} as you cast this spell. If you do, when this creature enters, create a 1/1 token copy of it.)\n{T}: Add one mana of any color.\n{T}: Put a +1/+1 counter on this creature."

    // Offspring modeled as Kicker
    keywordAbility(KeywordAbility.Kicker(ManaCost.parse("{2}")))

    // Offspring ETB: create token copy when kicked
    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        triggerCondition = WasKicked
        effect = Effects.CreateTokenCopyOfSelf()
    }

    // {T}: Add one mana of any color
    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddAnyColorMana()
        manaAbility = true
        description = "{T}: Add one mana of any color"
    }

    // {T}: Put a +1/+1 counter on this creature
    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddCounters("PLUS_ONE_PLUS_ONE", 1, EffectTarget.Self)
        description = "{T}: Put a +1/+1 counter on this creature"
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "196"
        artist = "Jakob Eirich"
        imageUri = "https://cards.scryfall.io/normal/front/6/b/6b8bfa91-adb0-4596-8c16-d8bb64fdb26d.jpg?1721426949"

        ruling("2024-07-26", "You can pay an offspring cost only once as you cast a spell with offspring.")
        ruling("2024-07-26", "The token copies exactly what was printed on the original creature and nothing else, except it's a 1/1.")
    }
}
