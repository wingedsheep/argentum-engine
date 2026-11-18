package com.wingedsheep.mtg.sets.definitions.jou.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Hydra Broodmaster — the becomes-monstrous trigger reads the X of the monstrosity ability that
 * made it monstrous as its own X (CR 701.37c), so both X's in "create X X/X tokens" are the X
 * chosen as the ability was activated.
 */
val HydraBroodmaster = card("Hydra Broodmaster") {
    manaCost = "{4}{G}{G}"
    typeLine = "Creature — Hydra"
    power = 7
    toughness = 7
    oracleText = "{X}{X}{G}: Monstrosity X. (If this creature isn't monstrous, put X +1/+1 counters on it and it becomes monstrous.)\n" +
        "When this creature becomes monstrous, create X X/X green Hydra creature tokens."

    activatedAbility {
        cost = Costs.Mana("{X}{X}{G}")
        effect = Effects.Monstrosity(DynamicAmounts.xValue())
    }

    triggeredAbility {
        trigger = Triggers.self.becomesMonstrous()
        effect = Effects.CreateToken(
            count = DynamicAmounts.xValue(),
            power = 0,
            toughness = 0,
            colors = setOf(Color.GREEN),
            creatureTypes = setOf("Hydra"),
            dynamicPower = DynamicAmounts.xValue(),
            dynamicToughness = DynamicAmounts.xValue(),
            imageUri = "https://cards.scryfall.io/normal/front/9/d/9d1af553-ec9f-46fd-9c6d-49436e93e682.jpg?1783939392"
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "128"
        artist = "Steve Prescott"
        imageUri = "https://cards.scryfall.io/normal/front/6/b/6b465289-676d-4c34-84d3-ce8daf0fd40e.jpg?1783939412"
        ruling("2014-04-26", "The value of each X in Hydra Broodmaster's last ability is equal to the value chosen for X when its monstrosity ability was activated.")
        ruling("2014-04-26", "Once a creature becomes monstrous, it can't become monstrous again. If the creature is already monstrous when the monstrosity ability resolves, nothing happens.")
        ruling("2014-04-26", "Monstrous isn't an ability that a creature has. It's just something true about that creature. If the creature stops being a creature or loses its abilities, it will continue to be monstrous.")
        ruling("2014-04-26", "An ability that triggers when a creature becomes monstrous won't trigger if that creature isn't on the battlefield when its monstrosity ability resolves.")
    }
}
