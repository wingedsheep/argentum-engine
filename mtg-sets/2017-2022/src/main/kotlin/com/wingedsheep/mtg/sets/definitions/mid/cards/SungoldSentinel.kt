package com.wingedsheep.mtg.sets.definitions.mid.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ActivationRestriction
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.CardNumericProperty

/**
 * Sungold Sentinel (Innistrad: Midnight Hunt #37)
 * {1}{W}
 * Creature — Human Soldier
 * 3/2
 * Whenever this creature enters or attacks, exile up to one target card from a graveyard.
 * Coven — {1}{W}: Choose a color. This creature gains hexproof from that color until end of turn
 * and can't be blocked by creatures of that color this turn. Activate only if you control three or
 * more creatures with different powers.
 *
 * "Enters or attacks" is two triggered abilities sharing one effect (the Graveyard Trespasser
 * shape). The color is chosen on resolution ([Effects.ChooseColorThen], the Skrelv idiom), and the
 * coven gate is an activation restriction counting distinct power values among your creatures.
 */
val SungoldSentinel = card("Sungold Sentinel") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Soldier"
    oracleText = "Whenever this creature enters or attacks, exile up to one target card from a graveyard.\n" +
        "Coven — {1}{W}: Choose a color. This creature gains hexproof from that color until end of " +
        "turn and can't be blocked by creatures of that color this turn. Activate only if you control " +
        "three or more creatures with different powers."
    power = 3
    toughness = 2

    triggeredAbility {
        trigger = Triggers.self.enters()
        val exiled = target(TargetFilter.CardInGraveyard, optional = true)
        effect = Effects.Move(exiled, Zone.EXILE)
        description = "Whenever this creature enters, exile up to one target card from a graveyard."
    }

    triggeredAbility {
        trigger = Triggers.self.attacks()
        val exiled = target(TargetFilter.CardInGraveyard, optional = true)
        effect = Effects.Move(exiled, Zone.EXILE)
        description = "Whenever this creature attacks, exile up to one target card from a graveyard."
    }

    activatedAbility {
        cost = Costs.Mana("{1}{W}")
        effect = Effects.ChooseColorThen(
            Effects.GrantHexproofFromChosenColor(EffectTarget.Self) then
                Effects.GrantCantBeBlockedByChosenColor(EffectTarget.Self)
        )
        restrictions = listOf(
            ActivationRestriction.OnlyIfCondition(
                Conditions.CompareAmounts(
                    left = DynamicAmounts.battlefield(
                        Player.You,
                        GameObjectFilter.Creature,
                    ).distinctValues(CardNumericProperty.POWER),
                    operator = ComparisonOperator.GTE,
                    right = 3,
                ),
            ),
        )
        description = "Coven — {1}{W}: Choose a color. This creature gains hexproof from that color " +
            "until end of turn and can't be blocked by creatures of that color this turn."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "37"
        artist = "Marta Nael"
        imageUri = "https://cards.scryfall.io/normal/front/e/7/e7e1afd0-49d4-4657-ab75-98b0bfbfb245.jpg?1783925650"
        ruling("2021-09-24", "For three creatures to have different powers from one another, each of their powers needs to be different. A 1/1 creature, a 2/1 creature, and another 2/1 creature aren't three creatures with different powers, even though both 2/1 creatures have different power than the 1/1 creature.")
    }
}
