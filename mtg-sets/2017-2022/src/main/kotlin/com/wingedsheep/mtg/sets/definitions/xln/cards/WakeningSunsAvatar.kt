package com.wingedsheep.mtg.sets.definitions.xln.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Wakening Sun's Avatar — Ixalan #44
 * {5}{W}{W}{W} · Creature — Dinosaur Avatar · 7/7 · Mythic
 *
 * When this creature enters, if you cast it from your hand, destroy all non-Dinosaur creatures.
 *
 * Reiver Demon's shape: the intervening-if is [Conditions.WasCastFromHand] on the enters trigger,
 * so a reanimated or blinked Avatar gets no wipe. The wipe is [Effects.DestroyAll] over
 * `Creature.notSubtype(DINOSAUR)`; the Avatar is itself a Dinosaur and survives, as do your other
 * Dinosaurs.
 */
val WakeningSunsAvatar = card("Wakening Sun's Avatar") {
    manaCost = "{5}{W}{W}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Dinosaur Avatar"
    power = 7
    toughness = 7
    oracleText = "When this creature enters, if you cast it from your hand, destroy all non-Dinosaur creatures."

    triggeredAbility {
        trigger = Triggers.self.enters()
        interveningIf = Conditions.WasCastFromHand
        effect = Effects.DestroyAll(
            filter = GameObjectFilter.Creature.notSubtype(Subtype.DINOSAUR)
        )
        description = "When this creature enters, if you cast it from your hand, destroy all non-Dinosaur creatures."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "44"
        artist = "Tyler Jacobson"
        flavorText = "\"Kinjalli's encompassing rays herald new life in the moment of the sun's dawning.\"\n—Chitlati, Lightning Rider"
        imageUri = "https://cards.scryfall.io/normal/front/7/4/7434abe4-87eb-4709-a26d-4e23154b4d31.jpg?1783935787"
    }
}
