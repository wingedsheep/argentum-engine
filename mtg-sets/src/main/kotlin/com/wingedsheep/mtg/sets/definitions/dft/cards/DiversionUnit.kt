package com.wingedsheep.mtg.sets.definitions.dft.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Diversion Unit — Aetherdrift #41
 * {1}{U} · Artifact Creature — Robot · 2/1
 *
 * Flying
 * {U}, Sacrifice this creature: Counter target instant or sorcery spell unless its controller
 * pays {3}.
 *
 * A plain Force Spike stapled to a flier: [Targets.InstantOrSorcerySpell] restricts the stack
 * target (either player's spell — the text says "its controller", not "you"), and
 * [Effects.CounterUnlessPays] models the "unless … pays {3}" tax.
 */
val DiversionUnit = card("Diversion Unit") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Artifact Creature — Robot"
    oracleText = "Flying\n{U}, Sacrifice this creature: Counter target instant or sorcery spell " +
        "unless its controller pays {3}."
    power = 2
    toughness = 1

    keywords(Keyword.FLYING)

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{U}"), Costs.SacrificeSelf)
        target = Targets.InstantOrSorcerySpell
        effect = Effects.CounterUnlessPays("{3}")
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "41"
        artist = "Xabi Gaztelua"
        flavorText = "\"Declarative: Goodbye.\""
        imageUri = "https://cards.scryfall.io/normal/front/e/0/e04d4fa6-1fa3-4bfd-a462-47c23ccf9124.jpg?1783907910"
    }
}
