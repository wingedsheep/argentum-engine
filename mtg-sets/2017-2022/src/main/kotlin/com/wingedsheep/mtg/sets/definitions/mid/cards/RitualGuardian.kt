package com.wingedsheep.mtg.sets.definitions.mid.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.CardNumericProperty

/**
 * Ritual Guardian (Innistrad: Midnight Hunt #30)
 * {2}{W}
 * Creature — Human Soldier
 * 3/2
 * Coven — At the beginning of combat on your turn, if you control three or more creatures with
 * different powers, this creature gains lifelink until end of turn.
 *
 * Coven is an intervening-if (checked when the trigger fires and again on resolution): three or more
 * creatures you control with pairwise-different powers, counted as distinct power values.
 */
val RitualGuardian = card("Ritual Guardian") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Soldier"
    oracleText = "Coven — At the beginning of combat on your turn, if you control three or more " +
        "creatures with different powers, this creature gains lifelink until end of turn."
    power = 3
    toughness = 2

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.BEGIN_COMBAT)
        interveningIf = Conditions.CompareAmounts(
            left = DynamicAmounts.battlefield(
                Player.You,
                GameObjectFilter.Creature,
            ).distinctValues(CardNumericProperty.POWER),
            operator = ComparisonOperator.GTE,
            right = 3,
        )
        effect = Effects.GrantKeyword(Keyword.LIFELINK, EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "30"
        artist = "Denman Rooke"
        flavorText = "\"By ancient magics, by angels' grace, we will survive this night.\""
        imageUri = "https://cards.scryfall.io/normal/front/f/0/f077f07e-4660-4483-b6ea-16cca0956892.jpg?1783925654"
        ruling("2021-09-24", "For three creatures to have different powers from one another, each of their powers needs to be different. A 1/1 creature, a 2/1 creature, and another 2/1 creature aren't three creatures with different powers, even though both 2/1 creatures have different power than the 1/1 creature.")
        ruling("2021-09-24", "You must control three or more creatures with different powers at the time the ability triggers and at the time the ability tries to resolve. They do not, however, need to be the same set of creatures in both cases.")
    }
}
