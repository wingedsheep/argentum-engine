package com.wingedsheep.mtg.sets.definitions.tsp.cards

import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Paradox Haze
 * {2}{U}
 * Enchantment — Aura
 *
 * Enchant player
 * At the beginning of enchanted player's first upkeep each turn, that player gets an additional
 * upkeep step after this step.
 *
 * "First upkeep each turn" is [Conditions.IsFirstUpkeepStepOfTurn] as the intervening-if, so the
 * extra upkeep step this adds (directly after this one, CR 500.9 — no untap or draw step between
 * them) doesn't add another. The trigger only fires on the enchanted player's turn, so "that player"
 * is the active player the step is added for.
 */
val ParadoxHaze = card("Paradox Haze") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant player\n" +
        "At the beginning of enchanted player's first upkeep each turn, that player gets an " +
        "additional upkeep step after this step."

    auraTarget = Targets.Player

    triggeredAbility {
        trigger = Triggers.player(Player.EnchantedPlayer).beginningOf(Step.UPKEEP)
        interveningIf = Conditions.IsFirstUpkeepStepOfTurn
        effect = Effects.AddAdditionalUpkeepSteps(1, afterThisStep = true)
        description = "At the beginning of enchanted player's first upkeep each turn, that player " +
            "gets an additional upkeep step after this step."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "71"
        artist = "Greg Staples"
        flavorText = "\"Keep your pace steady through the haze, lest you step on the heels of your " +
            "future self or trip the self a moment behind you.\"\n—Teferi"
        imageUri = "https://cards.scryfall.io/normal/front/a/7/a7c74def-83e5-4420-989d-2304bf4743ae.jpg?1783943242"
    }
}
