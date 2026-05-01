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
 * Gloomlake Verge
 * Land
 *
 * {T}: Add {U}.
 * {T}: Add {B}. Activate only if you control an Island or a Swamp.
 */
val GloomlakeVerge = card("Gloomlake Verge") {
    typeLine = "Land"
    oracleText = "{T}: Add {U}.\n{T}: Add {B}. Activate only if you control an Island or a Swamp."

    activatedAbility {
        cost = AbilityCost.Tap
        effect = AddManaEffect(Color.BLUE)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = AbilityCost.Tap
        effect = AddManaEffect(Color.BLACK)
        manaAbility = true
        timing = TimingRule.ManaAbility
        restrictions = listOf(
            ActivationRestriction.OnlyIfCondition(
                Conditions.Any(
                    Exists(Player.You, Zone.BATTLEFIELD, GameObjectFilter.Land.withSubtype("Island")),
                    Exists(Player.You, Zone.BATTLEFIELD, GameObjectFilter.Land.withSubtype("Swamp"))
                )
            )
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "260"
        artist = "Marco Gorlei"
        flavorText = "Here, the Floodpits languish like a corpse, the clear waters choked by the filthy hands of the bog."
        imageUri = "https://cards.scryfall.io/normal/front/8/3/83f510b7-4cbd-4883-9c26-c8824bc668ac.jpg?1773857340"
    }
}
