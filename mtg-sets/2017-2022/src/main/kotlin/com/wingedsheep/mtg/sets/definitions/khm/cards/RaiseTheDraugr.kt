package com.wingedsheep.mtg.sets.definitions.khm.cards

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
 * Raise the Draugr
 * {1}{B}
 * Instant
 *
 * Choose one —
 * • Return target creature card from your graveyard to your hand.
 * • Return two target creature cards that share a creature type from your graveyard
 *   to your hand.
 *
 * The second mode's pair is one two-slot requirement with [TargetObject.sameCreatureType]. Each
 * target is returned on its own, so when one of the pair leaves the graveyard in response the other
 * is still returned (CR 608.2b; the 2021-02-05 ruling).
 * The shared type isn't re-checked at resolution against the departed card's last-known types;
 * a graveyard card's types almost never change, so the survivor still shares one.
 */
val RaiseTheDraugr = card("Raise the Draugr") {
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
        rarity = Rarity.COMMON
        collectorNumber = "105"
        artist = "Randy Vargas"
        flavorText = "\"King Narfi calls. We go to war!\""
        imageUri = "https://cards.scryfall.io/normal/front/1/a/1a67c196-632c-4c7b-a132-de07d894e634.jpg?1783928243"
        ruling("2021-02-05", "If you choose the second mode, the cards must share at least one creature type, such as Knight or Djinn. Card types (e.g., artifact) and supertypes (e.g., legendary or snow) aren't creature types.")
        ruling("2021-02-05", "If you choose the second mode and one of the two cards leaves your graveyard, you'll still return the other card to your hand as long as it has a creature type that the other card had as it left your graveyard.")
    }
}
