package com.wingedsheep.mtg.sets.definitions.ddf.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Contagion Clasp
 * {2}
 * Artifact
 *
 * When this artifact enters, put a -1/-1 counter on target creature.
 * {4}, {T}: Proliferate.
 *
 * Canonical printing is Duel Decks: Elspeth vs. Tezzeret (earliest real printing).
 */
val ContagionClasp = card("Contagion Clasp") {
    manaCost = "{2}"
    typeLine = "Artifact"
    oracleText = "When this artifact enters, put a -1/-1 counter on target creature.\n" +
        "{4}, {T}: Proliferate. (Choose any number of permanents and/or players, then give each another " +
        "counter of each kind already there.)"

    triggeredAbility {
        trigger = Triggers.self.enters()
        val creature = target(TargetFilter.Creature)
        effect = Effects.AddCounters(CounterType.MINUS_ONE_MINUS_ONE, 1, creature)
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{4}"), Costs.Tap)
        effect = Effects.Proliferate()
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "63"
        artist = "Anthony Palumbo"
        imageUri = "https://cards.scryfall.io/normal/front/d/1/d134d21d-4a18-4fbe-a6c2-428e99a86466.jpg?1783941755"
    }
}
