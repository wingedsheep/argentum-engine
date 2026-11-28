package com.wingedsheep.mtg.sets.definitions.dka.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersTapped
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.effects.ManaRestriction

/**
 * Altar of the Lost
 * {3}
 * Artifact
 * This artifact enters tapped.
 * {T}: Add two mana in any combination of colors. Spend this mana only to cast spells with
 *      flashback from a graveyard.
 *
 * "In any combination of colors" colours each pip independently ([Effects.AddManaInAnyCombination]).
 * The restriction is two conditions at once: the spell has flashback, and it is cast from **a**
 * graveyard — any player's, so a flashback card cast out of an opponent's graveyard qualifies too
 * (`CastFromGraveyardOnly(anyGraveyard = true)`). The card cast from its owner's hand doesn't.
 */
val AltarOfTheLost = card("Altar of the Lost") {
    manaCost = "{3}"
    colorIdentity = ""
    typeLine = "Artifact"
    oracleText = "This artifact enters tapped.\n" +
        "{T}: Add two mana in any combination of colors. Spend this mana only to cast spells with " +
        "flashback from a graveyard."

    replacementEffect(EntersTapped())

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddManaInAnyCombination(
            amount = 2,
            restriction = ManaRestriction.AllOf(
                listOf(
                    ManaRestriction.SpellsWithKeywordOnly(Keyword.FLASHBACK),
                    ManaRestriction.CastFromGraveyardOnly(anyGraveyard = true),
                )
            ),
        )
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "144"
        artist = "Daarken"
        imageUri = "https://cards.scryfall.io/normal/front/7/7/774e5322-1b41-488d-94b1-7742fbd983d4.jpg?1783940793"
    }
}
