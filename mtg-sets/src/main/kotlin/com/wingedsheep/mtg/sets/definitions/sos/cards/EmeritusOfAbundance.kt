package com.wingedsheep.mtg.sets.definitions.sos.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Emeritus of Abundance // Regrowth — Secrets of Strixhaven #145
 * {2}{G} · Creature — Elf Druid · 3/4
 *
 * Vigilance
 * This creature enters prepared.
 * Whenever this creature attacks, if you control eight or more lands, this creature becomes prepared.
 * (While it's prepared, you may cast a copy of its spell. Doing so unprepares it.)
 * //
 * Regrowth — {1}{G}, Sorcery: Return target card from your graveyard to your hand.
 *
 * Prepare (Secrets of Strixhaven): the creature enters with the PREPARED keyword. The attack
 * ability re-prepares it via [Effects.BecomePrepared], gated as an intervening-if (CR 603.4) on
 * controlling eight or more lands. Becoming prepared creates a copy of its prepare spell
 * ("Regrowth") in exile that its controller may cast for {1}{G}; casting that copy unprepares
 * the creature. Modeled via [com.wingedsheep.sdk.model.CardLayout.PREPARE] + the
 * `prepare(name) { }` DSL.
 */
val EmeritusOfAbundance = card("Emeritus of Abundance") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Elf Druid"
    power = 3
    toughness = 4
    oracleText = "Vigilance\n" +
        "This creature enters prepared.\n" +
        "Whenever this creature attacks, if you control eight or more lands, this creature becomes prepared. " +
        "(While it's prepared, you may cast a copy of its spell. Doing so unprepares it.)"

    keywords(Keyword.VIGILANCE, Keyword.PREPARED)

    // Whenever this creature attacks, if you control eight or more lands, it becomes prepared.
    triggeredAbility {
        trigger = Triggers.Attacks
        triggerCondition = Conditions.ControlLandsAtLeast(8)
        effect = Effects.BecomePrepared(EffectTarget.Self)
    }

    // Regrowth — the prepare spell. Return target card from your graveyard to your hand.
    prepare("Regrowth") {
        manaCost = "{1}{G}"
        typeLine = "Sorcery"
        oracleText = "Return target card from your graveyard to your hand."
        spell {
            target = TargetObject(
                filter = TargetFilter(GameObjectFilter.Any.ownedByYou(), zone = Zone.GRAVEYARD)
            )
            effect = Effects.Move(EffectTarget.ContextTarget(0), Zone.HAND)
        }
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "145"
        artist = "Justyna Dura"
        imageUri = "https://cards.scryfall.io/normal/front/a/c/ac095763-6f4e-4d4e-9c99-414646368f8d.jpg?1778165051"
    }
}
