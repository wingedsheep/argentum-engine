package com.wingedsheep.mtg.sets.definitions.sos.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Echocasting Symposium — Secrets of Strixhaven #44
 * {4}{U}{U} · Sorcery — Lesson
 *
 * Target player creates a token that's a copy of target creature you control.
 * Paradigm (Then exile this spell. After you first resolve a spell with this name, you may cast
 *   a copy of it from exile without paying its mana cost at the beginning of each of your first
 *   main phases.)
 *
 * Two targets: the player who creates (and controls) the token, and the creature you control to
 * copy. `CreateTokenCopyOfTarget(target = <creature>, controller = <player>)` copies the chosen
 * creature but puts the token under the chosen player's control. `paradigm()` exiles the spell on
 * resolution and arms the recurring free-cast copy.
 */
val EchocastingSymposium = card("Echocasting Symposium") {
    manaCost = "{4}{U}{U}"
    colorIdentity = "U"
    typeLine = "Sorcery — Lesson"
    oracleText = "Target player creates a token that's a copy of target creature you control.\n" +
        "Paradigm (Then exile this spell. After you first resolve a spell with this name, you may " +
        "cast a copy of it from exile without paying its mana cost at the beginning of each of your " +
        "first main phases.)"

    spell {
        val player = target("target player", Targets.Player)
        val creature = target("target creature you control", Targets.CreatureYouControl)
        effect = Effects.CreateTokenCopyOfTarget(
            target = creature,
            controller = player,
        )
        paradigm()
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "44"
        artist = "Fajareka Setiawan"
        imageUri = "https://cards.scryfall.io/normal/front/5/d/5d7086a7-dc42-468a-a2cf-a6f89030f947.jpg?1775937216"
    }
}
