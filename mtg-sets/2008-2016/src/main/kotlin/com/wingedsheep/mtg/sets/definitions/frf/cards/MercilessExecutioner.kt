package com.wingedsheep.mtg.sets.definitions.frf.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Merciless Executioner
 * {2}{B}
 * Creature — Orc Warrior
 * 3/1
 *
 * When this creature enters, each player sacrifices a creature of their choice.
 *
 * "Each player" includes you, and the Executioner itself is a legal choice for its controller.
 */
val MercilessExecutioner = card("Merciless Executioner") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Orc Warrior"
    oracleText = "When this creature enters, each player sacrifices a creature of their choice."
    power = 3
    toughness = 1

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.Sacrifice(
            GameObjectFilter.Creature,
            1,
            EffectTarget.PlayerRef(Player.Each)
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "76"
        artist = "David Palumbo"
        flavorText = "He enjoys his work, for he sees only the worst Abzan criminals: those who betray their own kin."
        imageUri = "https://cards.scryfall.io/normal/front/7/c/7c588d7a-e352-40a0-9fc7-55c7b4bfd013.jpg?1783938694"
        ruling("2014-11-24", "If you control no other creatures as Merciless Executioner's ability resolves, you'll have to sacrifice Merciless Executioner.")
    }
}
