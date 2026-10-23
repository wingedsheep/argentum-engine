package com.wingedsheep.mtg.sets.definitions.sok.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CardNamePool
import com.wingedsheep.sdk.scripting.ChoiceType
import com.wingedsheep.sdk.scripting.EntersWithChoice
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.PreventActivatedAbilities

/**
 * Pithing Needle
 * {1}
 * Artifact
 * As this artifact enters, choose a card name.
 * Activated abilities of sources with the chosen name can't be activated unless they're mana abilities.
 *
 * Modeling notes:
 * - Same shape as Sorcerous Spyglass minus the look at an opponent's hand: the as-enters name choice is
 *   [EntersWithChoice]`(ChoiceType.CARD_NAME, CardNamePool.ANY)`, stored durably under
 *   `ChoiceSlot.CARD_NAME`.
 * - The lock is [PreventActivatedAbilities]`(nonManaAbilitiesOnly = true, anyZone = true)` over the bare
 *   chosen-name predicate — "sources" reaches cards in every zone, so cycling and channel are locked too.
 */
val PithingNeedle = card("Pithing Needle") {
    manaCost = "{1}"
    colorIdentity = ""
    typeLine = "Artifact"
    oracleText = "As this artifact enters, choose a card name.\n" +
        "Activated abilities of sources with the chosen name can't be activated unless they're mana abilities."

    // As this artifact enters, choose a card name.
    replacementEffect(
        EntersWithChoice(
            choiceType = ChoiceType.CARD_NAME,
            cardNamePool = CardNamePool.ANY,
        )
    )

    // Activated abilities of sources with the chosen name (in any zone) can't be activated unless mana abilities.
    staticAbility {
        ability = PreventActivatedAbilities(
            filter = GameObjectFilter.Any.namedFromChosenComponent(),
            nonManaAbilitiesOnly = true,
            anyZone = true,
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "158"
        artist = "Pete Venters"
        imageUri = "https://cards.scryfall.io/normal/front/7/8/78eb9e1d-113e-45ff-8435-32ee42fa5631.jpg?1783944130"
    }
}
