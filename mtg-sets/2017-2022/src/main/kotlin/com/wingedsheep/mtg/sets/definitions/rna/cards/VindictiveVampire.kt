package com.wingedsheep.mtg.sets.definitions.rna.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Vindictive Vampire — Ravnica Allegiance #90
 * {3}{B} · Creature — Vampire · 2 / 3
 */
val VindictiveVampire = card("Vindictive Vampire") {
    manaCost = "{3}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Vampire"
    power = 2
    toughness = 3
    oracleText = "Whenever another creature you control dies, this creature deals 1 damage to each opponent and you gain 1 life."

    triggeredAbility {
        trigger = Triggers.another(GameObjectFilter.Creature.youControl()).dies()
        effect = Effects.DealDamage(1, EffectTarget.PlayerRef(Player.EachOpponent)) then Effects.GainLife(1)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "90"
        artist = "Randy Gallegos"
        flavorText = "In theory, the Guildpact keeps guild feuds from spiraling out of control. " +
            "Personal feuds, however, are not covered."
        imageUri = "https://cards.scryfall.io/normal/front/7/c/7c0bfb47-c753-42fa-969b-7b10b87b0462.jpg?1783933686"
    }
}
