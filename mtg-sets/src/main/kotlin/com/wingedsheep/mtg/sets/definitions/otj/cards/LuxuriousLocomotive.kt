package com.wingedsheep.mtg.sets.definitions.otj.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility

/**
 * Luxurious Locomotive
 * {5}
 * Artifact — Vehicle
 * 6/5
 *
 * Whenever this Vehicle attacks, create a Treasure token for each creature that crewed it
 * this turn. (They're artifacts with "{T}, Sacrifice this token: Add one mana of any color.")
 * Crew 1. Activate only once each turn. (Tap any number of creatures you control with total
 * power 1 or more: This Vehicle becomes an artifact creature until end of turn.)
 *
 * The attack payoff reuses [DynamicAmounts.creaturesThatCrewedOrSaddledThisTurn] (source-relative
 * count off [com.wingedsheep.engine.state.components.battlefield.CrewSaddleContributorsComponent],
 * which the crew handler records and which includes contributors that have since left — per the
 * ruling). "Crew 1. Activate only once each turn." is `crew(1, onceEachTurn = true)`: the
 * once-per-turn cap is enforced in the crew enumerator + handler via the component's crew-activation
 * count (vanilla Crew is uncapped).
 */
val LuxuriousLocomotive = card("Luxurious Locomotive") {
    manaCost = "{5}"
    colorIdentity = ""
    typeLine = "Artifact — Vehicle"
    power = 6
    toughness = 5
    oracleText = "Whenever this Vehicle attacks, create a Treasure token for each creature that " +
        "crewed it this turn. (They're artifacts with \"{T}, Sacrifice this token: Add one mana " +
        "of any color.\")\n" +
        "Crew 1. Activate only once each turn. (Tap any number of creatures you control with total " +
        "power 1 or more: This Vehicle becomes an artifact creature until end of turn.)"

    // Whenever this Vehicle attacks, create a Treasure token for each creature that crewed it this turn.
    triggeredAbility {
        trigger = Triggers.Attacks
        effect = Effects.CreateTreasure(count = DynamicAmounts.creaturesThatCrewedOrSaddledThisTurn())
    }

    keywordAbility(KeywordAbility.crew(1, onceEachTurn = true))

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "244"
        artist = "Leon Tukker"
        imageUri = "https://cards.scryfall.io/normal/front/c/c/cc598338-eeba-4815-a0a6-ff2dc09790d2.jpg?1712356268"

        ruling("2024-04-12", "You may tap more creatures than necessary to activate a crew ability.")
        ruling("2024-04-12", "Once a player announces that they are activating a crew ability, no player may take other actions until the ability has been paid for.")
        ruling("2024-04-12", "Luxurious Locomotive's triggered ability counts each creature that crewed it this turn, including creatures that are no longer on the battlefield as the ability resolves.")
    }
}
