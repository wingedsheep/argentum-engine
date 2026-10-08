package com.wingedsheep.mtg.sets.definitions.chk.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Hearth Kami
 * {1}{R}
 * Creature — Spirit
 * 2/1
 *
 * {X}, Sacrifice this creature: Destroy target artifact with mana value X.
 *
 * The X paid for the ability threads into the *target filter* rather than into the effect:
 * `manaValueEqualsX()` lowers to a mana-value-equals-X comparison, which the target enumerator reads
 * off the X chosen for this activation, so activating for X=3 can only ever target a mana-value-3
 * artifact. That is the whole card — the destroy itself is the plain [Effects.Destroy].
 */
val HearthKami = card("Hearth Kami") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Spirit"
    power = 2
    toughness = 1
    oracleText = "{X}, Sacrifice this creature: Destroy target artifact with mana value X."

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{X}"), Costs.SacrificeSelf)
        val t = target(TargetFilter(GameObjectFilter.Artifact.manaValueEqualsX()))
        effect = Effects.Destroy(t)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "171"
        artist = "Luca Zontini"
        flavorText = "\"Every treachery, great or small, begets a spirit that rages at the " +
            "injustice. Given the opportunity, each will return that treachery to its owner " +
            "tenfold.\"\n—Sensei Hisoka"
        imageUri = "https://cards.scryfall.io/normal/front/d/7/d7361289-5111-49c2-a786-b7181384596b.jpg?1783944300"
    }
}
