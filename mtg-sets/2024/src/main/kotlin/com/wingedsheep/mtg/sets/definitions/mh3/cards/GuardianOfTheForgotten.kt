package com.wingedsheep.mtg.sets.definitions.mh3.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.predicates.StatePredicate

/**
 * Guardian of the Forgotten (MH3 #28)
 * {3}{W}
 * Creature — Elephant Warrior
 * 4/4
 *
 * Vigilance
 * Whenever a modified creature you control dies, manifest the top card of your library.
 *
 * "Modified" (CR 700.9) is [StatePredicate.IsModified] narrowing creatures you control; on a dies
 * trigger the trigger matcher answers it from the departing creature's last-known information
 * (counters, attached Equipment/Auras), so a creature that was modified as it died triggers this.
 * The reward is plain manifest (CR 701.40) via [Patterns.Library.manifest].
 */
private val modifiedCreatureYouControl = GameObjectFilter.Creature.youControl().let {
    it.copy(statePredicates = it.statePredicates + StatePredicate.IsModified)
}

val GuardianOfTheForgotten = card("Guardian of the Forgotten") {
    manaCost = "{3}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Elephant Warrior"
    power = 4
    toughness = 4
    oracleText = "Vigilance\n" +
        "Whenever a modified creature you control dies, manifest the top card of your library. " +
        "(Equipment, Auras you control, and counters are modifications. To manifest a card, put it " +
        "onto the battlefield face down as a 2/2 creature. Turn it face up any time for its mana " +
        "cost if it's a creature card.)"

    keywords(Keyword.VIGILANCE)

    triggeredAbility {
        trigger = Triggers.a(modifiedCreatureYouControl).dies()
        effect = Patterns.Library.manifest()
        description = "Whenever a modified creature you control dies, manifest the top card of your library."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "28"
        artist = "Ron Spears"
        imageUri = "https://cards.scryfall.io/normal/front/8/0/8032c909-3170-45ab-a552-a8dc683b0a6e.jpg?1783911301"
    }
}
