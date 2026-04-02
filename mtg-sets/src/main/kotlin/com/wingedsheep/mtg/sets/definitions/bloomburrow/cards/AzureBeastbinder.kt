package com.wingedsheep.mtg.sets.definitions.bloomburrow.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantBeBlockedByPower
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.RemoveAllAbilitiesEffect
import com.wingedsheep.sdk.scripting.effects.SetBasePowerToughnessEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Azure Beastbinder
 * {1}{U}
 * Creature — Rat Rogue
 * 1/3
 *
 * Vigilance
 * This creature can't be blocked by creatures with power 2 or greater.
 * Whenever this creature attacks, up to one target artifact, creature, or planeswalker
 * an opponent controls loses all abilities until your next turn. If it's a creature,
 * it also has base power and toughness 2/2 until your next turn.
 *
 * Note: The "if it's a creature" P/T setting always applies because the executor
 * checks if the target is on the battlefield, and the SetBasePowerToughness floating
 * effect only affects creatures via the layer system.
 */
val AzureBeastbinder = card("Azure Beastbinder") {
    manaCost = "{1}{U}"
    typeLine = "Creature — Rat Rogue"
    power = 1
    toughness = 3
    oracleText = "Vigilance\nThis creature can't be blocked by creatures with power 2 or greater.\nWhenever this creature attacks, up to one target artifact, creature, or planeswalker an opponent controls loses all abilities until your next turn. If it's a creature, it also has base power and toughness 2/2 until your next turn."

    keywords(Keyword.VIGILANCE)

    // Can't be blocked by creatures with power 2 or greater
    staticAbility {
        ability = CantBeBlockedByPower(minPower = 2)
    }

    // Whenever this creature attacks, up to one target loses abilities + base 2/2 if creature
    triggeredAbility {
        trigger = Triggers.Attacks
        val t = target(
            "artifact, creature, or planeswalker an opponent controls",
            TargetObject(
                optional = true,
                filter = TargetFilter(
                    (GameObjectFilter.Artifact or GameObjectFilter.Creature or GameObjectFilter.Planeswalker)
                        .opponentControls()
                )
            )
        )
        effect = RemoveAllAbilitiesEffect(t, Duration.UntilYourNextTurn)
            .then(SetBasePowerToughnessEffect(t, 2, 2, Duration.UntilYourNextTurn))
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "41"
        artist = "Adam Paquette"
        imageUri = "https://cards.scryfall.io/normal/front/2/1/211af1bf-910b-41a5-b928-f378188d1871.jpg?1721426023"
    }
}
