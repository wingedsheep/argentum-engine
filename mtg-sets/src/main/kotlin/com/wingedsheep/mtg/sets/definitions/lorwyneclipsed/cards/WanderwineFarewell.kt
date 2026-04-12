package com.wingedsheep.mtg.sets.definitions.lorwyneclipsed.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.ConditionalEffect
import com.wingedsheep.sdk.scripting.effects.CreateTokenEffect
import com.wingedsheep.sdk.scripting.effects.ForEachTargetEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Wanderwine Farewell
 * {5}{U}{U}
 * Kindred Sorcery — Merfolk
 *
 * Convoke
 * Return one or two target nonland permanents to their owners' hands.
 * Then if you control a Merfolk, create a 1/1 white and blue Merfolk
 * creature token for each permanent returned to its owner's hand this way.
 */
val WanderwineFarewell = card("Wanderwine Farewell") {
    manaCost = "{5}{U}{U}"
    typeLine = "Kindred Sorcery — Merfolk"
    oracleText = "Convoke (Your creatures can help cast this spell. Each creature you tap while casting this spell pays for {1} or one mana of that creature's color.)\n" +
        "Return one or two target nonland permanents to their owners' hands. Then if you control a Merfolk, create a 1/1 white and blue Merfolk creature token for each permanent returned to its owner's hand this way."

    keywords(Keyword.CONVOKE)

    spell {
        target = TargetObject(count = 2, minCount = 1, filter = TargetFilter.NonlandPermanent)
        effect = ForEachTargetEffect(
            listOf(Effects.ReturnToHand(EffectTarget.ContextTarget(0)))
        ) then ConditionalEffect(
            condition = Conditions.ControlCreatureOfType(Subtype.MERFOLK),
            effect = CreateTokenEffect(
                count = DynamicAmount.TargetCount,
                power = 1,
                toughness = 1,
                colors = setOf(Color.WHITE, Color.BLUE),
                creatureTypes = setOf("Merfolk"),
                imageUri = "https://cards.scryfall.io/normal/front/4/c/4c5ad4e1-b489-4023-88ab-1200c5f26ffc.jpg?1767955704"
            )
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "83"
        artist = "Aldo Domínguez"
        imageUri = "https://cards.scryfall.io/normal/front/4/7/473123a0-1366-406b-9b9e-154c0e9c224f.jpg?1767659604"
    }
}
