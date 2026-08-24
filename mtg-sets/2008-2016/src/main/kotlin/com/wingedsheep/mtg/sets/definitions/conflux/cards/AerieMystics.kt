package com.wingedsheep.mtg.sets.definitions.conflux.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Aerie Mystics
 * {4}{W}
 * Creature — Bird Wizard
 * 3/3
 * Flying
 * {1}{G}{U}: Creatures you control gain shroud until end of turn. (They can't be the targets of spells or abilities.)
 *
 * Flying is a printed [Keyword]. The activated ability is one [Patterns.Group.grantKeywordToAll]
 * over [Filters.Group.creaturesYouControl] — a single `ForEachInGroup` that names the group once
 * and grants [Keyword.SHROUD] to each member; the pattern's default duration is already
 * `Duration.EndOfTurn`, so the printed "until end of turn" needs no argument of its own.
 */
val AerieMystics = card("Aerie Mystics") {
    manaCost = "{4}{W}"
    colorIdentity = "WUG"
    typeLine = "Creature — Bird Wizard"
    power = 3
    toughness = 3
    oracleText = "Flying\n" +
        "{1}{G}{U}: Creatures you control gain shroud until end of turn. (They can't be the targets of spells or abilities.)"

    keywords(Keyword.FLYING)

    activatedAbility {
        cost = Costs.Mana("{1}{G}{U}")
        effect = Patterns.Group.grantKeywordToAll(
            keyword = Keyword.SHROUD,
            filter = Filters.Group.creaturesYouControl
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "1"
        artist = "Mark Zug"
        flavorText = "They are cautious with their body language and facial expressions. Any stray movement could betray the positions of the troops they protect and cost many lives."
        imageUri = "https://cards.scryfall.io/normal/front/5/8/58e86a42-cc53-4731-819a-e76f203a742a.jpg"
    }
}
