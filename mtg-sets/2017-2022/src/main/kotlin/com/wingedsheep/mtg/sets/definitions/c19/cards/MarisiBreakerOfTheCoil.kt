package com.wingedsheep.mtg.sets.definitions.c19.cards

import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.PlayersCantCastSpells
import com.wingedsheep.sdk.scripting.events.Recipient
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Marisi, Breaker of the Coil — Commander 2019 #46
 * {1}{R}{G}{W} · Legendary Creature — Cat Warrior · 5/4
 *
 * Your opponents can't cast spells during combat.
 * Whenever a creature you control deals combat damage to a player, goad each creature that player
 * controls.
 *
 * - The prohibition is [PlayersCantCastSpells] gated on `IsInPhase(COMBAT, yoursOnly = false)` —
 *   the combat phase of any player's turn, read at cast legality, so it also overrides effects that
 *   would let an opponent cast a spell during combat (2019-08-23 ruling). Activated abilities are
 *   untouched.
 * - The trigger fires once per creature that connects; "that player" is the damaged player
 *   ([Player.TriggeringPlayer]). Goad (CR 701.15) is applied to each creature that player controls
 *   as the ability resolves via [Effects.ForEachInGroup]; group iteration keeps Marisi's controller
 *   as the goader, so the designation lasts until that player's next turn even if Marisi leaves.
 *   Creatures entering afterwards aren't goaded.
 */
val MarisiBreakerOfTheCoil = card("Marisi, Breaker of the Coil") {
    manaCost = "{1}{R}{G}{W}"
    colorIdentity = "RGW"
    typeLine = "Legendary Creature — Cat Warrior"
    power = 5
    toughness = 4
    oracleText = "Your opponents can't cast spells during combat.\n" +
        "Whenever a creature you control deals combat damage to a player, goad each creature that player " +
        "controls. (Until your next turn, those creatures attack each combat if able and attack a player " +
        "other than you if able.)"

    staticAbility {
        ability = PlayersCantCastSpells(
            affected = Player.EachOpponent,
            condition = Conditions.IsInPhase(Phase.COMBAT, yoursOnly = false),
        )
    }

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Creature.youControl()).dealsCombatDamage(Recipient.AnyPlayer)
        effect = Effects.ForEachInGroup(
            GroupFilter(GameObjectFilter.Creature.controlledByTriggeringPlayer()),
            Effects.Goad(EffectTarget.IterationEntity),
        )
        description = "Whenever a creature you control deals combat damage to a player, goad each creature " +
            "that player controls."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "46"
        artist = "Rudy Siswanto"
        flavorText = "\"Cast off the law! Awaken your rage!\""
        imageUri = "https://cards.scryfall.io/normal/front/c/3/c366e1c0-f62d-41ce-baec-de11dbc1c5f4.jpg?1783932798"
        ruling(
            "2019-08-23",
            "If an effect allows or instructs an opponent to cast a spell during a combat phase, they can't " +
                "do so. Marisi's restriction takes precedence over that permission."
        )
        ruling("2019-08-23", "Opponents can still activate abilities during combat.")
        ruling("2019-08-23", "Creatures that enter the battlefield after Marisi's second ability resolves won't be goaded.")
        ruling("2019-08-23", "The goaded creatures remain goaded even if Marisi leaves the battlefield before your next turn.")
        ruling(
            "2023-06-30",
            "Attacking with a goaded creature doesn't cause it to stop being goaded. If there is an additional " +
                "combat phase that turn, or if another player gains control of it before it stops being goaded, " +
                "it must attack again if able."
        )
    }
}
