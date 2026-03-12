package com.wingedsheep.mtg.sets.definitions.dominaria.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Howling Golem
 * {3}
 * Artifact Creature — Golem
 * 2/3
 * Whenever Howling Golem attacks or blocks, each player draws a card.
 */
val HowlingGolem = card("Howling Golem") {
    manaCost = "{3}"
    typeLine = "Artifact Creature — Golem"
    power = 2
    toughness = 3
    oracleText = "Whenever Howling Golem attacks or blocks, each player draws a card."

    triggeredAbility {
        trigger = Triggers.Attacks
        effect = Effects.DrawCards(1, EffectTarget.PlayerRef(Player.Each))
    }

    triggeredAbility {
        trigger = Triggers.Blocks
        effect = Effects.DrawCards(1, EffectTarget.PlayerRef(Player.Each))
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "218"
        artist = "Grzegorz Rutkowski"
        flavorText = "\"It wails of buried riches and the souls lost seeking them.\""
        imageUri = "https://cards.scryfall.io/normal/front/7/7/775ab60a-2dc1-44fc-8022-f7becd8ee195.jpg?1562737992"
    }
}
