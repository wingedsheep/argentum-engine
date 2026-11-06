package com.wingedsheep.mtg.sets.definitions.akh.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Gideon, Martial Paragon — Amonkhet #270 (Planeswalker Deck)
 * {4}{W} · Legendary Planeswalker — Gideon · Mythic · Starting loyalty 5
 *
 * +2: Untap all creatures you control. Those creatures get +1/+1 until end of turn.
 * 0: Until end of turn, Gideon becomes a 5/5 Human Soldier creature with indestructible that's
 *    still a planeswalker. Prevent all damage that would be dealt to him this turn.
 * −10: Creatures you control get +2/+2 until end of turn. Tap all creatures your opponents control.
 *
 * "Those creatures" is every creature you control as the +2 resolves, untapped or not (ruling), so
 * one snapshot of the group gets both the untap and the pump. Both mass pumps lock their set at
 * resolution: creatures that arrive later in the turn get nothing. The 0 is the Gideon of the
 * Trials animate/prevent shape at 5/5.
 */
val GideonMartialParagon = card("Gideon, Martial Paragon") {
    manaCost = "{4}{W}"
    colorIdentity = "W"
    typeLine = "Legendary Planeswalker — Gideon"
    startingLoyalty = 5
    oracleText = "+2: Untap all creatures you control. Those creatures get +1/+1 until end of turn.\n" +
        "0: Until end of turn, Gideon becomes a 5/5 Human Soldier creature with indestructible that's " +
        "still a planeswalker. Prevent all damage that would be dealt to him this turn.\n" +
        "−10: Creatures you control get +2/+2 until end of turn. Tap all creatures your opponents control."

    // +2: Untap all creatures you control. Those creatures get +1/+1 until end of turn.
    loyaltyAbility(+2) {
        effect = Effects.ForEachInGroup(
            GroupFilter.AllCreaturesYouControl,
            Effects.Untap(EffectTarget.IterationEntity) then
                Effects.ModifyStats(1, 1, EffectTarget.IterationEntity)
        )
        description = "Untap all creatures you control. Those creatures get +1/+1 until end of turn."
    }

    // 0: Until end of turn, Gideon becomes a 5/5 Human Soldier creature with indestructible that's
    //    still a planeswalker. Prevent all damage that would be dealt to him this turn.
    loyaltyAbility(0) {
        effect = Effects.BecomeCreature(
            target = EffectTarget.Self,
            power = 5,
            toughness = 5,
            keywords = setOf(Keyword.INDESTRUCTIBLE),
            creatureTypes = setOf("Human", "Soldier"),
            duration = Duration.EndOfTurn
        ) then Effects.PreventDamage(target = EffectTarget.Self)
        description = "Until end of turn, Gideon becomes a 5/5 Human Soldier creature with indestructible " +
            "that's still a planeswalker. Prevent all damage that would be dealt to him this turn."
    }

    // −10: Creatures you control get +2/+2 until end of turn. Tap all creatures your opponents control.
    loyaltyAbility(-10) {
        effect = Patterns.Group.modifyStatsForAll(2, 2, GroupFilter.AllCreaturesYouControl) then
            Patterns.Group.tapAll(GroupFilter.AllCreaturesOpponentsControl)
        description = "Creatures you control get +2/+2 until end of turn. Tap all creatures your opponents control."
    }

    metadata {
        inBooster = false
        rarity = Rarity.MYTHIC
        collectorNumber = "270"
        artist = "Daarken"
        imageUri = "https://cards.scryfall.io/normal/front/2/c/2c057ca8-9811-4dc3-9b0e-f9f69f752d1d.jpg?1783936437"
        ruling("2017-04-18", "All creatures you control get +1/+1 from Gideon's first ability, not just the ones that it untapped.")
        ruling("2017-04-18", "The set of creatures affected by Gideon's first and third abilities is determined as the abilities resolve. Creatures you begin to control later in the turn and permanents you control that become creatures later in the turn won't get +1/+1 or +2/+2.")
        ruling("2017-04-18", "Gideon's second ability doesn't count as a creature entering the battlefield. Gideon was already on the battlefield; he only changed his types.")
        ruling("2017-04-18", "If Gideon becomes a creature the same turn he enters the battlefield, you can't attack with him or use any of his {T} abilities (if he gains any).")
        ruling("2017-04-18", "Gideon's second ability causes him to become a creature with the creature types Human and Soldier. He remains a planeswalker with the planeswalker type Gideon. (He also retains any other card types or subtypes he may have had.) Each subtype is correlated to the proper card type: planeswalker is only a type (not a creature type), and Human and Soldier are just creature types (not planeswalker types).")
        ruling("2017-04-18", "If damage that can't be prevented is dealt to Gideon after his second ability has resolved, that damage will have all applicable results: specifically, the damage is marked on Gideon (since he's a creature) and that damage causes that many loyalty counters to be removed from him (since he's a planeswalker). Even though he has indestructible, if Gideon has no loyalty counters on him, he's put into his owner's graveyard.")
    }
}
