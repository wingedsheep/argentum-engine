package com.wingedsheep.mtg.sets.definitions.hou.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ActivationRestriction

/**
 * Wall of Forgotten Pharaohs — Hour of Devastation #168 (canonical printing)
 * {2} · Artifact Creature — Wall · 0/4
 *
 * Defender
 * {T}: This creature deals 1 damage to target player or planeswalker. Activate only if you
 * control a Desert or there is a Desert card in your graveyard.
 */
val WallOfForgottenPharaohs = card("Wall of Forgotten Pharaohs") {
    manaCost = "{2}"
    colorIdentity = ""
    typeLine = "Artifact Creature — Wall"
    power = 0
    toughness = 4
    oracleText = "Defender\n{T}: This creature deals 1 damage to target player or planeswalker. " +
        "Activate only if you control a Desert or there is a Desert card in your graveyard."

    keywords(Keyword.DEFENDER)

    activatedAbility {
        cost = Costs.Tap
        val victim = target(Targets.PlayerOrPlaneswalker)
        effect = Effects.DealDamage(1, victim)
        restrictions = listOf(
            ActivationRestriction.OnlyIfCondition(
                Conditions.Any(
                    Conditions.ControlPermanentOfType(Subtype.DESERT),
                    Conditions.GraveyardContainsSubtype(Subtype.DESERT),
                )
            )
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "168"
        artist = "Igor Kieryluk"
        imageUri = "https://cards.scryfall.io/normal/front/d/3/d3f8f211-79f9-4d34-8e23-c0835703cd91.jpg?1783935999"
        ruling("2017-07-14", "If an ability checks whether you control a Desert or there is a Desert card in your graveyard, having more than one doesn't matter. Controlling one is the same as controlling five. There is also no extra bonus for both controlling one and having one in your graveyard.")
    }
}
