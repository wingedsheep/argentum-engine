package com.wingedsheep.mtg.sets.definitions.dft.cards

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
 * Riverpyre Verge
 * Land
 *
 * {T}: Add {R}.
 * {T}: Add {U}. Activate only if you control an Island or a Mountain.
 */
val RiverpyreVerge = card("Riverpyre Verge") {
    typeLine = "Land"
    colorIdentity = "RU"
    oracleText = "{T}: Add {R}.\n{T}: Add {U}. Activate only if you control an Island or a Mountain."

    activatedAbility {
        cost = AbilityCost.Tap
        effect = AddManaEffect(Color.RED)
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
                    Exists(Player.You, Zone.BATTLEFIELD, GameObjectFilter.Land.withSubtype("Island")),
                    Exists(Player.You, Zone.BATTLEFIELD, GameObjectFilter.Land.withSubtype("Mountain"))
                )
            )
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "260"
        artist = "Titus Lunter"
        flavorText = "\"While Loot's ability to predict Omenpaths is extraordinary, I'm more impressed by his unrelenting curiosity.\"\n—Pia Nalaar"
        imageUri = "https://cards.scryfall.io/normal/front/5/7/57a93a71-d77c-417f-85d0-cd420f573331.jpg?1773857340"
    }
}
