package com.wingedsheep.mtg.sets.definitions.m21.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.MustBeBlocked
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Canopy Stalker
 * {3}{G}
 * Creature — Cat
 * 4/2
 *
 * This creature must be blocked if able.
 * When this creature dies, you gain 1 life for each creature that died this turn.
 *
 * "Must be blocked if able" is the at-least-one [MustBeBlocked] static (Goblin Fire Fiend). The dies
 * trigger reads the global per-turn death tally at resolution, so it counts the Stalker itself and
 * any creature that dies before it resolves (2020-06-23 ruling).
 */
val CanopyStalker = card("Canopy Stalker") {
    manaCost = "{3}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Cat"
    power = 4
    toughness = 2
    oracleText = "This creature must be blocked if able.\n" +
        "When this creature dies, you gain 1 life for each creature that died this turn."

    staticAbility {
        ability = MustBeBlocked()
    }

    triggeredAbility {
        trigger = Triggers.self.dies()
        effect = Effects.GainLife(DynamicAmounts.creaturesDiedThisTurn(Player.Each))
        description = "When this creature dies, you gain 1 life for each creature that died this turn."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "175"
        artist = "Ilse Gort"
        flavorText = "The forest's cover offers shade, shelter, and an increased risk of sudden death."
        imageUri = "https://cards.scryfall.io/normal/front/a/3/a3f6a13a-ab38-49d1-8712-f9c9135a23c8.jpg?1783930678"
        ruling(
            "2020-06-23",
            "Only one creature is required to block Canopy Stalker. Other creatures may also block it, and " +
                "are free to block other creatures or not block at all."
        )
        ruling(
            "2020-06-23",
            "If each creature the defending player controls can't block for any reason (such as being " +
                "tapped), then Canopy Stalker isn't blocked. If there's a cost associated with blocking " +
                "Canopy Stalker, the defending player isn't forced to pay that cost, so it doesn't have to be " +
                "blocked in that case either."
        )
        ruling(
            "2020-06-23",
            "Canopy Stalker's triggered ability counts itself, as well as any other creatures that die " +
                "before the triggered ability resolves."
        )
    }
}
