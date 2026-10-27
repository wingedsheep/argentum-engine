package com.wingedsheep.mtg.sets.definitions.dgm.cards

import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CanOnlyAttackAlone
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Master of Cruelties
 * {3}{B}{R}
 * Creature — Demon
 * 1/4
 * First strike, deathtouch
 * This creature can only attack alone.
 * Whenever this creature attacks a player and isn't blocked, that player's life total becomes 1.
 * This creature assigns no combat damage this combat.
 *
 * "Attacks a player" is part of the trigger event, so it is a trigger restriction: attacking a
 * planeswalker or battle never triggers it (ruling). The attacked player is always an opponent,
 * which is what `attackingAnOpponent()` reads.
 */
val MasterOfCruelties = card("Master of Cruelties") {
    manaCost = "{3}{B}{R}"
    colorIdentity = "BR"
    typeLine = "Creature — Demon"
    oracleText = "First strike, deathtouch\nThis creature can only attack alone.\nWhenever this creature " +
        "attacks a player and isn't blocked, that player's life total becomes 1. This creature assigns no " +
        "combat damage this combat."
    power = 1
    toughness = 4

    keywords(Keyword.FIRST_STRIKE, Keyword.DEATHTOUCH)

    staticAbility {
        ability = CanOnlyAttackAlone()
    }

    triggeredAbility {
        trigger = Triggers.self.attacksAndIsntBlocked()
        triggerRestriction = Conditions.SourceMatches(GameObjectFilter.Creature.attackingAnOpponent())
        effect = Effects.SetLifeTotal(1, EffectTarget.PlayerRef(Player.DefendingPlayer)) then
            Effects.GrantKeyword(AbilityFlag.ASSIGNS_NO_COMBAT_DAMAGE, EffectTarget.Self, Duration.EndOfCombat)
        description = "Whenever this creature attacks a player and isn't blocked, that player's life total " +
            "becomes 1. This creature assigns no combat damage this combat."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "82"
        artist = "Chase Stone"
        imageUri = "https://cards.scryfall.io/normal/front/7/b/7b4d8ab5-252c-4727-817d-6f18cbaedd91.jpg?1783940026"
    }
}
