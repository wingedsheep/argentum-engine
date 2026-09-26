package com.wingedsheep.mtg.sets.definitions.chk.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Tide of War
 * {4}{R}{R}
 * Enchantment
 * Whenever one or more creatures block, flip a coin. If you win the flip, each blocking creature
 * is sacrificed by its controller. If you lose the flip, each blocked creature is sacrificed by
 * its controller.
 *
 * A batch block trigger: once per block declaration, and only when at least one creature blocks.
 * Tide of War's controller always flips. The sacrifices read combat status at resolution, so
 * unblocked attackers are untouched, and a blocked attacker stays blocked after its blockers are
 * sacrificed.
 */
val TideOfWar = card("Tide of War") {
    manaCost = "{4}{R}{R}"
    colorIdentity = "R"
    typeLine = "Enchantment"
    oracleText = "Whenever one or more creatures block, flip a coin. If you win the flip, each blocking creature " +
        "is sacrificed by its controller. If you lose the flip, each blocked creature is sacrificed by its controller."

    triggeredAbility {
        trigger = Triggers.oneOrMore(GameObjectFilter.Creature).block()
        effect = Effects.FlipCoin(
            wonEffect = Effects.SacrificeAll(GameObjectFilter.Creature.blocking()),
            lostEffect = Effects.SacrificeAll(GameObjectFilter.Creature.blocked()),
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "194"
        artist = "Wayne Reynolds"
        imageUri = "https://cards.scryfall.io/normal/front/4/3/43b5bd60-87ed-41d2-bbcb-bd4a40a772c9.jpg?1783944293"
    }
}
