package com.wingedsheep.mtg.sets.definitions.lrw.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Pollen Lullaby
 * {1}{W}
 * Instant
 *
 * Prevent all combat damage that would be dealt this turn. Clash with an opponent. If you win,
 * creatures that player controls don't untap during the player's next untap step.
 *
 * The fog resolves unconditionally and first — a lost clash still prevents the damage — so it sits
 * ahead of [Patterns.Mechanic.clash] rather than on either of its legs.
 *
 * "That player" is the *clash* opponent, not a target: the card chooses nobody, and
 * [Patterns.Mechanic.clash] writes the chosen opponent into the source's durable `OPPONENT` slot,
 * which [Player.ChosenOpponent] reads back. Using the chosen slot rather than a fresh
 * `Chooser.Opponent` is what keeps a multiplayer clash from freezing a player who never clashed.
 * `permanents = Creature` because the printed clause names creatures only — the Exhaustion/Blinding
 * Beam axis on [SkipUntapEffect], not a narrower effect.
 */
val PollenLullaby = card("Pollen Lullaby") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Instant"
    oracleText = "Prevent all combat damage that would be dealt this turn. Clash with an opponent. " +
        "If you win, creatures that player controls don't untap during the player's next untap step. " +
        "(Each clashing player reveals the top card of their library, then puts that card on their " +
        "choice of the top or bottom. A player wins if their card had a greater mana value.)"

    spell {
        effect = Effects.PreventAllCombatDamage() then
            Patterns.Mechanic.clash(
                Effects.SkipUntap(
                    target = EffectTarget.PlayerRef(Player.ChosenOpponent),
                    permanents = GameObjectFilter.Creature
                )
            )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "36"
        artist = "Warren Mahy"
        imageUri = "https://cards.scryfall.io/normal/front/d/e/deb2156a-8a77-4954-b557-1bdaf3ba171a.jpg?1783942910"
    }
}
