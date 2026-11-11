package com.wingedsheep.mtg.sets.definitions.bfz.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ActivationRestriction
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.effects.ManaRestriction

/**
 * Shrine of the Forsaken Gods
 * Land
 * {T}: Add {C}.
 * {T}: Add {C}{C}. Spend this mana only to cast colorless spells. Activate only if you control seven
 * or more lands.
 *
 * The {C}{C} is restricted to colorless *spells* only ([ManaRestriction.ColorlessSpellsOrAbilitiesOnly]
 * with its defaults) — abilities can't spend it.
 */
val ShrineOfTheForsakenGods = card("Shrine of the Forsaken Gods") {
    manaCost = ""
    colorIdentity = ""
    typeLine = "Land"
    oracleText = "{T}: Add {C}.\n" +
        "{T}: Add {C}{C}. Spend this mana only to cast colorless spells. Activate only if you control " +
        "seven or more lands."

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddColorlessMana(1)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddColorlessMana(2, restriction = ManaRestriction.ColorlessSpellsOrAbilitiesOnly())
        manaAbility = true
        timing = TimingRule.ManaAbility
        restrictions = listOf(ActivationRestriction.OnlyIfCondition(Conditions.ControlLandsAtLeast(7)))
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "245"
        artist = "Daniel Ljunggren"
        flavorText = "The gods known to the merfolk as Ula, Emeria, and Cosi were nothing more than false " +
            "memories of a monstrous trinity: the Eldrazi titans Ulamog, Emrakul, and Kozilek."
        imageUri = "https://cards.scryfall.io/normal/front/9/d/9da2af9a-28c4-4edd-95c4-cc09c2de4adc.jpg?1783938172"
    }
}
