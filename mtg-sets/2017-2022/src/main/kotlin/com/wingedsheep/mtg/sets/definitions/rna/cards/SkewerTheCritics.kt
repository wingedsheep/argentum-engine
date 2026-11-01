package com.wingedsheep.mtg.sets.definitions.rna.cards

import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.SelfAlternativeCost

/**
 * Skewer the Critics
 * {2}{R}
 * Sorcery
 * Spectacle {R} (You may cast this spell for its spectacle cost rather than its mana cost if an
 * opponent lost life this turn.)
 * Skewer the Critics deals 3 damage to any target.
 *
 * Spectacle is a [SelfAlternativeCost] gated on [Conditions.OpponentLostLifeThisTurn], as on
 * Light Up the Stage.
 */
val SkewerTheCritics = card("Skewer the Critics") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Sorcery"
    oracleText = "Spectacle {R} (You may cast this spell for its spectacle cost rather than its mana cost " +
        "if an opponent lost life this turn.)\n" +
        "Skewer the Critics deals 3 damage to any target."

    selfAlternativeCost = SelfAlternativeCost(
        manaCost = ManaCost.parse("{R}"),
        condition = Conditions.OpponentLostLifeThisTurn,
    )

    spell {
        val t = target(Targets.Any)
        effect = Effects.DealDamage(3, t)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "115"
        artist = "Heonhwa"
        flavorText = "Going to a Rakdos show is dangerous. Trying to leave early can be fatal."
        imageUri = "https://cards.scryfall.io/normal/front/9/7/97295660-6bea-46ae-9a3b-0fc6abba407f.jpg?1783933675"
        ruling("2024-01-12", "Damage dealt to a player causes that player to lose that much life.")
        ruling("2024-01-12", "Spectacle cares only that an opponent lost life during the turn, not that the opponent's life total is currently lower than it was. For example, if an opponent loses 1 life and then gains 2 life in the same turn, you can cast a spell for its spectacle cost that turn.")
        ruling("2024-01-12", "Spectacle doesn't change when you can cast the spell. For example, you can't cast a sorcery with spectacle during an opponent's turn unless another effect allows you to do so, even if that player has lost life this turn.")
        ruling("2024-01-12", "To determine the total cost of a spell, start with the mana cost or alternative cost you're paying (such as a spectacle cost), add any cost increases, then apply any cost reductions. The mana value of the spell remains unchanged, no matter what the total cost to cast it was.")
        ruling("2024-01-12", "A card's spectacle cost is the same no matter how much life your opponents lost or how many opponents lost life.")
        ruling("2024-01-12", "In a multiplayer game, if an opponent loses life and later that turn leaves the game, you can cast a spell for its spectacle cost. (If a player leaves the game during their turn, that turn continues without an active player.)")
    }
}
