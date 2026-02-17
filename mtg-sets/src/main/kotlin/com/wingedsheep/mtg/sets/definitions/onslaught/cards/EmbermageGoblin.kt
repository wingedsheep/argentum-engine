package com.wingedsheep.mtg.sets.definitions.onslaught.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.DealDamageEffect
import com.wingedsheep.sdk.scripting.EffectTarget
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.SearchDestination
import com.wingedsheep.sdk.scripting.MayEffect
import com.wingedsheep.sdk.dsl.Effects

/**
 * Embermage Goblin
 * {3}{R}
 * Creature — Goblin Wizard
 * 1/1
 * When Embermage Goblin enters the battlefield, you may search your library
 * for a card named Embermage Goblin, reveal it, and put it into your hand.
 * If you do, shuffle your library.
 * {T}: Embermage Goblin deals 1 damage to any target.
 */
val EmbermageGoblin = card("Embermage Goblin") {
    manaCost = "{3}{R}"
    typeLine = "Creature — Goblin Wizard"
    power = 1
    toughness = 1
    oracleText = "When Embermage Goblin enters the battlefield, you may search your library for a card named Embermage Goblin, reveal it, and put it into your hand. If you do, shuffle your library.\n{T}: Embermage Goblin deals 1 damage to any target."

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = MayEffect(
            Effects.SearchLibrary(
                filter = GameObjectFilter.Any.named("Embermage Goblin"),
                count = 1,
                destination = SearchDestination.HAND,
                reveal = true,
                shuffle = true
            )
        )
    }

    activatedAbility {
        cost = Costs.Tap
        target = Targets.Any
        effect = DealDamageEffect(1, EffectTarget.ContextTarget(0))
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "200"
        artist = "Pete Venters"
        flavorText = "Skirk Ridge goblins are known to have short tempers and even shorter fuses."
        imageUri = "https://cards.scryfall.io/large/front/f/5/f50f60a8-e99a-4891-b474-a21abee38970.jpg?1643753588"
    }
}
