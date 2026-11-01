package com.wingedsheep.mtg.sets.definitions.rna.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Dagger Caster
 * {3}{R}
 * Creature — Lizard Rogue
 * 2/3
 * When this creature enters, it deals 1 damage to each opponent and 1 damage to each creature your
 * opponents control.
 */
val DaggerCaster = card("Dagger Caster") {
    manaCost = "{3}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Lizard Rogue"
    power = 2
    toughness = 3
    oracleText = "When this creature enters, it deals 1 damage to each opponent and 1 damage to each creature " +
        "your opponents control."

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.DealDamage(1, EffectTarget.PlayerRef(Player.EachOpponent)) then
            Patterns.Group.dealDamageToAll(1, GroupFilter(GameObjectFilter.Creature.opponentControls()))
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "97"
        artist = "Viktor Titov"
        flavorText = "\"Keep coming. I have knives enough for everyone.\""
        imageUri = "https://cards.scryfall.io/normal/front/4/8/48a7ac34-ec4a-4571-abb1-bff1fb67c78f.jpg?1783933685"
    }
}
