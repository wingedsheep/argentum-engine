package com.wingedsheep.mtg.sets.definitions.ecl.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.mode
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.ModalEffect
import com.wingedsheep.sdk.scripting.effects.Mode
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Unbury
 * {1}{B}
 * Instant
 *
 * Choose one —
 * • Return target creature card from your graveyard to your hand.
 * • Return two target creature cards that share a creature type from your graveyard
 *   to your hand.
 *
 * Both modes target (CR 601.2c). The second mode's pair is one two-slot requirement with
 * [TargetObject.sameCreatureType], which reads a graveyard card's printed creature types (a
 * changeling card has them all). Each target is returned on its own, so when one of the pair
 * leaves the graveyard in response the other is still returned (CR 608.2b; the 2025-11-17 ruling).
 * The shared type isn't re-checked at resolution against the departed card's last-known types;
 * a graveyard card's types almost never change, so the survivor still shares one.
 */
val Unbury = card("Unbury") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Instant"
    oracleText = "Choose one —\n" +
        "• Return target creature card from your graveyard to your hand.\n" +
        "• Return two target creature cards that share a creature type from your graveyard to your hand."

    spell {
        effect = ModalEffect.chooseOne(
            mode("Return target creature card from your graveyard to your hand") {
                val creatureInYourGraveyard = target(TargetFilter.CreatureInYourGraveyard)
                effect = Effects.ReturnToHand(creatureInYourGraveyard)
            },
            Mode.withTarget(
                Effects.ForEachTarget(Effects.ReturnToHand(EffectTarget.ContextTarget(0))),
                TargetObject(
                    count = 2,
                    filter = TargetFilter.CreatureInYourGraveyard,
                    sameCreatureType = true
                ),
                "Return two target creature cards that share a creature type from your graveyard to your hand"
            )
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "123"
        artist = "Kev Fang"
        flavorText = "A well-aged boggart body is steeped in magics richer than any fae glamer."
        imageUri = "https://cards.scryfall.io/normal/front/b/0/b00766db-4109-4225-a62a-fa12fd526970.jpg?1767871924"
        ruling("2025-11-17", "If you choose the second mode, the cards must share at least one creature type, such as Faerie or Goblin. Card types such as artifact, and supertypes such as legendary or snow, aren't creature types.")
        ruling("2025-11-17", "If you choose the second mode and one of the two cards leaves your graveyard, you'll still return the other card to your hand as long as it has a creature type that the other card had as it left your graveyard.")
    }
}
