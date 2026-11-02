package com.wingedsheep.mtg.sets.definitions.m19.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Vampire Sovereign
 * {3}{B}{B}
 * Creature — Vampire Noble
 * 3/4
 * Flying
 * When this creature enters, target opponent loses 3 life and you gain 3 life.
 */
val VampireSovereign = card("Vampire Sovereign") {
    manaCost = "{3}{B}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Vampire Noble"
    power = 3
    toughness = 4
    oracleText = "Flying\nWhen this creature enters, target opponent loses 3 life and you gain 3 life."

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.self.enters()
        val opponent = target(Targets.Opponent)
        effect = Effects.LoseLife(3, opponent) then Effects.GainLife(3)
        description = "When this creature enters, target opponent loses 3 life and you gain 3 life."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "125"
        artist = "Volkan Baǵa"
        flavorText = "\"Your service shall be rewarded.\"\n—Queen Lian"
        imageUri = "https://cards.scryfall.io/normal/front/e/e/ee338221-ead9-4b89-8b0c-12745c4ca13d.jpg?1783934560"
    }
}
