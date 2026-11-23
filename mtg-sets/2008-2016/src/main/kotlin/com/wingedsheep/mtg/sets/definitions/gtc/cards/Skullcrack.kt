package com.wingedsheep.mtg.sets.definitions.gtc.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Skullcrack
 * {1}{R}
 * Instant
 * Players can't gain life this turn. Damage can't be prevented this turn. Skullcrack deals 3 damage
 * to target player or planeswalker.
 *
 * Every player is life-gain-locked for the rest of the turn ([Effects.LockLifeGain] over
 * [Player.Each], Call In a Professional's shape), then the turn-scoped "can't be prevented" goes up
 * before the damage so it covers Skullcrack's own 3 too (Wild Slash's ordering).
 */
val Skullcrack = card("Skullcrack") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Instant"
    oracleText = "Players can't gain life this turn. Damage can't be prevented this turn. " +
        "Skullcrack deals 3 damage to target player or planeswalker."

    spell {
        val t = target(Targets.PlayerOrPlaneswalker)
        effect = Effects.LockLifeGain(EffectTarget.PlayerRef(Player.Each), Duration.EndOfTurn) then
            Effects.DamageCantBePreventedThisTurn() then
            Effects.DealDamage(3, t)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "106"
        artist = "Dave Kendall"
        flavorText = "The lawmage's argument was clever and well reasoned, but Blunk's response proved irrefutable."
        imageUri = "https://cards.scryfall.io/normal/front/8/0/8068a146-f6fe-46f3-a42e-822fbc3502e6.jpg?1783940122"
        ruling(
            "2024-01-12",
            "Skullcrack targets only the player or planeswalker. If that player or planeswalker is an illegal target when Skullcrack tries to resolve, it won't resolve and none of its effects will happen.",
        )
        ruling(
            "2024-01-12",
            "Spells and abilities that would cause a player to gain life or that would prevent damage still resolve, but the life-gain and damage-prevention parts have no effect.",
        )
        ruling(
            "2024-01-12",
            "Effects that would replace gaining life with another effect won't apply because it's impossible for players to gain life.",
        )
        ruling(
            "2024-01-12",
            "If an effect says to set a player's life total to a certain number and that number is higher than the player's current life total, that part of the effect won't do anything. (If the number is lower than the player's current life total, the effect will work as normal.)",
        )
    }
}
