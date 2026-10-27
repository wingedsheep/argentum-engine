package com.wingedsheep.mtg.sets.definitions.khm.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Tyrite Sanctum
 * Land
 * {T}: Add {C}.
 * {2}, {T}: Target legendary creature becomes a God in addition to its other types.
 * Put a +1/+1 counter on it.
 * {4}, {T}, Sacrifice this land: Put an indestructible counter on target God.
 *
 * The God subtype is added permanently ([Effects.AddCreatureType] with [Duration.Permanent]);
 * "target God" is any permanent with the God subtype, read off projected state so a creature
 * made a God by the middle ability is a legal target. The indestructible counter grants
 * indestructible through the state projector's counter-keyword mapping.
 */
val TyriteSanctum = card("Tyrite Sanctum") {
    manaCost = ""
    colorIdentity = ""
    typeLine = "Land"
    oracleText = "{T}: Add {C}.\n" +
        "{2}, {T}: Target legendary creature becomes a God in addition to its other types. " +
        "Put a +1/+1 counter on it.\n" +
        "{4}, {T}, Sacrifice this land: Put an indestructible counter on target God."

    // {T}: Add {C}.
    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddColorlessMana(1)
        manaAbility = true
    }

    // {2}, {T}: Target legendary creature becomes a God and gets a +1/+1 counter.
    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{2}"), Costs.Tap)
        val creature = target(TargetFilter.Creature.legendary())
        effect = Effects.AddCreatureType("God", creature, Duration.Permanent) then
            Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, creature)
        description = "Target legendary creature becomes a God in addition to its other types. " +
            "Put a +1/+1 counter on it."
    }

    // {4}, {T}, Sacrifice this land: Put an indestructible counter on target God.
    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{4}"), Costs.Tap, Costs.SacrificeSelf)
        val god = target(TargetFilter(GameObjectFilter.Permanent.withSubtype(Subtype.GOD)))
        effect = Effects.AddCounters(CounterType.INDESTRUCTIBLE, 1, god)
        description = "Put an indestructible counter on target God."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "272"
        artist = "Volkan Baǵa"
        imageUri = "https://cards.scryfall.io/normal/front/3/0/309ff3bf-acd3-4bea-b518-6e25f6cfce61.jpg?1783928170"
        ruling(
            "2021-02-05",
            "You can activate the middle ability targeting a legendary creature that's already a God. " +
                "You'll put a +1/+1 counter on it."
        )
    }
}
