package com.wingedsheep.mtg.sets.definitions.m21.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Storm Caller
 * {2}{R}
 * Creature — Ogre Shaman
 * 3/2
 * When this creature enters, it deals 2 damage to each opponent.
 */
val StormCaller = card("Storm Caller") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Ogre Shaman"
    power = 3
    toughness = 2
    oracleText = "When this creature enters, it deals 2 damage to each opponent."

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.DealDamage(2, EffectTarget.PlayerRef(Player.EachOpponent))
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "335"
        artist = "Manuel Castañón"
        flavorText = "He became elder shaman when his predecessor was mysteriously struck by lightning on a cloudless day."
        imageUri = "https://cards.scryfall.io/normal/front/1/b/1b6d123c-c16d-43fa-b183-8a59c7e1a11e.jpg?1783930617"
        ruling("2020-06-23", "In a Two-Headed Giant game, Storm Caller's ability causes the opposing team to lose 4 life.")
    }
}
