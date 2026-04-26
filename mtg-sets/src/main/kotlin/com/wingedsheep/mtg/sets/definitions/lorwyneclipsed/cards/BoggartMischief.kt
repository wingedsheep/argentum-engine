package com.wingedsheep.mtg.sets.definitions.lorwyneclipsed.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.EffectPatterns
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameEvent.ZoneChangeEvent
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TriggerBinding
import com.wingedsheep.sdk.scripting.TriggerSpec
import com.wingedsheep.sdk.scripting.effects.CompositeEffect
import com.wingedsheep.sdk.scripting.effects.MayEffect
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Boggart Mischief
 * {2}{B}
 * Kindred Enchantment — Goblin
 *
 * When this enchantment enters, you may blight 1. If you do, create two 1/1 black
 * and red Goblin creature tokens.
 * Whenever a Goblin creature you control dies, each opponent loses 1 life and you
 * gain 1 life.
 */
val BoggartMischief = card("Boggart Mischief") {
    manaCost = "{2}{B}"
    typeLine = "Kindred Enchantment — Goblin"
    oracleText = "When this enchantment enters, you may blight 1. If you do, create two 1/1 black and red Goblin creature tokens. " +
        "(To blight 1, put a -1/-1 counter on a creature you control.)\n" +
        "Whenever a Goblin creature you control dies, each opponent loses 1 life and you gain 1 life."

    // When this enchantment enters, you may blight 1. If you do, create two 1/1 black and red Goblin creature tokens.
    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = MayEffect(
            CompositeEffect(
                listOf(
                    EffectPatterns.blight(1),
                    Effects.CreateToken(
                        power = 1,
                        toughness = 1,
                        colors = setOf(Color.BLACK, Color.RED),
                        creatureTypes = setOf("Goblin"),
                        count = 2,
                        imageUri = "https://cards.scryfall.io/normal/front/6/1/6139a45d-ebc7-4bca-8c13-73c85ea5fe0d.jpg?1768367480"
                    )
                )
            )
        )
    }

    // Whenever a Goblin creature you control dies, each opponent loses 1 life and you gain 1 life.
    triggeredAbility {
        trigger = TriggerSpec(
            event = ZoneChangeEvent(
                filter = GameObjectFilter.Creature.youControl().withSubtype("Goblin"),
                from = Zone.BATTLEFIELD,
                to = Zone.GRAVEYARD
            ),
            binding = TriggerBinding.ANY
        )
        effect = Effects.LoseLife(1, EffectTarget.PlayerRef(Player.EachOpponent)) then
            Effects.GainLife(1)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "92"
        artist = "Ron Spears"
        imageUri = "https://cards.scryfall.io/normal/front/a/a/aaeb9c9c-0f15-49dc-ae6d-2a958680f327.jpg?1767659614"
        ruling(
            "2025-11-17",
            "If Boggart Mischief is put into a graveyard from the battlefield at the same time as one or more " +
                "Goblin creatures you control die, its last ability will trigger for each of those Goblin creatures."
        )
    }
}
