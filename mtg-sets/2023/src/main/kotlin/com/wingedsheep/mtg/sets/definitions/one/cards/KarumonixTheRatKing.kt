package com.wingedsheep.mtg.sets.definitions.one.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.effects.SelectionMode

/**
 * Karumonix, the Rat King
 * {1}{B}{B}
 * Legendary Creature — Phyrexian Rat
 * 3/3
 * Toxic 1
 * Other Rats you control have toxic 1.
 * When Karumonix enters, look at the top five cards of your library. You may reveal any number of
 * Rat cards from among them and put the revealed cards into your hand. Put the rest on the bottom
 * of your library in a random order.
 *
 * The lord grant is the `TOXIC_<n>` string keyword that `Effects.GrantToxic` uses, carried by a
 * static [GrantKeyword]; instances are cumulative (CR 702.164b), so a printed-toxic Rat deals its
 * own toxic plus one.
 */
val KarumonixTheRatKing = card("Karumonix, the Rat King") {
    manaCost = "{1}{B}{B}"
    typeLine = "Legendary Creature — Phyrexian Rat"
    power = 3
    toughness = 3
    oracleText = "Toxic 1 (Players dealt combat damage by this creature also get a poison counter.)\n" +
        "Other Rats you control have toxic 1.\n" +
        "When Karumonix enters, look at the top five cards of your library. You may reveal any number " +
        "of Rat cards from among them and put the revealed cards into your hand. Put the rest on the " +
        "bottom of your library in a random order."

    keywordAbility(KeywordAbility.Numeric(Keyword.TOXIC, 1))

    staticAbility {
        ability = GrantKeyword(
            "TOXIC_1",
            GroupFilter(GameObjectFilter.Creature.withSubtype(Subtype.RAT).youControl()).other()
        )
    }

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Patterns.Library.lookAtTopAndTakeMatching(
            count = DynamicAmounts.fixed(5),
            filter = GameObjectFilter.Any.withSubtype(Subtype.RAT),
            prompt = "You may reveal any number of Rat cards and put them into your hand",
            selection = SelectionMode.ChooseAnyNumber,
            keepRevealed = true,
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "98"
        artist = "Helge C. Balzer"
        imageUri = "https://cards.scryfall.io/normal/front/f/1/f16e282d-8941-46c7-a974-c05b6f73c964.jpg?1783918045"
        ruling(
            "2023-02-04",
            "Multiple instances of toxic are cumulative. For example, if a creature has toxic 2 and gains toxic 1 due to another effect, combat damage that creature deals to a player will cause that player to get 3 poison counters."
        )
    }
}
