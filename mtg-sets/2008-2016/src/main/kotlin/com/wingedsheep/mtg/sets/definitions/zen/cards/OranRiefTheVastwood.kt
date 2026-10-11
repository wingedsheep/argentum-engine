package com.wingedsheep.mtg.sets.definitions.zen.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersTapped
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Oran-Rief, the Vastwood
 *
 * Land
 *
 * This land enters tapped.
 * {T}: Add {G}.
 * {T}: Put a +1/+1 counter on each green creature that entered this turn.
 *
 * The counter ability is untargeted and controller-agnostic ("each", not "you control"), and the
 * group is gathered at resolution against current characteristics — both per the 2009 rulings.
 */
val OranRiefTheVastwood = card("Oran-Rief, the Vastwood") {
    manaCost = ""
    colorIdentity = "G"
    typeLine = "Land"
    oracleText = "This land enters tapped.\n" +
        "{T}: Add {G}.\n" +
        "{T}: Put a +1/+1 counter on each green creature that entered this turn."

    replacementEffect(EntersTapped())

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddMana(Color.GREEN)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.ForEachInGroup(
            GroupFilter(GameObjectFilter.Creature.withColor(Color.GREEN).enteredThisTurn()),
            Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.IterationEntity)
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "221"
        artist = "Mike Bierek"
        imageUri = "https://cards.scryfall.io/normal/front/9/b/9bcc3bab-46f8-4ed2-abdb-7a117ad07d03.jpg?1783942122"
        ruling("2009-10-01", "Oran-Rief's last ability affects all permanents that are green creatures that entered this turn, not just the ones you control.")
        ruling("2009-10-01", "Oran-Rief's last ability cares about permanents' characteristics at the time the ability resolves, not their characteristics at the time they entered. For example, if a blue creature enters, then is turned green by a spell or ability, then Oran-Rief's second ability resolves, you'll put a +1/+1 counter on that creature.")
    }
}
