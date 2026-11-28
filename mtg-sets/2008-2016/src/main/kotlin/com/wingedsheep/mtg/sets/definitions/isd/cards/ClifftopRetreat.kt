package com.wingedsheep.mtg.sets.definitions.isd.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.EntersTapped
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.conditions.Exists
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Clifftop Retreat
 * Land
 * This land enters tapped unless you control a Mountain or a Plains.
 * {T}: Add {R} or {W}.
 */
val ClifftopRetreat = card("Clifftop Retreat") {
    typeLine = "Land"
    colorIdentity = "WR"
    oracleText = "This land enters tapped unless you control a Mountain or a Plains.\n{T}: Add {R} or {W}."

    replacementEffect(EntersTapped(
        unlessCondition = Conditions.Any(
            Exists(Player.You, Zone.BATTLEFIELD, GameObjectFilter.Land.withSubtype("Mountain")),
            Exists(Player.You, Zone.BATTLEFIELD, GameObjectFilter.Land.withSubtype("Plains"))
        )
    ))

    activatedAbility {
        cost = AbilityCost.Tap
        effect = Effects.AddMana(Color.RED)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = AbilityCost.Tap
        effect = Effects.AddMana(Color.WHITE)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    metadata {
        flavorText = "Where cathars learn to fight not only demons and vampires, but ignorance as well."
        rarity = Rarity.RARE
        collectorNumber = "238"
        artist = "John Avon"
        imageUri = "https://cards.scryfall.io/normal/front/f/d/fd7e1bf9-bd6a-48e3-9331-178e5142c06a.jpg"
    }
}
