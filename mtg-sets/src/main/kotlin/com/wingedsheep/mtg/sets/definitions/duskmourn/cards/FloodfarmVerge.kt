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
 * Floodfarm Verge
 * Land
 *
 * {T}: Add {W}.
 * {T}: Add {U}. Activate only if you control a Plains or an Island.
 */
val FloodfarmVerge = card("Floodfarm Verge") {
    typeLine = "Land"
    oracleText = "{T}: Add {W}.\n{T}: Add {U}. Activate only if you control a Plains or an Island."

    activatedAbility {
        cost = AbilityCost.Tap
        effect = AddManaEffect(Color.WHITE)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = AbilityCost.Tap
        effect = AddManaEffect(Color.BLUE)
        manaAbility = true
        timing = TimingRule.ManaAbility
        restrictions = listOf(
            ActivationRestriction.OnlyIfCondition(
                Conditions.Any(
                    Exists(Player.You, Zone.BATTLEFIELD, GameObjectFilter.Land.withSubtype("Plains")),
                    Exists(Player.You, Zone.BATTLEFIELD, GameObjectFilter.Land.withSubtype("Island"))
                )
            )
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "259"
        artist = "Randy Gallegos"
        flavorText = "Here, the Mistmoors slump like a grieving parent, the once-thriving fields drowned by the relentless deluge."
        imageUri = "https://cards.scryfall.io/normal/front/d/5/d53ed0db-1199-44b3-8eda-8189dfcf53d1.jpg?1773857337"
    }
}
