package com.wingedsheep.mtg.sets.definitions.rna.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Sauroform Hybrid — Ravnica Allegiance #140
 * {1}{G} · Creature — Human Lizard Warrior · 2 / 2
 */
val SauroformHybrid = card("Sauroform Hybrid") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Human Lizard Warrior"
    power = 2
    toughness = 2
    oracleText = "{4}{G}{G}: Adapt 4. (If this creature has no +1/+1 counters on it, put four +1/+1 counters on it.)"

    activatedAbility {
        cost = Costs.Mana("{4}{G}{G}")
        effect = Effects.Adapt(4)
        description = "{4}{G}{G}: Adapt 4."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "140"
        artist = "Nils Hamm"
        flavorText = "\"Within each of us, the potential for great power waits to be released.\"\n—Zija, Simic mutationist"
        imageUri = "https://cards.scryfall.io/normal/front/2/f/2f7ecad5-8bb5-416f-b8ef-a04aba4dc4b5.jpg?1783933664"
    }
}
