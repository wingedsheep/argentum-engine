package com.wingedsheep.mtg.sets.definitions.mh3.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Retrofitted Transmogrant
 * {B}
 * Artifact Creature — Zombie
 * 1/1
 * {3}{B}: Return this card from your graveyard to the battlefield tapped with two +1/+1 counters on it.
 */
val RetrofittedTransmogrant = card("Retrofitted Transmogrant") {
    manaCost = "{B}"
    colorIdentity = "B"
    typeLine = "Artifact Creature — Zombie"
    power = 1
    toughness = 1
    oracleText = "{3}{B}: Return this card from your graveyard to the battlefield tapped with two +1/+1 counters on it."

    activatedAbility {
        cost = Costs.Mana("{3}{B}")
        effect = Effects.PutOntoBattlefieldFromGraveyard(EffectTarget.Self, tapped = true) then
            Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 2, EffectTarget.Self)
        activateFromZone = Zone.GRAVEYARD
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "106"
        artist = "Kekai Kotaki"
        flavorText = "\"Repair it again! I need reinforcements by tomorrow.\"\n—Ashnod"
        imageUri = "https://cards.scryfall.io/normal/front/1/2/12c1b83d-710b-4680-855a-02ba1f72abf0.jpg?1783911276"
    }
}
