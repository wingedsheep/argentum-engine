package com.wingedsheep.mtg.sets.definitions.ody.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AbilityId
import com.wingedsheep.sdk.scripting.ActivatedAbility
import com.wingedsheep.sdk.scripting.GrantActivatedAbility
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Squirrel Nest
 * {1}{G}{G}
 * Enchantment — Aura
 * Enchant land
 * Enchanted land has "{T}: Create a 1/1 green Squirrel creature token."
 *
 * The granted ability belongs to the enchanted land (it is the land that taps), so it is a
 * [GrantActivatedAbility] onto the attached permanent — the New Horizons shape, without the
 * mana-ability flag since creating a token is not a mana ability.
 */
val SquirrelNest = card("Squirrel Nest") {
    manaCost = "{1}{G}{G}"
    colorIdentity = "G"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant land\n" +
        "Enchanted land has \"{T}: Create a 1/1 green Squirrel creature token.\""

    auraTarget = TargetObject(filter = TargetFilter.Land)

    staticAbility {
        ability = GrantActivatedAbility(
            ability = ActivatedAbility(
                id = AbilityId.next(),
                cost = Costs.Tap,
                effect = Effects.CreateToken(
                    power = 1,
                    toughness = 1,
                    colors = setOf(Color.GREEN),
                    creatureTypes = setOf("Squirrel")
                )
            )
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "274"
        artist = "Anthony S. Waters"
        flavorText = "In the Krosan Forest, you've got to keep an eye out for the squirrels."
        imageUri = "https://cards.scryfall.io/normal/front/2/2/22eccb27-1723-4c5a-96b8-85e6e5739c30.jpg?1783945210"
    }
}
