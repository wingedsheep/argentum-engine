package com.wingedsheep.mtg.sets.definitions.isd.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.EntersWithCharacteristics
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Grimoire of the Dead — Innistrad #226
 * {4} · Legendary Artifact — Book
 * {1}, {T}, Discard a card: Put a study counter on Grimoire of the Dead.
 * {T}, Remove three study counters from Grimoire of the Dead and sacrifice it: Put all creature
 * cards from all graveyards onto the battlefield under your control. They're black Zombies in
 * addition to their other colors and types.
 *
 * - The charge ability is Hellion Crucible's shape: a composite cost that adds a counter to itself.
 * - The payoff gathers every creature card in every graveyard (artifact creatures included — the
 *   ruling) and puts them onto the battlefield together under the ability's controller (the move's
 *   default), each entering already a black Zombie in addition to its other colors and types
 *   ([EntersWithCharacteristics], Grave Betrayal's rider). The rider has no duration, so it lasts
 *   while each creature stays on the battlefield.
 * - The Grimoire is sacrificed as part of the cost, so it is in the graveyard but isn't a creature
 *   card and never returns with them.
 */
val GrimoireOfTheDead = card("Grimoire of the Dead") {
    manaCost = "{4}"
    colorIdentity = ""
    typeLine = "Legendary Artifact — Book"
    oracleText = "{1}, {T}, Discard a card: Put a study counter on Grimoire of the Dead.\n" +
        "{T}, Remove three study counters from Grimoire of the Dead and sacrifice it: Put all creature " +
        "cards from all graveyards onto the battlefield under your control. They're black Zombies in " +
        "addition to their other colors and types."

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{1}"), Costs.Tap, Costs.Discard())
        effect = Effects.AddCounters(CounterType.STUDY, 1, EffectTarget.Self)
        description = "{1}, {T}, Discard a card: Put a study counter on Grimoire of the Dead."
    }

    activatedAbility {
        cost = Costs.Composite(
            Costs.Tap,
            Costs.RemoveCounterFromSelf(CounterType.STUDY, 3),
            Costs.SacrificeSelf,
        )
        effect = Effects.Pipeline {
            val creatureCards = gather(
                CardSource.FromZone(
                    zone = Zone.GRAVEYARD,
                    player = Player.Each,
                    filter = GameObjectFilter.Creature
                )
            )
            move(
                creatureCards,
                CardDestination.ToZone(Zone.BATTLEFIELD),
                entersWith = EntersWithCharacteristics(
                    addedSubtypes = setOf(Subtype.ZOMBIE),
                    addedColors = setOf(Color.BLACK),
                ),
            )
        }
        description = "{T}, Remove three study counters from Grimoire of the Dead and sacrifice it: Put all " +
            "creature cards from all graveyards onto the battlefield under your control. They're black " +
            "Zombies in addition to their other colors and types."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "226"
        artist = "Steven Belledin"
        imageUri = "https://cards.scryfall.io/normal/front/d/2/d268d078-b854-47c1-bc7f-7698723405a2.jpg?1783940901"
        ruling("2011-09-22", "\"Creature cards\" includes each card with the type creature, even if it has additional types, such as artifact.")
    }
}
