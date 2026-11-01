package com.wingedsheep.mtg.sets.definitions.war.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Emergence Zone — War of the Spark #245
 * Land
 *
 * {T}: Add {C}.
 * {1}, {T}, Sacrifice this land: You may cast spells this turn as though they had flash.
 *
 * The flash grant is Borne Upon a Wind's turn-scoped [Effects.GrantFlashToSpells] on the
 * controller.
 */
val EmergenceZone = card("Emergence Zone") {
    colorIdentity = ""
    typeLine = "Land"
    oracleText = "{T}: Add {C}.\n" +
        "{1}, {T}, Sacrifice this land: You may cast spells this turn as though they had flash."

    activatedAbility {
        cost = Costs.Tap
        manaAbility = true
        effect = Effects.AddColorlessMana(1)
        description = "{T}: Add {C}."
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{1}"), Costs.Tap, Costs.SacrificeSelf)
        effect = Effects.GrantFlashToSpells(
            spellFilter = GameObjectFilter.Any,
            duration = Duration.EndOfTurn
        )
        description = "{1}, {T}, Sacrifice this land: You may cast spells this turn as though they had flash."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "245"
        artist = "Jonas De Ro"
        flavorText = "The Planar Bridge opened over the Chamber of the Guildpact, reducing the symbol " +
            "of Ravnica's endurance to rubble."
        imageUri = "https://cards.scryfall.io/normal/front/a/b/ab95f6e7-b806-47fe-a071-6c38b3176d94.jpg?1783933367"
    }
}
