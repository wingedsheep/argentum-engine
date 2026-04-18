package com.wingedsheep.mtg.sets.definitions.lorwyneclipsed.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.EffectPatterns
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AdditionalCost
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantMayCastFromLinkedExile
import com.wingedsheep.sdk.scripting.effects.MoveToZoneEffect

/**
 * Dawnhand Dissident
 * {B}
 * Creature — Elf Warlock
 * 1/2
 *
 * {T}, Blight 1: Surveil 1.
 * {T}, Blight 2: Exile target card from a graveyard.
 * During your turn, you may cast creature spells from among cards you own exiled
 * with this creature by removing three counters from among creatures you control
 * in addition to paying their other costs.
 */
val DawnhandDissident = card("Dawnhand Dissident") {
    manaCost = "{B}"
    typeLine = "Creature — Elf Warlock"
    power = 1
    toughness = 2
    oracleText = "{T}, Blight 1: Surveil 1.\n" +
        "{T}, Blight 2: Exile target card from a graveyard.\n" +
        "During your turn, you may cast creature spells from among cards you own exiled " +
        "with this creature by removing three counters from among creatures you control " +
        "in addition to paying their other costs."

    // {T}, Blight 1: Surveil 1.
    activatedAbility {
        cost = Costs.Composite(Costs.Tap, Costs.Blight(1))
        effect = EffectPatterns.surveil(1)
    }

    // {T}, Blight 2: Exile target card from a graveyard, linked to this creature.
    activatedAbility {
        cost = Costs.Composite(Costs.Tap, Costs.Blight(2))
        val graveyardTarget = target("target card in a graveyard", Targets.CardInGraveyard)
        effect = MoveToZoneEffect(
            target = graveyardTarget,
            destination = Zone.EXILE,
            linkToSource = true
        )
    }

    // Static: during your turn, you may cast creature spells from cards exiled with
    // this permanent, paying an additional 3 counters from among your creatures.
    staticAbility {
        ability = GrantMayCastFromLinkedExile(
            filter = GameObjectFilter.Creature,
            duringYourTurnOnly = true,
            additionalCost = AdditionalCost.RemoveCountersFromYourCreatures(totalCount = 3),
            ownedByYou = true
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "98"
        artist = "Jacob Walker"
        imageUri = "https://cards.scryfall.io/normal/front/6/a/6ac1f765-f348-4813-88dc-26376e0f3f33.jpg?1767659632"
    }
}
