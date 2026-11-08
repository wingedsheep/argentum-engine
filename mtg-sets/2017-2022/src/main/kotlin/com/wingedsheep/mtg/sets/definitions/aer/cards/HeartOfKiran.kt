package com.wingedsheep.mtg.sets.definitions.aer.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AlternativeCrewCost
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility

/**
 * Heart of Kiran
 * {2}
 * Legendary Artifact — Vehicle
 * 4/4
 * Flying, vigilance
 * Crew 3
 * You may remove a loyalty counter from a planeswalker you control rather than pay Heart of
 * Kiran's crew cost.
 */
val HeartOfKiran = card("Heart of Kiran") {
    manaCost = "{2}"
    colorIdentity = ""
    typeLine = "Legendary Artifact — Vehicle"
    power = 4
    toughness = 4
    oracleText = "Flying, vigilance\nCrew 3 (Tap any number of creatures you control with total power 3 or more: This Vehicle becomes an artifact creature until end of turn.)\nYou may remove a loyalty counter from a planeswalker you control rather than pay Heart of Kiran's crew cost."

    keywords(Keyword.FLYING, Keyword.VIGILANCE)
    keywordAbility(KeywordAbility.crew(3))

    staticAbility {
        ability = AlternativeCrewCost(
            counterType = CounterType.LOYALTY,
            filter = GameObjectFilter.Planeswalker
        )
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "153"
        artist = "Jaime Jones"
        imageUri = "https://cards.scryfall.io/normal/front/5/e/5e847c44-1849-4251-9075-88ed5c7792a6.jpg?1783936731"
        ruling("2017-02-09", "Paying Heart of Kiran's alternative crew cost isn't a loyalty ability. It can be done even if you've already activated a loyalty ability of the planeswalker this turn, and it can be done any time you could activate Heart of Kiran's crew ability.")
        ruling("2017-09-29", "Vehicle is an artifact type, not a creature type. A Vehicle that's crewed won't normally have any creature type.")
        ruling("2017-09-29", "Each Vehicle is printed with a power and toughness, but it's not a creature. If it becomes a creature (most likely through its crew ability), it will have that power and toughness.")
        ruling("2017-09-29", "If an effect causes a Vehicle to become an artifact creature with a specified power and toughness, that effect overwrites the Vehicle's printed power and toughness.")
        ruling("2017-09-29", "Once a player announces that they are activating a crew ability, no player may take other actions until the ability has been paid for. Notably, players can't try to stop the ability by changing a creature's power or by removing or tapping a creature.")
        ruling("2017-09-29", "Any untapped creature you control can be tapped to pay a crew cost, even one that just came under your control.")
        ruling("2017-09-29", "You may tap more creatures than necessary to activate a crew ability.")
        ruling("2017-09-29", "Creatures that crew a Vehicle aren't attached to it or related in any other way. Effects that affect the Vehicle, such as by destroying it or giving it a +1/+1 counter, don't affect the creatures that crewed it.")
        ruling("2017-09-29", "Once a Vehicle becomes a creature, it behaves exactly like any other artifact creature. It can't attack unless you've controlled it continuously since your turn began, it can block if it's untapped, it can be tapped to pay a Vehicle's crew cost, and so on.")
        ruling("2017-09-29", "You may activate a crew ability of a Vehicle even if it's already an artifact creature. Doing so has no effect on the Vehicle. It doesn't change its power and toughness.")
        ruling("2017-09-29", "For a Vehicle to be able to attack, it must be a creature as the declare attackers step begins, so the latest you can activate its crew ability to attack with it is during the beginning of combat step. For a Vehicle to be able to block, it must be a creature as the declare blockers step begins, so the latest you can activate its crew ability to block with it is during the declare attackers step. In either case, players may take actions after the crew ability resolves but before the Vehicle has been declared as an attacking or blocking creature.")
        ruling("2017-09-29", "When a Vehicle becomes a creature, that doesn't count as having a creature enter the battlefield. The permanent was already on the battlefield; it only changed its types. Abilities that trigger whenever a creature enters the battlefield won't trigger.")
        ruling("2017-09-29", "If a permanent becomes a copy of a Vehicle, the copy won't be a creature, even if the Vehicle it's copying has become an artifact creature.")
    }
}
