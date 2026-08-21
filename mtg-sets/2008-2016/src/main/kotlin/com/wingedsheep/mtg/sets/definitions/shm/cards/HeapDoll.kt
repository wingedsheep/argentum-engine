package com.wingedsheep.mtg.sets.definitions.shm.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Heap Doll
 * {1}
 * Artifact Creature — Scarecrow
 * 1 / 1
 *
 * Sacrifice this creature: Exile target card from a graveyard.
 *
 * - The sacrifice is a *cost*, so it is paid on activation and the ability resolves even if the
 *   Doll is somehow gone — the target card still gets exiled.
 * - [Targets.CardInGraveyard] is any card in any graveyard, not just an opponent's: the printed
 *   wording is "a graveyard", so one of your own cards is a legal target too.
 * - No `fromZone` gate on the exile. The target is already zone-scoped to the graveyard by the
 *   target requirement, and CR 608.2b fizzles the ability if the card has moved on.
 */
val HeapDoll = card("Heap Doll") {
    manaCost = "{1}"
    typeLine = "Artifact Creature — Scarecrow"
    power = 1
    toughness = 1
    oracleText = "Sacrifice this creature: Exile target card from a graveyard."

    // Sacrifice this creature: Exile target card from a graveyard.
    activatedAbility {
        cost = Costs.SacrificeSelf
        val t = target("target", Targets.CardInGraveyard)
        effect = Effects.Exile(t)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "253"
        artist = "John Avon"
        flavorText = "\"I know one night it won't come back. Then I'll know it's truly done its job.\"\n" +
            "—Braenna, cobblesmith"
        imageUri = "https://cards.scryfall.io/normal/front/8/6/86d726e1-a363-4d77-97d4-e8c94f93fd51.jpg?1783942712"
    }
}
