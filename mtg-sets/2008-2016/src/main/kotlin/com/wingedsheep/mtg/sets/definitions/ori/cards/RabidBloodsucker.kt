package com.wingedsheep.mtg.sets.definitions.ori.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Rabid Bloodsucker
 * {4}{B}
 * Creature — Vampire
 * 3/2
 * Flying
 * When this creature enters, each player loses 2 life.
 *
 * "Each player" includes the Bloodsucker's controller — [Player.Each], not each opponent.
 */
val RabidBloodsucker = card("Rabid Bloodsucker") {
    manaCost = "{4}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Vampire"
    power = 3
    toughness = 2
    oracleText = "Flying\nWhen this creature enters, each player loses 2 life."

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.LoseLife(2, EffectTarget.PlayerRef(Player.Each))
        description = "When this creature enters, each player loses 2 life."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "113"
        artist = "Seb McKinnon"
        flavorText = "She haunts the streets, wings spread wide, with carrion on her breath and blood in her eyes."
        imageUri = "https://cards.scryfall.io/normal/front/7/3/7335da62-3b59-4c73-a19e-b1733393ed88.jpg?1783938338"
    }
}
