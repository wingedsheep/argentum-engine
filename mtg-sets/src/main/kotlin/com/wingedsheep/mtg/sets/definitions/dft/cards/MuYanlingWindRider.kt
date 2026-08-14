package com.wingedsheep.mtg.sets.definitions.dft.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EventPattern.OneOrMoreDealCombatDamageToPlayerEvent
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.TriggerBinding
import com.wingedsheep.sdk.scripting.TriggerSpec
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Mu Yanling, Wind Rider — Aetherdrift #52
 * {2}{U}{U} · Legendary Creature — Human Wizard Pilot · 2/4
 *
 * When Mu Yanling enters, create a 3/2 colorless Vehicle artifact token with crew 1.
 * Vehicles you control have flying.
 * Whenever one or more creatures you control with flying deal combat damage to a player, draw a
 * card.
 *
 * Modeling notes:
 *
 *  - **The Vehicle token** is the set's standard 3/2 crew-1 Vehicle, so it's a predefined token
 *    (`PredefinedTokens.Vehicle`) rather than an inline token spec — a *noncreature* artifact with
 *    printed power and toughness, which is what makes the second and third abilities interact:
 *    the token has flying from Mu Yanling immediately, but only counts for the draw trigger once
 *    something crews it into a creature.
 *
 *  - **"Vehicles you control have flying"** is a Layer 6 grant over every Vehicle you control, not
 *    just creature ones — an uncrewed Vehicle is a noncreature artifact, hence a bare subtype
 *    filter rather than `GameObjectFilter.Creature`.
 *
 *  - **The draw is a batch trigger** (CR 603.2c): one card per *damaged player* per combat, no
 *    matter how many fliers connected with them. `OneOrMoreDealCombatDamageToPlayerEvent` already
 *    scopes to sources you control, so the filter only has to say "creature with flying". It's
 *    matched against projected state, so a crewed Vehicle flying courtesy of Mu Yanling herself
 *    counts, and a creature that lost flying before damage doesn't.
 */
val MuYanlingWindRider = card("Mu Yanling, Wind Rider") {
    manaCost = "{2}{U}{U}"
    colorIdentity = "U"
    typeLine = "Legendary Creature — Human Wizard Pilot"
    oracleText = "When Mu Yanling enters, create a 3/2 colorless Vehicle artifact token with crew 1.\n" +
        "Vehicles you control have flying.\n" +
        "Whenever one or more creatures you control with flying deal combat damage to a player, " +
        "draw a card."
    power = 2
    toughness = 4

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = Effects.CreateVehicleToken()
        description = "When Mu Yanling enters, create a 3/2 colorless Vehicle artifact token with " +
            "crew 1."
    }

    staticAbility {
        ability = GrantKeyword(
            Keyword.FLYING,
            GroupFilter(GameObjectFilter.Any.withSubtype(Subtype.VEHICLE).youControl())
        )
    }

    triggeredAbility {
        trigger = TriggerSpec(
            OneOrMoreDealCombatDamageToPlayerEvent(
                sourceFilter = GameObjectFilter.Creature.withKeyword(Keyword.FLYING)
            ),
            TriggerBinding.ANY
        )
        effect = Effects.DrawCards(1)
        description = "Whenever one or more creatures you control with flying deal combat damage " +
            "to a player, draw a card."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "52"
        artist = "Justyna Dura"
        flavorText = "The search for her lost mentor continued."
        imageUri = "https://cards.scryfall.io/normal/front/7/6/76423446-d62f-4cc5-a23a-3175be88bd73.jpg?1783907905"
    }
}
