package com.wingedsheep.mtg.sets.definitions.xln.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Skymarch Bloodletter
 * {2}{B}
 * Creature — Vampire Soldier
 * 2/2
 * Flying
 * When this creature enters, target opponent loses 1 life and you gain 1 life.
 */
val SkymarchBloodletter = card("Skymarch Bloodletter") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Vampire Soldier"
    power = 2
    toughness = 2
    oracleText = "Flying\nWhen this creature enters, target opponent loses 1 life and you gain 1 life."

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.self.enters()
        val opponent = target(Targets.Opponent)
        effect = Effects.LoseLife(1, opponent) then Effects.GainLife(1)
        description = "When this creature enters, target opponent loses 1 life and you gain 1 life."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "124"
        artist = "Tomasz Jedruszek"
        flavorText = "From the perpetual shadowsmoke that hung above the ship, a silent form emerged, lips curled with malice and anticipation."
        imageUri = "https://cards.scryfall.io/normal/front/1/f/1f4b41d7-30fa-43ab-8f8d-c1de68a19add.jpg?1783935753"
    }
}
