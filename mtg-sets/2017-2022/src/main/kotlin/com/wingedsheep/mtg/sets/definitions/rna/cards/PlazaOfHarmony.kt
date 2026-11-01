package com.wingedsheep.mtg.sets.definitions.rna.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.values.LandControllerScope

/**
 * Plaza of Harmony
 * Land
 * When this land enters, if you control two or more Gates, you gain 3 life.
 * {T}: Add {C}.
 * {T}: Add one mana of any type that a Gate you control could produce.
 *
 * "Any type" is the five colors plus colorless (CR 106.1b); colorless is already the first mana
 * ability, so the second only needs the colors, read from Gates you control through
 * `LandsCouldProduce(YOU, landFilter = Gate)`. Plaza itself isn't a Gate, so it never reflects itself.
 */
val PlazaOfHarmony = card("Plaza of Harmony") {
    typeLine = "Land"
    oracleText = "When this land enters, if you control two or more Gates, you gain 3 life.\n" +
        "{T}: Add {C}.\n" +
        "{T}: Add one mana of any type that a Gate you control could produce."

    triggeredAbility {
        trigger = Triggers.self.enters()
        interveningIf = Conditions.YouControlAtLeast(2, GameObjectFilter.Land.withSubtype("Gate"))
        effect = Effects.GainLife(3)
    }

    activatedAbility {
        cost = Costs.Tap
        manaAbility = true
        effect = Effects.AddColorlessMana(1)
        description = "{T}: Add {C}."
    }

    activatedAbility {
        cost = Costs.Tap
        manaAbility = true
        effect = Effects.AddManaOfColorLandsCouldProduce(
            LandControllerScope.YOU,
            landFilter = GameObjectFilter.Land.withSubtype("Gate"),
        )
        description = "{T}: Add one mana of any type that a Gate you control could produce."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "254"
        artist = "Jedd Chevrier"
        imageUri = "https://cards.scryfall.io/normal/front/0/7/07f2ae00-206d-4984-84eb-d10ab75d3791.jpg?1783933616"
    }
}
