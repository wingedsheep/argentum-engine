package com.wingedsheep.mtg.sets.definitions.akh.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ExertAsItAttacks
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Combat Celebrant
 * {2}{R}
 * Creature — Human Warrior
 * 4/1
 *
 * If this creature hasn't been exerted this turn, you may exert it as it attacks. When you do,
 * untap all other creatures you control and after this phase, there is an additional combat phase.
 *
 * The exert is a once-per-turn optional cost to attack ([ExertAsItAttacks] with
 * `onlyIfNotExertedThisTurn`, CR 701.43d); the "when you do" paragraph is its linked trigger
 * (CR 607.2h). The additional combat phase follows this one directly, with no main phase between.
 */
val CombatCelebrant = card("Combat Celebrant") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Human Warrior"
    oracleText = "If this creature hasn't been exerted this turn, you may exert it as it attacks. When you do, " +
        "untap all other creatures you control and after this phase, there is an additional combat phase. " +
        "(An exerted creature won't untap during your next untap step.)"
    power = 4
    toughness = 1

    staticAbility {
        ability = ExertAsItAttacks(onlyIfNotExertedThisTurn = true)
    }

    triggeredAbility {
        trigger = Triggers.self.exertedAsItAttacks()
        effect = Effects.ForEachInGroup(
            GroupFilter.OtherCreaturesYouControl,
            Effects.Untap(EffectTarget.IterationEntity)
        ) then Effects.AddCombatPhase
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "125"
        artist = "Chris Rallis"
        imageUri = "https://cards.scryfall.io/normal/front/2/8/28b63c3d-2e55-4343-b49a-11fa602ec473.jpg?1783936491"
        ruling("2017-04-18", "Combat Celebrant's ability untaps all of your creatures, not just the ones that are attacking.")
        ruling("2017-04-18", "There's no main phase between your combat phases, so you'll have no opportunity to cast spells or activate abilities that could only be cast any time you could cast a sorcery. For example, you won't be able to cast another creature or equip Equipment between combats.")
        ruling("2017-04-18", "If you exert Combat Celebrant, you get an additional combat phase even if Combat Celebrant doesn't survive the first combat phase.")
    }
}
