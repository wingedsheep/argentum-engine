package com.wingedsheep.mtg.sets.definitions.blb.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.GrantCastCreaturesFromGraveyardWithForageEffect

/**
 * Osteomancer Adept
 * {1}{B}
 * Creature — Squirrel Warlock
 * 2/2
 *
 * Deathtouch
 *
 * {T}: Until end of turn, you may cast creature spells from your graveyard
 * by foraging in addition to paying their other costs. If you cast a spell
 * this way, that creature enters with a finality counter on it.
 *
 * Engine support:
 * - Finality counter death replacement: implemented in ZoneMovementUtils
 * - Forage additional cost: Costs.additional.Forage + CostHandler validation
 * - Graveyard casting permission: MayCastCreaturesFromGraveyardWithForageComponent
 * - Legal action enumeration: CastFromZoneEnumerator.enumerateGraveyardCreaturesWithForage
 *
 * Note: Forage cost payment (exile 3 or sacrifice Food) during cast is validated
 * but the finality counter application on ETB for forage-cast creatures is not
 * yet wired (needs CastSpellHandler + StackResolver changes to track and apply).
 */
val OsteomancerAdept = card("Osteomancer Adept") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Squirrel Warlock"
    power = 2
    toughness = 2
    oracleText = "Deathtouch\n{T}: Until end of turn, you may cast creature spells from your graveyard by foraging in addition to paying their other costs. If you cast a spell this way, that creature enters with a finality counter on it."

    keywords(Keyword.DEATHTOUCH)

    activatedAbility {
        cost = Costs.Tap
        effect = GrantCastCreaturesFromGraveyardWithForageEffect()
        description = "{T}: Until end of turn, cast creatures from graveyard by foraging (finality counter)"
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "103"
        artist = "Daniel Zrom"
        imageUri = "https://cards.scryfall.io/normal/front/7/d/7d8238dd-858f-466c-96de-986bd66861d7.jpg?1721426463"

        ruling("2024-07-26", "Finality counters don't stop permanents from going to zones other than the graveyard from the battlefield.")
        ruling("2024-07-26", "You must still follow timing restrictions and permissions for creature spells you cast with the permission granted by Osteomancer Adept's last ability.")
        ruling("2024-07-26", "Multiple finality counters on a single permanent are redundant.")
    }
}
