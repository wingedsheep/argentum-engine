package com.wingedsheep.mtg.sets.definitions.neo.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Roadside Reliquary — Kamigawa: Neon Dynasty #272 (canonical printing)
 * Land
 *
 * {T}: Add {C}.
 * {2}, {T}, Sacrifice this land: Draw a card if you control an artifact. Draw a card if you
 * control an enchantment.
 *
 * Both checks happen on resolution, after the land is already sacrificed as a cost, so each is a
 * plain battlefield-existence test over the permanents you still control.
 */
val RoadsideReliquary = card("Roadside Reliquary") {
    typeLine = "Land"
    colorIdentity = ""
    oracleText = "{T}: Add {C}.\n" +
        "{2}, {T}, Sacrifice this land: Draw a card if you control an artifact. Draw a card if you " +
        "control an enchantment."

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddColorlessMana(1)
        manaAbility = true
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{2}"), Costs.Tap, Costs.SacrificeSelf)
        effect = Effects.If(
            condition = Conditions.YouControl(GameObjectFilter.Artifact),
            then = Effects.DrawCards(1),
        ) then Effects.If(
            condition = Conditions.YouControl(GameObjectFilter.Enchantment),
            then = Effects.DrawCards(1),
        )
        description = "{2}, {T}, Sacrifice this land: Draw a card if you control an artifact. " +
            "Draw a card if you control an enchantment."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "272"
        artist = "Robin Olausson"
        imageUri = "https://cards.scryfall.io/normal/front/8/0/8002de90-93fb-48ea-a849-40fdad0aef5a.jpg?1783923816"
    }
}
