package com.wingedsheep.mtg.sets.definitions.`5dn`.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Engineered Explosives
 * {X}
 * Artifact
 *
 * Sunburst (This artifact enters with a charge counter on it for each color of mana spent to cast it.)
 * {2}, Sacrifice this artifact: Destroy each nonland permanent with mana value equal to the number of
 * charge counters on this artifact.
 *
 * Sunburst is the bare keyword: the engine places the counters as it enters from the stack. The
 * sacrifice is a cost, so the ability counts the charge counters it had as it last existed on the
 * battlefield (CR 113.7a).
 */
val EngineeredExplosives = card("Engineered Explosives") {
    manaCost = "{X}"
    colorIdentity = ""
    typeLine = "Artifact"
    oracleText = "Sunburst (This artifact enters with a charge counter on it for each color of mana " +
        "spent to cast it.)\n" +
        "{2}, Sacrifice this artifact: Destroy each nonland permanent with mana value equal to the " +
        "number of charge counters on this artifact."
    keywords(Keyword.SUNBURST)

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{2}"), Costs.SacrificeSelf)
        effect = Effects.DestroyAll(
            GameObjectFilter.NonlandPermanent.manaValueEqualsDynamic(
                DynamicAmounts.lastKnownSourceCounters(CounterType.CHARGE)
            )
        )
        description = "{2}, Sacrifice this artifact: Destroy each nonland permanent with mana value " +
            "equal to the number of charge counters on this artifact."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "118"
        artist = "Ron Spears"
        imageUri = "https://cards.scryfall.io/normal/front/8/4/8492a272-e595-4f94-a6eb-08d29f211fd6.jpg?1783944383"
        ruling("2020-08-07", "You can choose any value for X as you cast Engineered Explosives. The value chosen for X doesn't directly affect the number of charge counters Engineered Explosives enters the battlefield with, but it does let you pay more mana and thus spend more colors of mana to cast it.")
        ruling("2020-08-07", "Colorless mana won't give Engineered Explosives another charge counter. Colorless is not a color.")
        ruling("2020-08-07", "Tokens that aren't a copy of something else don't have a mana cost. Anything without a mana cost normally has a mana value of 0.")
        ruling("2020-08-07", "If a permanent has {X} in its mana cost, X is considered to be 0.")
    }
}
