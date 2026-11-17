package com.wingedsheep.mtg.sets.definitions.frf.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.conditions.Exists
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Winds of Qal Sisma
 * {1}{G}
 * Instant
 * Prevent all combat damage that would be dealt this turn.
 * Ferocious — If you control a creature with power 4 or greater, instead prevent all combat damage
 * that would be dealt this turn by creatures your opponents control.
 *
 * The ferocious check is made once, as the spell resolves (2014-11-24 ruling), and picks which
 * turn-scoped shield is created. "Creatures your opponents control" is judged when the damage would
 * be dealt.
 */
val WindsOfQalSisma = card("Winds of Qal Sisma") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Instant"
    oracleText = "Prevent all combat damage that would be dealt this turn.\n" +
        "Ferocious — If you control a creature with power 4 or greater, instead prevent all combat damage that would be dealt this turn by creatures your opponents control."

    spell {
        effect = Effects.If(
            condition = Exists(Player.You, Zone.BATTLEFIELD, GameObjectFilter.Creature.powerAtLeast(4)),
            then = Effects.PreventCombatDamageFrom(GameObjectFilter.Creature.opponentControls()),
            otherwise = Effects.PreventAllCombatDamage()
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "147"
        artist = "Howard Lyon"
        imageUri = "https://cards.scryfall.io/normal/front/0/c/0c45d98d-b8bd-409d-bce9-d7d73be735a9.jpg?1783938674"
        ruling(
            "2014-11-24",
            "If you control a creature with power 4 or greater as Winds of Qal Sisma resolves, the ferocious ability will apply. Combat damage dealt by creatures your opponents control will be prevented, even if you no longer control a creature with power 4 or greater as that damage would be dealt.",
        )
    }
}
