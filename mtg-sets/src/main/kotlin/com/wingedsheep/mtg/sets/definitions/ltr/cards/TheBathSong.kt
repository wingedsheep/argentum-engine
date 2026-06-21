package com.wingedsheep.mtg.sets.definitions.ltr.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.AddManaEffect
import com.wingedsheep.sdk.scripting.effects.ForEachTargetEffect
import com.wingedsheep.sdk.scripting.effects.ShuffleLibraryEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns

/**
 * The Bath Song
 * {3}{U}
 * Enchantment — Saga
 *
 * (As this Saga enters and after your draw step, add a lore counter. Sacrifice after III.)
 * I, II — Draw two cards, then discard a card.
 * III — Shuffle any number of target cards from your graveyard into your library. Add {U}{U}.
 */
val TheBathSong = card("The Bath Song") {
    manaCost = "{3}{U}"
    colorIdentity = "U"
    typeLine = "Enchantment — Saga"
    oracleText = "(As this Saga enters and after your draw step, add a lore counter. Sacrifice after III.)\n" +
        "I, II — Draw two cards, then discard a card.\n" +
        "III — Shuffle any number of target cards from your graveyard into your library. Add {U}{U}."

    sagaChapter(1) {
        effect = Patterns.Hand.loot(draw = 2, discard = 1)
    }

    sagaChapter(2) {
        effect = Patterns.Hand.loot(draw = 2, discard = 1)
    }

    sagaChapter(3) {
        target(
            "target cards from your graveyard",
            TargetObject(
                unlimited = true,
                filter = TargetFilter(GameObjectFilter.Any.ownedByYou(), zone = Zone.GRAVEYARD)
            )
        )
        effect = ForEachTargetEffect(
            effects = listOf(Effects.Move(EffectTarget.ContextTarget(0), Zone.LIBRARY))
        ).then(ShuffleLibraryEffect())
            .then(AddManaEffect(Color.BLUE, 2))
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "40"
        artist = "JB Casacop"
        imageUri = "https://cards.scryfall.io/normal/front/f/b/fb11c04f-6f17-4da4-bbf6-0bd09de6e544.jpg?1688569306"
    }
}
