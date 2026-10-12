package com.wingedsheep.mtg.sets.definitions.iko.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CompanionCondition
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/** Cat, Elemental, Nightmare, Dinosaur, or Beast — the five types Kaheera reads twice. */
private val kaheeraTypes = listOf("Cat", "Elemental", "Nightmare", "Dinosaur", "Beast").map(::Subtype)

/** Each other creature you control that's one of [kaheeraTypes] — once, however many it has (ruling). */
private val kaheeraKin = GroupFilter(
    GameObjectFilter.Creature.withAnyOfSubtypes(kaheeraTypes).youControl(),
    excludeSelf = true,
)

/**
 * Kaheera, the Orphanguard — Ikoria: Lair of Behemoths #224
 * {1}{G/W}{G/W} · Legendary Creature — Cat Beast · 3/2
 *
 * Companion — Each creature card in your starting deck is a Cat, Elemental, Nightmare, Dinosaur,
 * or Beast card: every card is a noncreature card or has one of the five types (CR 702.139).
 */
val KaheeraTheOrphanguard = card("Kaheera, the Orphanguard") {
    manaCost = "{1}{G/W}{G/W}"
    typeLine = "Legendary Creature — Cat Beast"
    power = 3
    toughness = 2
    oracleText = "Companion — Each creature card in your starting deck is a Cat, Elemental, Nightmare, " +
        "Dinosaur, or Beast card. (If this card is your chosen companion, you may put it into your hand " +
        "from outside the game for {3} as a sorcery.)\n" +
        "Vigilance\n" +
        "Each other creature you control that's a Cat, Elemental, Nightmare, Dinosaur, or Beast gets " +
        "+1/+1 and has vigilance."

    keywordAbility(
        KeywordAbility.companion(
            CompanionCondition.EachCard(
                GameObjectFilter.Noncreature or GameObjectFilter.Any.withAnyOfSubtypes(kaheeraTypes)
            )
        )
    )
    keywords(Keyword.VIGILANCE)

    staticAbility {
        ability = ModifyStats(powerBonus = 1, toughnessBonus = 1, filter = kaheeraKin)
    }
    staticAbility {
        ability = GrantKeyword(Keyword.VIGILANCE, kaheeraKin)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "224"
        artist = "Ryan Pancoast"
        imageUri = "https://cards.scryfall.io/normal/front/d/4/d4ebed0b-8060-4a7b-a060-5cfcd2172b16.jpg?1783931013"
        ruling("2020-04-17", "Creature cards in your deck may be a mix of the five creature types that Kaheera cares about. They may also have additional types, as long as each creature card is at least one of those five. Noncreature cards may have any subtypes.")
        ruling("2020-04-17", "A creature that's more than one of the five types gets +1/+1 only once from Kaheera's last ability.")
    }
}
