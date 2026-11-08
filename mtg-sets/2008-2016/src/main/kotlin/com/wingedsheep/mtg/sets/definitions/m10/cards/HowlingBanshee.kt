package com.wingedsheep.mtg.sets.definitions.m10.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Howling Banshee
 * {2}{B}{B}
 * Creature — Spirit
 * 3/3
 * Flying
 * When this creature enters, each player loses 3 life.
 *
 * "Each player" includes the Banshee's controller — [Player.Each], not each opponent.
 */
val HowlingBanshee = card("Howling Banshee") {
    manaCost = "{2}{B}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Spirit"
    power = 3
    toughness = 3
    oracleText = "Flying\nWhen this creature enters, each player loses 3 life."

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.LoseLife(3, EffectTarget.PlayerRef(Player.Each))
        description = "When this creature enters, each player loses 3 life."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "99"
        artist = "Andrew Robinson"
        flavorText = "Villagers cloaked the town in magical silence, but their ears still bled."
        imageUri = "https://cards.scryfall.io/normal/front/e/7/e7e38e91-696e-4bf9-b46c-c9ed85c82b19.jpg?1783942382"
    }
}
