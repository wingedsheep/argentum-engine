package com.wingedsheep.mtg.sets.definitions.mh1.cards

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
 * Return from Extinction
 * {1}{B}
 * Sorcery
 *
 * Choose one —
 * • Return target creature card from your graveyard to your hand.
 * • Return two target creature cards that share a creature type from your graveyard to your hand.
 *
 * Both modes target (CR 601.2c). The second mode's pair is one two-slot requirement with
 * [TargetObject.sameCreatureType] — the same shape as Unbury — which reads a graveyard card's printed
 * creature types (a changeling card has them all). Each target is returned on its own, so when one of
 * the pair leaves the graveyard in response the other is still returned (2019-06-14 ruling).
 */
val ReturnFromExtinction = card("Return from Extinction") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
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
        rarity = Rarity.COMMON
        collectorNumber = "104"
        artist = "Deruchenko Alexander"
        flavorText = "The First Sliver calls. The dead answer."
        imageUri = "https://cards.scryfall.io/normal/front/c/6/c6db664e-f0c4-4e60-add7-89b9f210cbc7.jpg?1783933123"
        ruling("2019-06-14", "If you choose the second mode, the cards must share at least one creature type, such as Sliver or Warrior. Card types such as artifact, and supertypes such as legendary or snow, aren't creature types.")
        ruling("2019-06-14", "If you choose the second mode and one of the two cards leaves your graveyard, you'll still return the other card to your hand as long as it has a creature type that the other card had as it left your graveyard.")
    }
}
