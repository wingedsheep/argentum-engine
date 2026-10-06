package com.wingedsheep.mtg.sets.definitions.snc.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Pyre-Sledge Arsonist
 * {2}{R}
 * Creature — Lizard Shaman
 * 2/2
 * {1}, {T}: This creature deals X damage to any target, where X is the number of permanents
 * you've sacrificed this turn.
 */
val PyreSledgeArsonist = card("Pyre-Sledge Arsonist") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Lizard Shaman"
    power = 2
    toughness = 2
    oracleText = "{1}, {T}: This creature deals X damage to any target, where X is the number of " +
        "permanents you've sacrificed this turn."

    // X is read on resolution from the controller-scoped "permanents sacrificed this turn" count.
    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{1}"), Costs.Tap)
        val any = target(Targets.Any)
        effect = Effects.DealDamage(
            DynamicAmounts.permanentsSacrificedThisTurn(),
            any,
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "118"
        artist = "Kekai Kotaki"
        flavorText = "Everything from damaged furniture to inconvenient corpses becomes fuel for " +
            "Ziatora's ever-burning foundry."
        imageUri = "https://cards.scryfall.io/normal/front/3/3/33c5fd3e-3799-4535-8c33-9c567f4f5709.jpg?1783923116"
    }
}
