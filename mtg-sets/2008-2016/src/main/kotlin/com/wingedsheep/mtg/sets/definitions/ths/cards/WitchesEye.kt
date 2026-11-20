package com.wingedsheep.mtg.sets.definitions.ths.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.grantedActivatedAbility
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantActivatedAbility

/**
 * Witches' Eye
 * {1}
 * Artifact — Equipment
 *
 * Equipped creature has "{1}, {T}: Scry 1."
 * Equip {1}
 *
 * The granted ability lives on the equipped creature, so its {T} taps the creature.
 */
val WitchesEye = card("Witches' Eye") {
    manaCost = "{1}"
    colorIdentity = ""
    typeLine = "Artifact — Equipment"
    oracleText = "Equipped creature has \"{1}, {T}: Scry 1.\" (To scry 1, look at the top card of your " +
        "library, then you may put that card on the bottom.)\n" +
        "Equip {1}"

    staticAbility {
        ability = GrantActivatedAbility(
            ability = grantedActivatedAbility {
                cost = Costs.Composite(Costs.Mana("{1}"), Costs.Tap)
                effect = Effects.Scry(1)
            }
        )
    }

    equipAbility("{1}")

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "222"
        artist = "Daniel Ljunggren"
        flavorText = "The price of prophecy is a vacant eye socket."
        imageUri = "https://cards.scryfall.io/normal/front/6/d/6d989daa-78eb-4120-81b5-7866a7b04590.jpg?1783939715"
    }
}
