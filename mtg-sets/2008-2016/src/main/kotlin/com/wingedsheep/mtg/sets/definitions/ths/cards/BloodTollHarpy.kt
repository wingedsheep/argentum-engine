package com.wingedsheep.mtg.sets.definitions.ths.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Blood-Toll Harpy
 * {2}{B}
 * Creature — Harpy
 * 2/1
 * Flying
 * When this creature enters, each player loses 1 life.
 *
 * "Each player" includes the Harpy's controller — [Player.Each], not each opponent.
 */
val BloodTollHarpy = card("Blood-Toll Harpy") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Harpy"
    power = 2
    toughness = 1
    oracleText = "Flying\nWhen this creature enters, each player loses 1 life."

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.LoseLife(1, EffectTarget.PlayerRef(Player.Each))
        description = "When this creature enters, each player loses 1 life."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "79"
        artist = "Kev Walker"
        flavorText = "When harpies demand a toll to cross through their territory, consider yourself lucky if they permit payment in coin."
        imageUri = "https://cards.scryfall.io/normal/front/d/3/d334fc9b-c1f4-411e-8619-a92f1cc4e7db.jpg?1783939783"
    }
}
