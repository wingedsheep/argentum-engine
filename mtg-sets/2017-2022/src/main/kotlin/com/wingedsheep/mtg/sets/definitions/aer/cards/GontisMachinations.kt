package com.wingedsheep.mtg.sets.definitions.aer.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Gonti's Machinations — Aether Revolt #63
 * {B} · Enchantment · Uncommon
 *
 * Whenever you lose life for the first time each turn, you get {E}.
 * Pay {E}{E}, Sacrifice this enchantment: Each opponent loses 3 life. You gain life equal to the
 * life lost this way.
 *
 * - `Triggers.you.losesLife(firstTimeEachTurn = true)` fires on the controller's first life-loss
 *   event of the turn (damage or "loses life"), so at most one {E} per turn.
 * - [Effects.DrainLife] gains the total life *actually* lost, read back from the loss events, so a
 *   life-loss replacement on an opponent shrinks the gain.
 */
val GontisMachinations = card("Gonti's Machinations") {
    manaCost = "{B}"
    colorIdentity = "B"
    typeLine = "Enchantment"
    oracleText = "Whenever you lose life for the first time each turn, you get {E}. " +
        "(You get an energy counter. Damage causes loss of life.)\n" +
        "Pay {E}{E}, Sacrifice this enchantment: Each opponent loses 3 life. " +
        "You gain life equal to the life lost this way."

    triggeredAbility {
        trigger = Triggers.you.losesLife(firstTimeEachTurn = true)
        effect = Effects.GetEnergy(1)
    }

    activatedAbility {
        cost = Costs.Composite(Costs.PayPlayerCounters(CounterType.ENERGY, 2), Costs.SacrificeSelf)
        effect = Effects.DrainLife(3)
        description = "Pay {E}{E}, Sacrifice this enchantment: Each opponent loses 3 life. " +
            "You gain life equal to the life lost this way."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "63"
        artist = "Daarken"
        flavorText = "\"I am confident that all our enterprises can benefit from this little revolution.\""
        imageUri = "https://cards.scryfall.io/normal/front/6/c/6c97f5a9-6e04-45c3-aa8d-dcf0fd81d5b9.jpg?1783936763"
        ruling("2017-02-09", "The triggered ability gives you at most one energy counter each turn, regardless of how much life you lost.")
        ruling(
            "2017-02-09",
            "In a Two-Headed Giant game, damage and life loss happen to individual players, and the result affects " +
                "the team's life total. Combat damage is assigned and dealt to specific players. If your teammate is " +
                "dealt damage or otherwise loses life, Gonti's Machinations doesn't trigger, even though your life " +
                "total went down."
        )
    }
}
