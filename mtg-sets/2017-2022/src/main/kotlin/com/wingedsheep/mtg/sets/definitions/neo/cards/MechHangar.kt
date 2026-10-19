package com.wingedsheep.mtg.sets.definitions.neo.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.effects.ManaRestriction
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Mech Hangar
 * Land
 * {T}: Add {C}.
 * {T}: Add one mana of any color. Spend this mana only to cast a Pilot or Vehicle spell.
 * {3}, {T}: Target Vehicle becomes an artifact creature until end of turn.
 */
val MechHangar = card("Mech Hangar") {
    manaCost = ""
    colorIdentity = ""
    typeLine = "Land"
    oracleText = "{T}: Add {C}.\n" +
        "{T}: Add one mana of any color. Spend this mana only to cast a Pilot or Vehicle spell.\n" +
        "{3}, {T}: Target Vehicle becomes an artifact creature until end of turn."

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddColorlessMana(1)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddManaOfChoice(
            restriction = ManaRestriction.SubtypeSpellsOnly(setOf("Pilot", "Vehicle")),
        )
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{3}"), Costs.Tap)
        val vehicle = target(TargetFilter(GameObjectFilter.Permanent.withSubtype(Subtype.VEHICLE)))
        // Add types without changing printed or previously set base power and toughness.
        effect = Effects.AddCardType("Artifact", vehicle, Duration.EndOfTurn) then
            Effects.AddCardType("Creature", vehicle, Duration.EndOfTurn)
        description = "Target Vehicle becomes an artifact creature until end of turn."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "270"
        artist = "Julian Kok Joon Wen"
        imageUri = "https://cards.scryfall.io/normal/front/c/0/c093984d-38cd-4b49-b179-1e289ab442d1.jpg?1783923817"
    }
}
