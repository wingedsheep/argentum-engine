package com.wingedsheep.mtg.sets.definitions.one.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Charforger
 * {1}{B}{R}
 * Creature — Phyrexian Beast
 * 2/3
 *
 * When this creature enters, create a 1/1 red Phyrexian Goblin creature token.
 * Whenever another creature or artifact you control is put into a graveyard from the battlefield,
 * put an oil counter on this creature.
 * Remove three oil counters from this creature: Exile the top card of your library. You may play
 * that card this turn.
 */
val Charforger = card("Charforger") {
    manaCost = "{1}{B}{R}"
    colorIdentity = "BR"
    typeLine = "Creature — Phyrexian Beast"
    power = 2
    toughness = 3
    oracleText = "When this creature enters, create a 1/1 red Phyrexian Goblin creature token.\n" +
        "Whenever another creature or artifact you control is put into a graveyard from the " +
        "battlefield, put an oil counter on this creature.\n" +
        "Remove three oil counters from this creature: Exile the top card of your library. " +
        "You may play that card this turn."

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.CreateToken(
            power = 1,
            toughness = 1,
            colors = setOf(Color.RED),
            creatureTypes = setOf("Phyrexian", "Goblin"),
            imageUri = "https://cards.scryfall.io/normal/front/3/6/3663e79b-2bf9-44af-a638-c0ad9067d8d4.jpg?1783918169",
        )
    }

    triggeredAbility {
        trigger = Triggers.another(GameObjectFilter.CreatureOrArtifact.youControl()).dies()
        effect = Effects.AddCounters(CounterType.OIL, 1, EffectTarget.Self)
    }

    activatedAbility {
        cost = Costs.RemoveCounterFromSelf(CounterType.OIL, 3)
        effect = Patterns.Exile.impulse(count = 1)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "199"
        artist = "Svetlin Velinov"
        imageUri = "https://cards.scryfall.io/normal/front/4/5/45db6b46-c4b4-4684-8a49-53a64ffc82f2.jpg?1783918003"
    }
}
