package com.wingedsheep.mtg.sets.definitions.mid.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.LookAtTopOfLibrary
import com.wingedsheep.sdk.scripting.PlayLandsAndCastFilteredFromTopOfLibrary
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.values.CardNumericProperty

/**
 * Augur of Autumn
 * {1}{G}{G}
 * Creature — Human Druid
 * 2/3
 * You may look at the top card of your library any time.
 * You may play lands from the top of your library.
 * Coven — As long as you control three or more creatures with different powers, you may cast
 * creature spells from the top of your library.
 *
 * The private peek is [LookAtTopOfLibrary]; the lands-only permission is
 * [PlayLandsAndCastFilteredFromTopOfLibrary] with `spellFilter = null` (as Courser of Kruphix).
 * The coven clause is a second such permission with a creature spell filter, gated by a conditional
 * static counting distinct power values among your creatures (the Sungold Sentinel coven count).
 */
val AugurOfAutumn = card("Augur of Autumn") {
    manaCost = "{1}{G}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Human Druid"
    power = 2
    toughness = 3
    oracleText = "You may look at the top card of your library any time.\n" +
        "You may play lands from the top of your library.\n" +
        "Coven — As long as you control three or more creatures with different powers, you may cast " +
        "creature spells from the top of your library."

    staticAbility { ability = LookAtTopOfLibrary }

    staticAbility {
        ability = PlayLandsAndCastFilteredFromTopOfLibrary(spellFilter = null)
    }

    staticAbility {
        condition = Conditions.CompareAmounts(
            left = DynamicAmounts.battlefield(
                Player.You,
                GameObjectFilter.Creature,
            ).distinctValues(CardNumericProperty.POWER),
            operator = ComparisonOperator.GTE,
            right = 3,
        )
        ability = PlayLandsAndCastFilteredFromTopOfLibrary(spellFilter = GameObjectFilter.Creature)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "168"
        artist = "Billy Christian"
        imageUri = "https://cards.scryfall.io/normal/front/6/a/6aa98767-ae27-4cf0-98ea-93e659f160f4.jpg?1783925586"
        ruling(
            "2021-09-24",
            "Augur of Autumn doesn't change the times when you can play a land card from the top of your " +
                "library. You can play a land only during your main phase when you have priority and the " +
                "stack is empty. Doing so counts as your land play for the turn.",
        )
        ruling(
            "2021-09-24",
            "For three creatures to have different powers from one another, each of their powers needs to " +
                "be different. A 1/1 creature, a 2/1 creature, and another 2/1 creature aren't three " +
                "creatures with different powers.",
        )
        ruling(
            "2021-09-24",
            "You still pay all costs for that spell, including additional costs. You may also pay " +
                "alternative costs.",
        )
    }
}
