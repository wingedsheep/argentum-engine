package com.wingedsheep.mtg.sets.definitions.leg.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.OnEnterRun
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.EntityNumericProperty

/**
 * Wood Elemental
 * {3}{G}
 * Creature — Elemental
 * &#42;/&#42;
 * As this creature enters, sacrifice any number of untapped Forests.
 * Wood Elemental's power and toughness are each equal to the number of Forests sacrificed as it
 * entered.
 *
 * The as-enters sacrifice is an [OnEnterRun] replacement, so it happens however the creature enters
 * (the 2004-10-04 ruling), and its outcome is stamped on the permanent with
 * [Effects.RecordValueAsEntered] for the characteristic-defining P/T to read back during projection —
 * the same stamp Nameless Race's paid life uses.
 */
val WoodElemental = card("Wood Elemental") {
    manaCost = "{3}{G}"
    typeLine = "Creature — Elemental"
    oracleText = "As this creature enters, sacrifice any number of untapped Forests.\n" +
        "Wood Elemental's power and toughness are each equal to the number of Forests sacrificed " +
        "as it entered."

    dynamicStats(
        DynamicAmounts.propertyOf(EffectTarget.Self, EntityNumericProperty.ValueChosenAsEntered)
    )

    replacementEffect(
        OnEnterRun(
            Effects.SacrificeAnyNumber(GameObjectFilter.Land.withSubtype(Subtype.FOREST).untapped()) then
                Effects.RecordValueAsEntered(DynamicAmounts.permanentsSacrificedThisWay())
        )
    )

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "215"
        artist = "Brian Snõddy"
        imageUri = "https://cards.scryfall.io/normal/front/e/d/edc71122-2951-43eb-8ca8-1cda6d231013.jpg?1783948042"
        ruling(
            "2004-10-04",
            "The Forests are sacrificed to define the power/toughness of this card right before it " +
                "enters. You have to do this sacrifice no matter how this card enters."
        )
    }
}
