package com.wingedsheep.mtg.sets.definitions.msh.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.effects.SearchDestination
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * The Masters of Evil — Marvel Super Heroes #105
 * {5}{B} · Legendary Creature — Human Villain · 5/6
 *
 * Other Villains you control get +2/+1.
 * {1}{B}, Discard this card: Search your library for a Plan card, reveal it, put it into your
 * hand, then shuffle.
 *
 * The lord is the standard Layer 7c [ModifyStats] over an `excludeSelf` [GroupFilter] of Villains
 * you control (Attuma, Atlantean Warlord idiom).
 *
 * The tutor functions **from the hand**: its cost discards this card ([Costs.DiscardSelf]) and it
 * is activated from [Zone.HAND] (Stegron the Dinosaur Man idiom), so The Masters of Evil is either
 * a six-mana body or a two-mana Plan tutor. "Plan" is an enchantment subtype in this set, so the
 * search filter is a plain subtype filter over any card.
 */
val TheMastersOfEvil = card("The Masters of Evil") {
    manaCost = "{5}{B}"
    colorIdentity = "B"
    typeLine = "Legendary Creature — Human Villain"
    power = 5
    toughness = 6
    oracleText = "Other Villains you control get +2/+1.\n" +
        "{1}{B}, Discard this card: Search your library for a Plan card, reveal it, put it into " +
        "your hand, then shuffle."

    staticAbility {
        ability = ModifyStats(
            powerBonus = 2,
            toughnessBonus = 1,
            filter = GroupFilter(
                GameObjectFilter.Creature.withSubtype(Subtype.VILLAIN).youControl(),
                excludeSelf = true
            )
        )
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{1}{B}"), Costs.DiscardSelf)
        activateFromZone = Zone.HAND
        effect = Patterns.Library.searchLibrary(
            filter = GameObjectFilter.Any.withSubtype("Plan"),
            count = 1,
            destination = SearchDestination.HAND,
            reveal = true
        )
        description = "Search your library for a Plan card, reveal it, put it into your hand, then shuffle."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "105"
        artist = "Bastien L. Deharme"
        flavorText = "\"You have made many enemies, Avengers. And your enemies have made many " +
            "friends.\"\n—Baron Zemo"
        imageUri = "https://cards.scryfall.io/normal/front/6/5/65ba4439-3282-4179-85b9-67a25e2e5d24.jpg?1783902941"
    }
}
