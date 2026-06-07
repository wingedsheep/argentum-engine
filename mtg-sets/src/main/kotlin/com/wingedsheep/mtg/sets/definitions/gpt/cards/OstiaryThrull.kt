package com.wingedsheep.mtg.sets.definitions.gpt.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Ostiary Thrull
 * {3}{B}
 * Creature — Thrull
 * 2/2
 * {W}, {T}: Tap target creature.
 */
val OstiaryThrull = card("Ostiary Thrull") {
    manaCost = "{3}{B}"
    colorIdentity = "WB"
    typeLine = "Creature — Thrull"
    power = 2
    toughness = 2
    oracleText = "{W}, {T}: Tap target creature."

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{W}"), Costs.Tap)
        val t = target("creature", Targets.Creature)
        effect = Effects.Tap(t)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "55"
        artist = "Ron Spencer"
        flavorText = "Orzhov churches don't pass the plate for collection. They charge for admission."
        imageUri = "https://cards.scryfall.io/normal/front/2/6/26407330-d7a1-4915-b7c6-e08e166cf638.jpg?1593272233"
    }
}
