package com.wingedsheep.mtg.sets.definitions.woe.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.model.Rarity

/**
 * Conceited Witch // Price of Beauty
 * {2}{B}
 * Creature — Human Warlock
 * 2/3
 * Menace
 *
 * Adventure: Price of Beauty — {B}, Sorcery — Adventure
 * Create a Wicked Role token attached to target creature you control.
 * (Enchanted creature gets +1/+1. When this Role is put into a graveyard, each opponent loses 1 life.)
 *
 * The same shape as [BesottedKnight] — a vanilla-plus body whose Adventure is a one-line Role maker —
 * differing only in which Role it hands out. `Effects.CreateRoleToken` owns the whole Role ruleset
 * (CR 113.2c: Roles are Auras; the "if you control another Role on it, put that one into the
 * graveyard" state-based action in CR 704.5r), so nothing here needs to special-case Wicked.
 *
 * The Adventure targets `Targets.CreatureYouControl`, matching the oracle's "target creature you
 * control" — an opponent's creature is not a legal target, and the spell is countered on resolution
 * if the only target has left or become illegal (CR 608.2b).
 */
val ConceitedWitch = card("Conceited Witch") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Human Warlock"
    oracleText = "Menace (This creature can't be blocked except by two or more creatures.)"
    power = 2
    toughness = 3

    keywords(Keyword.MENACE)

    adventure("Price of Beauty") {
        manaCost = "{B}"
        typeLine = "Sorcery — Adventure"
        oracleText = "Create a Wicked Role token attached to target creature you control. " +
            "(If you control another Role on it, put that one into the graveyard. Enchanted creature " +
            "gets +1/+1. When this Role is put into a graveyard, each opponent loses 1 life.) " +
            "(Then exile this card. You may cast the creature later from exile.)"
        spell {
            val t = target("target", Targets.CreatureYouControl)
            effect = Effects.CreateRoleToken("Wicked Role", t)
        }
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "84"
        artist = "Anna Pavleeva"
        flavorText = "She wasn't much for self-reflection."
        imageUri = "https://cards.scryfall.io/normal/front/f/8/f8a0c0f6-fef9-42c5-934d-a2855c11b440.jpg?1783915109"
    }
}
