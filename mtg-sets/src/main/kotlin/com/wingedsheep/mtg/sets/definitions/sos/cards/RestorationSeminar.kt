package com.wingedsheep.mtg.sets.definitions.sos.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Restoration Seminar
 * {5}{W}{W}
 * Sorcery — Lesson
 *
 * Return target nonland permanent card from your graveyard to the battlefield.
 * Paradigm (Then exile this spell. After you first resolve a spell with this name, you may cast a
 * copy of it from exile without paying its mana cost at the beginning of each of your first main
 * phases.)
 *
 * Reanimation that returns one *nonland* permanent card from the caster's graveyard to the
 * battlefield: bind one nonland-permanent card you own in your graveyard, then move it from the
 * graveyard to the battlefield via [Effects.Move] — the canonical reanimation shape used across the
 * corpus (Doomed Necromancer, Unburial Rites), so it matches the mtgish emitter's output.
 *
 * `paradigm()` is the SOS ability word — it tags the spell so the engine exiles it on resolution
 * and synthesizes the recurring "at the beginning of each of your first main phases, you may cast a
 * free copy from exile" ability ([com.wingedsheep.sdk.scripting.Paradigm.recastAbility]). Each
 * recast copy picks its own target at resolution against the then-current graveyard, so the
 * recurrence reanimates whatever permanent card is available that turn.
 */
val RestorationSeminar = card("Restoration Seminar") {
    manaCost = "{5}{W}{W}"
    colorIdentity = "W"
    typeLine = "Sorcery — Lesson"
    oracleText = "Return target nonland permanent card from your graveyard to the battlefield.\n" +
        "Paradigm (Then exile this spell. After you first resolve a spell with this name, you may " +
        "cast a copy of it from exile without paying its mana cost at the beginning of each of " +
        "your first main phases.)"

    spell {
        val returnTarget = target(
            "target nonland permanent card from your graveyard",
            TargetObject(
                filter = TargetFilter(
                    GameObjectFilter.NonlandPermanent.ownedByYou(),
                    zone = Zone.GRAVEYARD
                )
            )
        )
        effect = Effects.Move(returnTarget, Zone.BATTLEFIELD, fromZone = Zone.GRAVEYARD)
        paradigm()
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "30"
        artist = "Josu Hernaiz"
        imageUri = "https://cards.scryfall.io/normal/front/9/e/9ebc4ecf-2fa2-4ab8-afde-3b91cf5eadb6.jpg?1775937123"
    }
}
