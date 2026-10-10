package com.wingedsheep.mtg.sets.definitions.fin.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Namazu Trader
 * {3}{B}
 * Creature — Fish Citizen
 * 3/4
 *
 * When this creature enters, you lose 1 life and create a Treasure token.
 * Whenever this creature attacks, you may sacrifice another creature or artifact.
 * If you do, surveil 2.
 */
val NamazuTrader = card("Namazu Trader") {
    manaCost = "{3}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Fish Citizen"
    power = 3
    toughness = 4
    oracleText = "When this creature enters, you lose 1 life and create a Treasure token.\n" +
        "Whenever this creature attacks, you may sacrifice another creature or artifact. If you do, surveil 2. " +
        "(Look at the top two cards of your library, then put any number of them into your graveyard and the rest on top of your library in any order.)"

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.LoseLife(1, EffectTarget.Controller) then Effects.CreateTreasure(1)
    }

    triggeredAbility {
        trigger = Triggers.self.attacks()
        effect = Effects.MayPay(
            Effects.SacrificeOwn(GameObjectFilter.Creature.or(GameObjectFilter.Artifact), excludeSource = true),
            then = Patterns.Library.surveil(2)
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "107"
        artist = "Andrea Tentori Montalto"
        imageUri = "https://cards.scryfall.io/normal/front/f/9/f9d25b34-990d-416c-aef7-1b5a73f19dd4.jpg?1748962390"
    }
}
