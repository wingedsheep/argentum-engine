package com.wingedsheep.mtg.sets.definitions.atq.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ChoiceType
import com.wingedsheep.sdk.scripting.EntersWithChoice
import com.wingedsheep.sdk.scripting.SetMaximumHandSize
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Cursed Rack
 * {4}
 * Artifact
 *
 * As this artifact enters, choose an opponent.
 * The chosen player's maximum hand size is four.
 *
 * The choose-an-opponent-as-it-enters half reuses the existing
 * [EntersWithChoice]`(ChoiceType.OPPONENT)` replacement effect (same as Jihad), storing the chosen
 * player on the permanent under `ChoiceSlot.OPPONENT`. The new [SetMaximumHandSize] static reads it
 * back via [Player.ChosenOpponent], so the chosen player's maximum hand size becomes 4 — enforced
 * by the cleanup-step discard-down action (CR 402.2 / 514.1), a rule-modifying continuous effect
 * (CR 613.11).
 */
val CursedRack = card("Cursed Rack") {
    manaCost = "{4}"
    colorIdentity = ""
    typeLine = "Artifact"
    oracleText = "As this artifact enters, choose an opponent.\n" +
        "The chosen player's maximum hand size is four."

    replacementEffect(EntersWithChoice(ChoiceType.OPPONENT))

    staticAbility {
        ability = SetMaximumHandSize(player = Player.ChosenOpponent, amount = DynamicAmount.Fixed(4))
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "48"
        artist = "Richard Thomas"
        flavorText = "Ashnod invented several torture techniques that could make victims even miles away beg for mercy as if the End had come."
        imageUri = "https://cards.scryfall.io/normal/front/7/2/720d871d-1e7b-482e-bd1e-8ec79519fb86.jpg?1562918942"
    }
}
