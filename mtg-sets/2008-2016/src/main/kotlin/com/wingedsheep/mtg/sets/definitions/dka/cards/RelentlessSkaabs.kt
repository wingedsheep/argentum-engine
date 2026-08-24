package com.wingedsheep.mtg.sets.definitions.dka.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Relentless Skaabs
 * {3}{U}{U}
 * Creature — Zombie
 * 4/4
 * As an additional cost to cast this spell, exile a creature card from your graveyard.
 * Undying (When this creature dies, if it had no +1/+1 counters on it, return it to the
 * battlefield under its owner's control with a +1/+1 counter on it.)
 *
 * [StitchedDrake]'s additional cost plus [YoungWolf]'s keyword — the additional cost rides the
 * *cast*, so the undying return (not a cast, CR 702.92a) exiles nothing, which is exactly the
 * printed ruling.
 */
val RelentlessSkaabs = card("Relentless Skaabs") {
    manaCost = "{3}{U}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Zombie"
    oracleText = "As an additional cost to cast this spell, exile a creature card from your graveyard.\n" +
        "Undying (When this creature dies, if it had no +1/+1 counters on it, return it to the " +
        "battlefield under its owner's control with a +1/+1 counter on it.)"
    power = 4
    toughness = 4

    additionalCost(Costs.additional.ExileCards(count = 1, filter = GameObjectFilter.Creature))

    keywords(Keyword.UNDYING)

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "45"
        artist = "Karl Kopinski"
        imageUri = "https://cards.scryfall.io/normal/front/b/3/b3304cab-0dc9-47e4-ac68-00974b64f5a0.jpg?1783940838"
        ruling("2013-04-15", "You must exile exactly one creature card from your graveyard to cast this spell; you cannot cast it without exiling a creature card, and you cannot exile additional creature cards.")
        ruling("2013-04-15", "Players can only respond once this spell has been cast and all its costs have been paid. No one can try to otherwise remove the creature card you exiled in order to prevent you from casting this spell.")
        ruling("2011-01-22", "When Relentless Skaabs returns to the battlefield because of its undying ability, it's not being cast. You won't exile a creature card from your graveyard.")
    }
}
