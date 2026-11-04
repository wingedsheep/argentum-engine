package com.wingedsheep.mtg.sets.definitions.rix.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.SearchDestination
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Angrath's Fury — Rivals of Ixalan #204 (planeswalker deck)
 * {3}{B}{R} · Sorcery
 *
 * Destroy target creature. Angrath's Fury deals 3 damage to target player or planeswalker. You may
 * search your library and/or graveyard for a card named Angrath, Minotaur Pirate, reveal it, and put
 * it into your hand. If you search your library this way, shuffle.
 *
 * The search half is Vraska's Scorn's optional planeswalker-deck tutor. With one target illegal the
 * other is still affected and the search still happens; with both illegal the spell doesn't resolve.
 */
val AngrathsFury = card("Angrath's Fury") {
    manaCost = "{3}{B}{R}"
    colorIdentity = "BR"
    typeLine = "Sorcery"
    oracleText = "Destroy target creature. Angrath's Fury deals 3 damage to target player or planeswalker. " +
        "You may search your library and/or graveyard for a card named Angrath, Minotaur Pirate, reveal it, " +
        "and put it into your hand. If you search your library this way, shuffle."

    spell {
        val creature = target(TargetFilter.Creature)
        val victim = target(Targets.PlayerOrPlaneswalker)
        effect = Effects.Destroy(creature) then
            Effects.DealDamage(3, victim) then
            Patterns.Library.searchLibraryAndOrGraveyard(
                filter = GameObjectFilter.Any.named("Angrath, Minotaur Pirate"),
                destination = SearchDestination.HAND,
                reveal = true,
                optional = true
            )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "204"
        inBooster = false
        artist = "Lake Hurwitz"
        imageUri = "https://cards.scryfall.io/normal/front/7/0/708006ba-d494-4093-b108-8249b110831e.jpg?1783935256"
        ruling("2018-01-19", "You can't cast Angrath's Fury unless you choose both a target creature and a target player or planeswalker.")
        ruling("2018-01-19", "If either target becomes illegal after you cast Angrath's Fury but before it resolves, the other is still affected as appropriate and you'll search for Angrath. However, if both targets become illegal, the spell doesn't resolve and you won't search.")
    }
}
