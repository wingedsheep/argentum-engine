package com.wingedsheep.mtg.sets.definitions.bloomburrow.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AddLandTypeByCounter
import com.wingedsheep.sdk.scripting.CostReductionSource
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ReduceFirstSpellOfTypeColoredCost
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Eluge, the Shoreless Sea
 * {1}{U}{U}{U}
 * Legendary Creature — Elemental Fish
 * *|*
 *
 * Eluge's power and toughness are each equal to the number of Islands you control.
 * Whenever Eluge enters or attacks, put a flood counter on target land. It's an Island
 * in addition to its other types for as long as it has a flood counter on it.
 * The first instant or sorcery spell you cast each turn costs {U} less to cast for each
 * land you control with a flood counter on it.
 */
val ElugeTheShoreslessSea = card("Eluge, the Shoreless Sea") {
    manaCost = "{1}{U}{U}{U}"
    typeLine = "Legendary Creature — Elemental Fish"
    oracleText = "Eluge, the Shoreless Sea's power and toughness are each equal to the number of Islands you control.\nWhenever Eluge enters or attacks, put a flood counter on target land. It's an Island in addition to its other types for as long as it has a flood counter on it.\nThe first instant or sorcery spell you cast each turn costs {U} less to cast for each land you control with a flood counter on it. (Excess cost reduction reduces generic mana.)"

    // */\* = number of Islands you control
    dynamicStats(
        DynamicAmounts.battlefield(Player.You, GameObjectFilter.Land.withSubtype("Island")).count()
    )

    // Whenever Eluge enters the battlefield, put a flood counter on target land
    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        val land = target("target land", Targets.Land)
        effect = Effects.AddCounters(Counters.FLOOD, 1, land)
    }

    // Whenever Eluge attacks, put a flood counter on target land
    triggeredAbility {
        trigger = Triggers.Attacks
        val land = target("target land", Targets.Land)
        effect = Effects.AddCounters(Counters.FLOOD, 1, land)
    }

    // Lands with flood counters are Islands in addition to their other types
    staticAbility {
        ability = AddLandTypeByCounter(
            landType = "Island",
            counterType = Counters.FLOOD
        )
    }

    // First instant/sorcery each turn costs {U} less per flood-counter land you control
    staticAbility {
        ability = ReduceFirstSpellOfTypeColoredCost(
            spellFilter = GameObjectFilter.InstantOrSorcery,
            manaReductionPerUnit = "{U}",
            countSource = CostReductionSource.PermanentsWithCounterYouControl(
                filter = GameObjectFilter.Land,
                counterType = Counters.FLOOD
            )
        )
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "49"
        artist = "Chase Stone"
        imageUri = "https://cards.scryfall.io/normal/front/1/f/1f2bf6ba-cd1a-4382-9572-6dfbcf6ed0c6.jpg?1721426077"
    }
}
