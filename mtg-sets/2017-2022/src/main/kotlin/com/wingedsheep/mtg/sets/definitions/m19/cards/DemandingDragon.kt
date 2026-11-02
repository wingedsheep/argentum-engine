package com.wingedsheep.mtg.sets.definitions.m19.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Demanding Dragon — Core Set 2019 #135
 * {3}{R}{R} · Creature — Dragon · 5/5
 * Flying
 * When this creature enters, it deals 5 damage to target opponent unless that player sacrifices a
 * creature of their choice.
 *
 * Mogis's punisher ([Effects.PayOrSuffer] over a creature sacrifice) offered to the targeted
 * opponent, Tergrid's Lantern style: the target is both the payer and the damage recipient. An
 * opponent who controls no creature can't pay and takes the 5 damage.
 */
val DemandingDragon = card("Demanding Dragon") {
    manaCost = "{3}{R}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Dragon"
    power = 5
    toughness = 5
    oracleText = "Flying\n" +
        "When this creature enters, it deals 5 damage to target opponent unless that player " +
        "sacrifices a creature of their choice."

    keywords(Keyword.FLYING)

    triggeredAbility {
        val opponent = target(Targets.Opponent)
        trigger = Triggers.self.enters()
        effect = Effects.PayOrSuffer(
            cost = Costs.pay.Sacrifice(GameObjectFilter.Creature),
            suffer = Effects.DealDamage(5, opponent),
            player = opponent
        )
        description = "When this creature enters, it deals 5 damage to target opponent unless " +
            "that player sacrifices a creature of their choice."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "135"
        artist = "Chris Rahn"
        flavorText = "A knight's devotion to their steed goes only so far."
        imageUri = "https://cards.scryfall.io/normal/front/3/1/31b73282-6207-4207-8b9c-86a8f9a263a2.jpg?1783934556"
    }
}
