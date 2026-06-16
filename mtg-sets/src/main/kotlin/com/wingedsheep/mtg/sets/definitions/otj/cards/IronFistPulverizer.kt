package com.wingedsheep.mtg.sets.definitions.otj.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Iron-Fist Pulverizer
 * {4}{R}
 * Creature — Giant Warrior
 * 4/5
 *
 * Reach
 * Whenever you cast your second spell each turn, this creature deals 2 damage to target opponent.
 * Scry 1.
 *
 * "Your second spell each turn" → [Triggers.NthSpellCast] with n = 2 scoped to [Player.You]. The
 * damage source defaults to this permanent. Scry 1 resolves after the damage as part of the same
 * resolution.
 */
val IronFistPulverizer = card("Iron-Fist Pulverizer") {
    manaCost = "{4}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Giant Warrior"
    power = 4
    toughness = 5
    oracleText = "Reach\n" +
        "Whenever you cast your second spell each turn, this creature deals 2 damage to target " +
        "opponent. Scry 1. (Look at the top card of your library. You may put that card on the bottom.)"

    keywords(Keyword.REACH)

    triggeredAbility {
        trigger = Triggers.NthSpellCast(2, Player.You)
        val opponent = target("opponent", Targets.Opponent)
        effect = Effects.Composite(
            listOf(
                Effects.DealDamage(2, opponent),
                Patterns.Library.scry(1),
            ),
        )
        description = "Whenever you cast your second spell each turn, this creature deals 2 damage " +
            "to target opponent. Scry 1."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "131"
        artist = "Xabi Gaztelua"
        flavorText = "\"The 3:10 to Omenport will be delayed.\"\n—Prosperity Station announcement"
        imageUri = "https://cards.scryfall.io/normal/front/c/d/cd7f984a-0b56-45df-958d-6178e4da61ed.jpg?1712355784"
    }
}
