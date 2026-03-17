package com.wingedsheep.mtg.sets.definitions.dominaria.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetCreatureOrPlaneswalker
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Yawgmoth's Vile Offering
 * {4}{B}
 * Legendary Sorcery
 *
 * (You may cast a legendary sorcery only if you control a legendary creature or planeswalker.)
 * Put up to one target creature or planeswalker card from a graveyard onto the battlefield
 * under your control. Destroy up to one target creature or planeswalker. Exile Yawgmoth's
 * Vile Offering.
 */
val YawgmothsVileOffering = card("Yawgmoth's Vile Offering") {
    manaCost = "{4}{B}"
    typeLine = "Legendary Sorcery"
    oracleText = "(You may cast a legendary sorcery only if you control a legendary creature or planeswalker.)\n" +
        "Put up to one target creature or planeswalker card from a graveyard onto the battlefield under your control. " +
        "Destroy up to one target creature or planeswalker. Exile Yawgmoth's Vile Offering."

    spell {
        castOnlyIf(Conditions.ControlLegendaryCreatureOrPlaneswalker)
        selfExile()

        val graveyardTarget = target(
            "creature or planeswalker card in a graveyard",
            TargetObject(
                optional = true,
                filter = TargetFilter(GameObjectFilter.CreatureOrPlaneswalker, zone = Zone.GRAVEYARD)
            )
        )
        val permanentTarget = target(
            "creature or planeswalker",
            TargetCreatureOrPlaneswalker(optional = true)
        )

        effect = Effects.PutOntoBattlefieldUnderYourControl(graveyardTarget)
            .then(Effects.Destroy(permanentTarget))
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "114"
        artist = "Noah Bradley"
        flavorText = "Centuries ago, a mad god offered a simple trade."
        imageUri = "https://cards.scryfall.io/normal/front/b/f/bf9794a7-caf5-40b9-8046-67823ff64a2c.jpg?1665611825"
        ruling("2018-04-27", "You can't cast a legendary sorcery unless you control a legendary creature or a legendary planeswalker. Once you begin to cast a legendary sorcery, losing control of your legendary creatures and planeswalkers won't affect that spell.")
        ruling("2018-04-27", "Other than the casting restriction, the legendary supertype on a sorcery carries no additional rules. You may cast any number of legendary sorceries in a turn, and your deck may contain any number of legendary cards (but no more than four of any with the same name).")
    }
}
