package com.wingedsheep.mtg.sets.definitions.mmq.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Forced March
 * {X}{B}{B}{B}
 * Sorcery
 * Destroy all creatures with mana value X or less.
 *
 * The bound is part of the *filter*, not a count: [GameObjectFilter.manaValueAtMostX] adds
 * `CardPredicate.ManaValueAtMostX`, which reads the {X} paid for this spell at resolution.
 * Same idiom as Day of Black Sun's board wipe.
 */
val ForcedMarch = card("Forced March") {
    manaCost = "{X}{B}{B}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "Destroy all creatures with mana value X or less."

    spell {
        effect = Effects.DestroyAll(GameObjectFilter.Creature.manaValueAtMostX())
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "136"
        artist = "Greg Hildebrandt & Tim Hildebrandt"
        flavorText = "The Caterans call it a screening process. The dead are in no condition to argue."
        imageUri = "https://cards.scryfall.io/normal/front/3/6/36eae0e1-7100-449d-a259-7abfcd429117.jpg"
    }
}
