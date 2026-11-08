package com.wingedsheep.mtg.sets.definitions.aer.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Aethersphere Harvester
 * {3}
 * Artifact — Vehicle
 * 3/5
 * Flying
 * When this Vehicle enters, you get {E}{E} (two energy counters).
 * Pay {E}: This Vehicle gains lifelink until end of turn.
 * Crew 1
 */
val AethersphereHarvester = card("Aethersphere Harvester") {
    manaCost = "{3}"
    typeLine = "Artifact — Vehicle"
    power = 3
    toughness = 5
    oracleText = "Flying\n" +
        "When this Vehicle enters, you get {E}{E} (two energy counters).\n" +
        "Pay {E}: This Vehicle gains lifelink until end of turn.\n" +
        "Crew 1 (Tap any number of creatures you control with total power 1 or more: This Vehicle becomes an artifact creature until end of turn.)"

    keywords(Keyword.FLYING)
    keywordAbility(KeywordAbility.crew(1))

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.GetEnergy(2)
    }

    activatedAbility {
        cost = Costs.PayPlayerCounters(CounterType.ENERGY, 1)
        effect = Effects.GrantKeyword(Keyword.LIFELINK, EffectTarget.Self)
        description = "Pay {E}: This Vehicle gains lifelink until end of turn."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "142"
        artist = "Christine Choi"
        imageUri = "https://cards.scryfall.io/normal/front/4/1/4160bb5f-4b49-4535-94f5-776d7abd1d1a.jpg?1783936732"
        ruling("2017-09-29", "Each Vehicle is printed with a power and toughness, but it's not a creature. If it becomes a creature (most likely through its crew ability), it will have that power and toughness.")
        ruling("2017-09-29", "Vehicle is an artifact type, not a creature type. A Vehicle that's crewed won't normally have any creature type.")
        ruling("2017-09-29", "You may activate a crew ability of a Vehicle even if it's already an artifact creature. Doing so has no effect on the Vehicle. It doesn't change its power and toughness.")
        ruling("2017-09-29", "For a Vehicle to be able to attack, it must be a creature as the declare attackers step begins, so the latest you can activate its crew ability to attack with it is during the beginning of combat step. For a Vehicle to be able to block, it must be a creature as the declare blockers step begins, so the latest you can activate its crew ability to block with it is during the declare attackers step. In either case, players may take actions after the crew ability resolves but before the Vehicle has been declared as an attacking or blocking creature.")
        ruling("2017-09-29", "When a Vehicle becomes a creature, that doesn't count as having a creature enter the battlefield. The permanent was already on the battlefield; it only changed its types. Abilities that trigger whenever a creature enters the battlefield won't trigger.")
        ruling("2017-09-29", "Any untapped creature you control can be tapped to pay a crew cost, even one that just came under your control.")
        ruling("2017-09-29", "You may tap more creatures than necessary to activate a crew ability.")
        ruling("2017-09-29", "Once a Vehicle becomes a creature, it behaves exactly like any other artifact creature. It can't attack unless you've controlled it continuously since your turn began, it can block if it's untapped, it can be tapped to pay a Vehicle's crew cost, and so on.")
        ruling("2017-02-09", "Multiple instances of lifelink on the same creature are redundant.")
        ruling("2024-06-07", "If an effect says you get one or more {E}, you get that many energy counters. To pay one or more {E}, you lose that many energy counters. You can't pay more energy counters than you have. Any effects that interact with counters a player gets, has, or loses can interact with energy counters.")
    }
}
