package com.wingedsheep.mtg.sets.definitions.snc.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Glittering Stockpile
 * {2}{R}
 * Artifact — Treasure
 * {T}: Add {R}. Put a stash counter on this artifact.
 * {T}, Sacrifice this artifact: Add X mana of any one color, where X is the number of stash
 * counters on this artifact.
 *
 * Both abilities are mana abilities. The second sacrifices the Stockpile as a cost, so X reads the
 * stash counters off its last-known information (CR 113.7a) — the mana-ability resolver hands the
 * effect the source's pre-activation snapshot.
 */
val GlitteringStockpile = card("Glittering Stockpile") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Artifact — Treasure"
    oracleText = "{T}: Add {R}. Put a stash counter on this artifact.\n" +
        "{T}, Sacrifice this artifact: Add X mana of any one color, where X is the number of stash counters on this artifact."

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddMana(Color.RED) then
            Effects.AddCounters(CounterType.STASH, 1, EffectTarget.Self)
        manaAbility = true
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Tap, Costs.SacrificeSelf)
        effect = Effects.AddAnyColorMana(DynamicAmounts.countersOnSelf(CounterType.STASH))
        manaAbility = true
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "107"
        artist = "Brock Grossman"
        flavorText = "The only thing Ziatora desires is \"more.\""
        imageUri = "https://cards.scryfall.io/normal/front/9/7/973685e8-3df1-436b-b5c2-01573a92b61e.jpg"
    }
}
