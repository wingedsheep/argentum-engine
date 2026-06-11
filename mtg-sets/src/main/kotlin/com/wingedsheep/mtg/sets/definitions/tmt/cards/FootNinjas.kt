package com.wingedsheep.mtg.sets.definitions.tmt.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Foot Ninjas
 * {4}{W/B}{W/B}
 * Creature — Human Ninja
 * 5/5
 *
 * Sneak {3}{W/B} (You may cast this spell for {3}{W/B} if you also return an
 * unblocked attacker you control to hand during the declare blockers step.
 * It enters tapped and attacking.)
 * When this creature enters, you gain 3 life.
 */
val FootNinjas = card("Foot Ninjas") {
    manaCost = "{4}{W/B}{W/B}"
    colorIdentity = "WB"
    typeLine = "Creature — Human Ninja"
    oracleText = "Sneak {3}{W/B} (You may cast this spell for {3}{W/B} if you also return an unblocked attacker you control to hand during the declare blockers step. It enters tapped and attacking.)\nWhen this creature enters, you gain 3 life."
    power = 5
    toughness = 5

    sneak("{3}{W/B}")

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = Effects.GainLife(3)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "147"
        artist = "Miklós Ligeti"
        imageUri = "https://cards.scryfall.io/normal/front/a/b/abb1ab9c-b067-4b75-8e5b-a893b7948df1.jpg?1771503783"
    }
}
