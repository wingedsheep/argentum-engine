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
 * Isolated Chapel
 * Land
 * This land enters tapped unless you control a Plains or a Swamp.
 * {T}: Add {W} or {B}.
 */
val IsolatedChapel = card("Isolated Chapel") {
    typeLine = "Land"
    colorIdentity = "WB"
    oracleText = "This land enters tapped unless you control a Plains or a Swamp.\n{T}: Add {W} or {B}."

    replacementEffect(EntersTapped(
        unlessCondition = Conditions.Any(
            Exists(Player.You, Zone.BATTLEFIELD, GameObjectFilter.Land.withSubtype("Plains")),
            Exists(Player.You, Zone.BATTLEFIELD, GameObjectFilter.Land.withSubtype("Swamp"))
        )
    ))

    activatedAbility {
        cost = AbilityCost.Tap
        effect = Effects.AddMana(Color.WHITE)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = AbilityCost.Tap
        effect = Effects.AddMana(Color.BLACK)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    metadata {
        flavorText = "Not every church is a place of faith."
        rarity = Rarity.RARE
        collectorNumber = "242"
        artist = "Cliff Childs"
        imageUri = "https://cards.scryfall.io/normal/front/b/3/b3c1a371-5ded-4a3a-bf96-503c4f1a665d.jpg"
    }
}
