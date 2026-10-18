package com.wingedsheep.mtg.sets.definitions.neo.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.effects.SearchDestination
import com.wingedsheep.sdk.scripting.events.Recipient
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.predicates.StatePredicate

/**
 * Kodama of the West Tree — Kamigawa: Neon Dynasty #199 (canonical printing)
 * {2}{G} · Legendary Creature — Spirit · 3/3
 *
 * Reach. Modified creatures you control have trample. Whenever a modified creature you control
 * deals combat damage to a player, search your library for a basic land card, put it onto the
 * battlefield tapped, then shuffle.
 *
 * "Modified" is [StatePredicate.IsModified] (CR 700.9: a counter, an Equipment, or an Aura its
 * controller controls). The trample grant is a group static re-evaluated continuously in
 * projection; the land search is an observer trigger that fires once per modified creature that
 * connects, the Kodama included when it is itself modified.
 */
val KodamaOfTheWestTree = card("Kodama of the West Tree") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Legendary Creature — Spirit"
    oracleText = "Reach\n" +
        "Modified creatures you control have trample. (Equipment, Auras you control, and counters are modifications.)\n" +
        "Whenever a modified creature you control deals combat damage to a player, search your library for a " +
        "basic land card, put it onto the battlefield tapped, then shuffle."
    power = 3
    toughness = 3

    keywords(Keyword.REACH)

    staticAbility {
        ability = GrantKeyword(
            Keyword.TRAMPLE,
            GroupFilter(GameObjectFilter.Creature.youControl().withStatePredicate(StatePredicate.IsModified)),
        )
    }

    triggeredAbility {
        trigger = Triggers.a(
            GameObjectFilter.Creature.youControl().withStatePredicate(StatePredicate.IsModified)
        ).dealsCombatDamage(Recipient.AnyPlayer)
        effect = Patterns.Library.searchLibrary(
            filter = GameObjectFilter.BasicLand,
            destination = SearchDestination.BATTLEFIELD,
            entersTapped = true,
        )
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "199"
        artist = "Daarken"
        imageUri = "https://cards.scryfall.io/normal/front/e/f/ef1e1dff-b559-441d-8df3-b6a418066aca.jpg?1783923845"
        ruling("2022-02-18", "An Aura controlled by an opponent does not cause a creature you control to be modified.")
        ruling("2022-02-18", "A creature with a counter on it is considered modified no matter what kind of counter it is or which player put it on that creature.")
        ruling("2022-02-18", "A creature that is equipped is considered modified no matter who controls the Equipment that's attached to it.")
    }
}
