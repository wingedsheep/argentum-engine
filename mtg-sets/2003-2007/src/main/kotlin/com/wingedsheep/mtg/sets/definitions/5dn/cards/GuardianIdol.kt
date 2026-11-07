package com.wingedsheep.mtg.sets.definitions.`5dn`.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersTapped
import com.wingedsheep.sdk.scripting.TimingRule

/**
 * Guardian Idol
 * {2}
 * Artifact
 *
 * This artifact enters tapped.
 * {T}: Add {C}.
 * {2}: This artifact becomes a 2/2 Golem artifact creature until end of turn.
 *
 * The animation is additive: `BecomeCreature` with no `removeTypes` keeps the Artifact type and
 * `addTypes = setOf("ARTIFACT")` restates it, matching the printed "artifact creature". The default
 * target is the Idol itself and the default duration is end of turn.
 */
val GuardianIdol = card("Guardian Idol") {
    manaCost = "{2}"
    colorIdentity = ""
    typeLine = "Artifact"
    oracleText = "This artifact enters tapped.\n" +
        "{T}: Add {C}.\n" +
        "{2}: This artifact becomes a 2/2 Golem artifact creature until end of turn."

    replacementEffect(EntersTapped())

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddColorlessMana(1)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = Costs.Mana("{2}")
        effect = Effects.BecomeCreature(
            power = 2,
            toughness = 2,
            creatureTypes = setOf("Golem"),
            addTypes = setOf("ARTIFACT")
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "128"
        artist = "Edward P. Beard, Jr."
        imageUri = "https://cards.scryfall.io/normal/front/a/6/a6a62a73-b7db-47ec-9b68-65dd7c1a06a5.jpg?1783944380"
    }
}
