package com.wingedsheep.mtg.sets.definitions.som.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Ratchet Bomb
 * {2}
 * Artifact
 *
 * {T}: Put a charge counter on this artifact.
 * {T}, Sacrifice this artifact: Destroy each nonland permanent with mana value equal to the number
 * of charge counters on this artifact.
 *
 * The sacrifice is a cost, so the second ability counts the charge counters the artifact had as it
 * last existed on the battlefield (CR 113.7a) — the same composition as Engineered Explosives.
 * Tokens that aren't copies have mana value 0, so a bomb sacrificed with no counters destroys them.
 */
val RatchetBomb = card("Ratchet Bomb") {
    manaCost = "{2}"
    colorIdentity = ""
    typeLine = "Artifact"
    oracleText = "{T}: Put a charge counter on this artifact.\n" +
        "{T}, Sacrifice this artifact: Destroy each nonland permanent with mana value equal to the " +
        "number of charge counters on this artifact."

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddCounters(CounterType.CHARGE, 1, EffectTarget.Self)
        description = "{T}: Put a charge counter on this artifact."
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Tap, Costs.SacrificeSelf)
        effect = Effects.DestroyAll(
            GameObjectFilter.NonlandPermanent.manaValueEqualsDynamic(
                DynamicAmounts.lastKnownSourceCounters(CounterType.CHARGE)
            )
        )
        description = "{T}, Sacrifice this artifact: Destroy each nonland permanent with mana value " +
            "equal to the number of charge counters on this artifact."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "196"
        artist = "Austin Hsu"
        imageUri = "https://cards.scryfall.io/normal/front/c/3/c3db7645-20b9-4884-849b-a7d4b6d3aa00.jpg?1783941698"
        ruling("2020-08-07", "Tokens that aren't a copy of something else don't have a mana cost. Anything without a mana cost normally has a mana value of 0.")
        ruling("2020-08-07", "If a permanent has {X} in its mana cost, X is considered to be 0.")
    }
}
