package com.wingedsheep.mtg.sets.definitions.aer.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility

/**
 * Consulate Dreadnought
 * {1}
 * Artifact — Vehicle
 * 7/11
 * Crew 6
 */
val ConsulateDreadnought = card("Consulate Dreadnought") {
    manaCost = "{1}"
    colorIdentity = ""
    typeLine = "Artifact — Vehicle"
    power = 7
    toughness = 11
    oracleText = "Crew 6 (Tap any number of creatures you control with total power 6 or more: This Vehicle becomes an artifact creature until end of turn.)"

    keywordAbility(KeywordAbility.crew(6))

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "146"
        artist = "Cliff Childs"
        flavorText = "\"It's like they planted another Bastion right in the middle of the harbor.\"\n—Bes Tavani, Bomat merchant"
        imageUri = "https://cards.scryfall.io/normal/front/1/1/11eff338-1940-4684-94c7-ef90e56dea99.jpg?1783936730"
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
