package com.wingedsheep.mtg.sets.definitions.dominaria.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.EntersTapped
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.conditions.Exists
import com.wingedsheep.sdk.scripting.effects.AddManaEffect
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Hinterland Harbor
 * Land
 * This land enters tapped unless you control a Forest or an Island.
 * {T}: Add {G} or {U}.
 */
val HinterlandHarbor = card("Hinterland Harbor") {
    typeLine = "Land"
    oracleText = "This land enters tapped unless you control a Forest or an Island.\n{T}: Add {G} or {U}."

    replacementEffect(EntersTapped(
        unlessCondition = Conditions.Any(
            Exists(Player.You, Zone.BATTLEFIELD, GameObjectFilter.Land.withSubtype("Forest")),
            Exists(Player.You, Zone.BATTLEFIELD, GameObjectFilter.Land.withSubtype("Island"))
        )
    ))

    activatedAbility {
        cost = AbilityCost.Tap
        effect = AddManaEffect(Color.GREEN)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = AbilityCost.Tap
        effect = AddManaEffect(Color.BLUE)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "240"
        artist = "Daniel Ljunggren"
        flavorText = "\"Our ancestors brought down a Phyrexian portal ship, then built our town on its hull. We're pretty proud of that.\" —Alene of Riverspan"
        imageUri = "https://cards.scryfall.io/normal/front/d/6/d631d040-51e9-4540-91e7-aeb5ade84090.jpg?1562743677"
    }
}
