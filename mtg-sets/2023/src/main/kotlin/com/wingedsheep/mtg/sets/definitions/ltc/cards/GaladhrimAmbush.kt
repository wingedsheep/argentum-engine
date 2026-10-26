package com.wingedsheep.mtg.sets.definitions.ltc.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Galadhrim Ambush — Tales of Middle-earth Commander #38 (canonical / earliest printing).
 * {3}{G} · Instant
 *
 * Create X 1/1 green Elf Warrior creature tokens, where X is the number of attacking creatures.
 * Prevent all combat damage that would be dealt this turn by non-Elf creatures.
 *
 * X counts every attacking creature on the battlefield, whoever controls it (the 2023-06-16
 * ruling) — Divine Retribution's tally — read on resolution, before the tokens exist. The
 * prevention is Moonmist's [Effects.PreventCombatDamageFrom] shield over non-Elf creatures, whose
 * source filter is judged as each combat damage event happens.
 */
val GaladhrimAmbush = card("Galadhrim Ambush") {
    manaCost = "{3}{G}"
    colorIdentity = "G"
    typeLine = "Instant"
    oracleText = "Create X 1/1 green Elf Warrior creature tokens, where X is the number of attacking creatures.\n" +
        "Prevent all combat damage that would be dealt this turn by non-Elf creatures."

    spell {
        effect = Effects.CreateToken(
            count = DynamicAmounts.battlefield(Player.Each, GameObjectFilter.Creature.attacking()).count(),
            power = 1,
            toughness = 1,
            colors = setOf(Color.GREEN),
            creatureTypes = setOf("Elf", "Warrior"),
            imageUri = "https://cards.scryfall.io/normal/front/0/2/023f668a-a8e6-4fcb-98d1-30adf343f050.jpg?1783916056",
        ) then Effects.PreventCombatDamageFrom(
            source = GameObjectFilter.Creature.notSubtype(Subtype.ELF),
            duration = Duration.EndOfTurn
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "38"
        artist = "John Di Giovanni"
        flavorText = "\"None of the Orcs will ever return out of Lórien.\"\n—Haldir"
        imageUri = "https://cards.scryfall.io/normal/front/e/6/e69e06f0-6142-4f80-9208-362b3e74d51d.jpg?1783916026"
        ruling(
            "2023-06-16",
            "Galadhrim Ambush counts all attacking creatures no matter which player, planeswalker, or " +
                "battle they're attacking, including attacking creatures you control if Galadhrim Ambush " +
                "is cast during your combat phase."
        )
    }
}
