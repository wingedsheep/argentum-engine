package com.wingedsheep.mtg.sets.definitions.duskmourn.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.ActivationRestriction
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.conditions.Exists
import com.wingedsheep.sdk.scripting.effects.AddManaEffect
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Blazemire Verge
 * Land
 *
 * {T}: Add {B}.
 * {T}: Add {R}. Activate only if you control a Swamp or a Mountain.
 */
val BlazemireVerge = card("Blazemire Verge") {
    typeLine = "Land"
    oracleText = "{T}: Add {B}.\n{T}: Add {R}. Activate only if you control a Swamp or a Mountain."

    activatedAbility {
        cost = AbilityCost.Tap
        effect = AddManaEffect(Color.BLACK)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = AbilityCost.Tap
        effect = AddManaEffect(Color.RED)
        manaAbility = true
        timing = TimingRule.ManaAbility
        restrictions = listOf(
            ActivationRestriction.OnlyIfCondition(
                Conditions.Any(
                    Exists(Player.You, Zone.BATTLEFIELD, GameObjectFilter.Land.withSubtype("Swamp")),
                    Exists(Player.You, Zone.BATTLEFIELD, GameObjectFilter.Land.withSubtype("Mountain"))
                )
            )
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "256"
        artist = "Andrew Mar"
        flavorText = "Here, the Balemurk spits and boils, the oppressive darkness pierced by scorching spears of fire."
        imageUri = "https://cards.scryfall.io/normal/front/d/1/d151c8e2-d715-470d-868a-f45191db9fa0.jpg?1773857320"
    }
}
