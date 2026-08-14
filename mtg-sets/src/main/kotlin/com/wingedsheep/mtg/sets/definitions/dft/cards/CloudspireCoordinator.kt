package com.wingedsheep.mtg.sets.definitions.dft.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CrewSaddleContribution

/**
 * Cloudspire Coordinator
 * {R}{W}
 * Creature — Human Pilot
 * 3/1
 *
 * When this creature enters, scry 2.
 * {T}: Create X 1/1 colorless Pilot creature tokens, where X is the number of Mounts and/or
 * Vehicles that entered the battlefield under your control this turn. The tokens have "This token
 * saddles Mounts and crews Vehicles as though its power were 2 greater."
 *
 * X reads the per-player entry log (`PermanentsEnteredUnderControlThisTurnComponent`) rather than
 * scanning the battlefield, so a Vehicle that entered and has since died or been exiled still
 * counts — the entry event is what the card asks about.
 *
 * "Mounts and/or Vehicles" is one any-of amount, not two summed ones: a permanent carrying both
 * subtypes must count once. Hence `subtypesEnteredUnderControlThisTurn(setOf(MOUNT, VEHICLE))`.
 */
val CloudspireCoordinator = card("Cloudspire Coordinator") {
    manaCost = "{R}{W}"
    colorIdentity = "RW"
    typeLine = "Creature — Human Pilot"
    power = 3
    toughness = 1
    oracleText = "When this creature enters, scry 2.\n" +
        "{T}: Create X 1/1 colorless Pilot creature tokens, where X is the number of Mounts " +
        "and/or Vehicles that entered the battlefield under your control this turn. The tokens " +
        "have \"This token saddles Mounts and crews Vehicles as though its power were 2 greater.\""

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = Patterns.Library.scry(2)
    }

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.CreateToken(
            count = DynamicAmounts.subtypesEnteredUnderControlThisTurn(setOf(Subtype.MOUNT, Subtype.VEHICLE)),
            power = 1,
            toughness = 1,
            creatureTypes = setOf("Pilot"),
            imageUri = "https://cards.scryfall.io/normal/front/8/6/8672d795-04f9-4089-9c92-6d6ff628da12.jpg?1783907682",
            staticAbilities = listOf(CrewSaddleContribution(modifier = 2))
        )
        // The auto-derived text inlines the dynamic amount ("Create the number of Mounts and/or
        // Vehicles that entered … 1/1 Pilot creature tokens"), and this string is the action-menu
        // button label. Use the printed wording instead.
        description = "{T}: Create X 1/1 colorless Pilot creature tokens, where X is the number of " +
            "Mounts and/or Vehicles that entered the battlefield under your control this turn."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "196"
        artist = "Eduardo Francisco"
        imageUri = "https://cards.scryfall.io/normal/front/e/e/eef16cda-9150-4e7d-8490-d9f287b81b62.jpg?1783907860"
    }
}
