package com.wingedsheep.mtg.sets.definitions.spm.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CastSpellTypesFromTopOfLibrary
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.LookAtTopOfLibrary
import com.wingedsheep.sdk.scripting.effects.MayEffect

/**
 * Madame Web, Clairvoyant
 * {4}{U}{U}
 * Legendary Creature — Mutant Advisor
 * 4/4
 * You may look at the top card of your library any time.
 * You may cast Spider spells and noncreature spells from the top of your library.
 * Whenever you attack, you may mill a card. (You may put the top card of your library into your graveyard.)
 *
 * Modeled after Precognition Field: [LookAtTopOfLibrary] grants the private top-card peek, and
 * [CastSpellTypesFromTopOfLibrary] grants casting (but not land play — a land is not a spell) of the
 * top card when it is a Spider spell OR any noncreature spell. The attack trigger is player-level
 * ("Whenever you attack" = [Triggers.YouAttack]) with an optional mill of one.
 */
val MadameWebClairvoyant = card("Madame Web, Clairvoyant") {
    manaCost = "{4}{U}{U}"
    colorIdentity = "U"
    typeLine = "Legendary Creature — Mutant Advisor"
    power = 4
    toughness = 4
    oracleText = "You may look at the top card of your library any time.\n" +
        "You may cast Spider spells and noncreature spells from the top of your library.\n" +
        "Whenever you attack, you may mill a card. (You may put the top card of your library into your graveyard.)"

    staticAbility {
        ability = LookAtTopOfLibrary
    }

    staticAbility {
        ability = CastSpellTypesFromTopOfLibrary(
            filter = GameObjectFilter.Any.withSubtype(Subtype.SPIDER) or GameObjectFilter.Noncreature
        )
    }

    triggeredAbility {
        trigger = Triggers.YouAttack
        effect = MayEffect(Patterns.Library.mill(1))
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "36"
        artist = "Pavel Kolomeyets"
        imageUri = "https://cards.scryfall.io/normal/front/a/1/a16ca3fc-3cdf-4333-93e0-524afafe367b.jpg?1783905353"
    }
}
