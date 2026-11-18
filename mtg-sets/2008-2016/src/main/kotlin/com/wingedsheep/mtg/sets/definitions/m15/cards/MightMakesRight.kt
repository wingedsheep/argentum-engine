package com.wingedsheep.mtg.sets.definitions.m15.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.values.CardNumericProperty

/**
 * Might Makes Right — Magic 2015 #156
 * {5}{R} · Enchantment
 *
 * At the beginning of combat on your turn, if you control each creature on the battlefield with the
 * greatest power, gain control of target creature an opponent controls until end of turn. Untap that
 * creature. It gains haste until end of turn.
 *
 * **Intervening-if** (checked as combat begins and again on resolution, CR 603.4): "you control each
 * creature with the greatest power" holds exactly when you control a creature and no creature another
 * player controls has power greater than or equal to the greatest power among yours — a tie with
 * anyone else's creature fails it (ruling). The vacuous "no creatures at all" reading never matters:
 * with no creature an opponent controls the ability has no target and is removed from the stack.
 */
val MightMakesRight = card("Might Makes Right") {
    manaCost = "{5}{R}"
    colorIdentity = "R"
    typeLine = "Enchantment"
    oracleText = "At the beginning of combat on your turn, if you control each creature on the battlefield " +
        "with the greatest power, gain control of target creature an opponent controls until end of turn. " +
        "Untap that creature. It gains haste until end of turn. (It can attack and {T} this turn.)"

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.BEGIN_COMBAT)
        interveningIf = Conditions.All(
            Conditions.YouControl(GameObjectFilter.Creature),
            Conditions.CompareAmounts(
                DynamicAmounts.battlefield(
                    Player.EachOtherPlayer,
                    GameObjectFilter.Creature.compareNumericProperty(
                        CardNumericProperty.POWER,
                        ComparisonOperator.GTE,
                        DynamicAmounts.battlefield(Player.You, GameObjectFilter.Creature).maxPower(),
                    ),
                ).count(),
                ComparisonOperator.EQ,
                0,
            ),
        )
        val t = target(TargetFilter.CreatureOpponentControls)
        effect = Effects.GainControl(t, Duration.EndOfTurn) then
            Effects.Untap(t) then
            Effects.GrantKeyword(Keyword.HASTE, t)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "156"
        artist = "Mark Winters"
        flavorText = "An oath of fealty sworn with a handshake."
        imageUri = "https://cards.scryfall.io/normal/front/7/5/75c63c02-a75b-4880-b9fa-6f4bcf90ede5.jpg?1783939171"
        ruling(
            "2014-07-18",
            "The ability will trigger only if you control each creature on the battlefield with the greatest " +
                "power as the beginning of combat step begins. If another player controls a creature with the " +
                "greatest power or tied for the greatest power at that time, the ability won't trigger at all."
        )
        ruling(
            "2014-07-18",
            "The ability will check its condition again as it tries to resolve. If, at that time, another " +
                "player controls a creature with the greatest power or tied for the greatest power, the " +
                "ability won't do anything."
        )
        ruling(
            "2014-07-18",
            "Gaining control of a creature doesn't cause you to gain control of any Auras or Equipment attached to it."
        )
    }
}
