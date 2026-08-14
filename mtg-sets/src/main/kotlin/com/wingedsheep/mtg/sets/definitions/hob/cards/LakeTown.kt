package com.wingedsheep.mtg.sets.definitions.hob.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersTapped
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetCreature

/**
 * Lake-town — The Hobbit #186
 * Land · Common
 *
 * This land enters tapped.
 * {T}: Add {W} or {U}.
 * {2}{W}{U}, {T}, Sacrifice this land: Put two +1/+1 counters on target Human you control.
 * Activate only as a sorcery.
 *
 * The Human member of the HOB tapland cycle; see [IronHills] for the shape. "Add {W} or {U}" is two
 * separate mana abilities (one per color) rather than a choice on resolution, matching every other
 * dual tapland in the engine.
 */
val LakeTown = card("Lake-town") {
    manaCost = ""
    colorIdentity = "WU"
    typeLine = "Land"
    oracleText = "This land enters tapped.\n" +
        "{T}: Add {W} or {U}.\n" +
        "{2}{W}{U}, {T}, Sacrifice this land: Put two +1/+1 counters on target Human you control. " +
        "Activate only as a sorcery."

    replacementEffect(EntersTapped())

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddMana(Color.WHITE)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }
    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddMana(Color.BLUE)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{2}{W}{U}"), Costs.Tap, Costs.SacrificeSelf)
        val human = target(
            "target Human you control",
            TargetCreature(filter = TargetFilter.CreatureYouControl.withSubtype(Subtype.HUMAN))
        )
        effect = Effects.AddCounters(Counters.PLUS_ONE_PLUS_ONE, 2, human)
        timing = TimingRule.SorcerySpeed
        description = "Put two +1/+1 counters on target Human you control."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "186"
        artist = "Marina Ortega Lorente"
        flavorText = "The Elves spoke of a strange town built right on the surface of the Long Lake, " +
            "under the shadow of the Dragon-mountain."
        imageUri = "https://cards.scryfall.io/normal/front/2/f/2fbd0584-81a7-4c47-8af1-1c8635899a97.jpg?1785323601"
    }
}
