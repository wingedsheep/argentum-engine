package com.wingedsheep.mtg.sets.definitions.otj.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.effects.AddCountersEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Bandit's Haul
 * {3}
 * Artifact
 *
 * Whenever you commit a crime, put a loot counter on this artifact. This ability triggers only once
 * each turn. (Targeting opponents, anything they control, and/or cards in their graveyards is a
 * crime.)
 * {T}: Add one mana of any color.
 * {2}, {T}, Remove two loot counters from this artifact: Draw a card.
 */
val BanditsHaul = card("Bandit's Haul") {
    manaCost = "{3}"
    colorIdentity = ""
    typeLine = "Artifact"
    oracleText = "Whenever you commit a crime, put a loot counter on this artifact. This ability " +
        "triggers only once each turn. (Targeting opponents, anything they control, and/or cards " +
        "in their graveyards is a crime.)\n" +
        "{T}: Add one mana of any color.\n" +
        "{2}, {T}, Remove two loot counters from this artifact: Draw a card."

    triggeredAbility {
        trigger = Triggers.YouCommitCrime
        oncePerTurn = true
        effect = AddCountersEffect(
            counterType = Counters.LOOT,
            count = 1,
            target = EffectTarget.Self
        )
        description = "Whenever you commit a crime, put a loot counter on this artifact. " +
            "This ability triggers only once each turn."
    }

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddAnyColorMana(1)
        manaAbility = true
        timing = TimingRule.ManaAbility
        description = "{T}: Add one mana of any color."
    }

    activatedAbility {
        cost = Costs.Composite(
            Costs.Mana("{2}"),
            Costs.Tap,
            Costs.RemoveCounterFromSelf(Counters.LOOT, 2)
        )
        effect = Effects.DrawCards(1)
        description = "{2}, {T}, Remove two loot counters from this artifact: Draw a card."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "240"
        artist = "Monztre"
        imageUri = "https://cards.scryfall.io/normal/front/6/8/68b2e74b-933b-4285-963b-dda3a986a914.jpg?1712356248"
    }
}
