package com.wingedsheep.mtg.sets.definitions.dtk.cards

import com.wingedsheep.sdk.core.Keyword
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

/**
 * Sabertooth Outrider
 * {3}{R}
 * Creature — Human Warrior
 * 4/2
 *
 * Trample
 * Formidable — Whenever this creature attacks, if creatures you control have total power 8 or
 * greater, this creature gains first strike until end of turn.
 *
 * The formidable clause is an intervening-if (CR 603.4): the same total-power check as Surrak, the
 * Hunt Caller, tested when the attack trigger would fire and again on resolution.
 */
val SabertoothOutrider = card("Sabertooth Outrider") {
    manaCost = "{3}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Human Warrior"
    oracleText = "Trample\n" +
        "Formidable — Whenever this creature attacks, if creatures you control have total power 8 " +
        "or greater, this creature gains first strike until end of turn."
    power = 4
    toughness = 2

    keywords(Keyword.TRAMPLE)

    triggeredAbility {
        trigger = Triggers.self.attacks()
        interveningIf = Conditions.CompareAmounts(
            DynamicAmounts.battlefield(Player.You, GameObjectFilter.Creature).sumPower(),
            ComparisonOperator.GTE,
            8
        )
        effect = Effects.GrantKeyword(Keyword.FIRST_STRIKE, EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "152"
        artist = "Winona Nelson"
        imageUri = "https://cards.scryfall.io/normal/front/6/4/64ddda91-3ccb-4c87-82e6-d4e60f4afc24.jpg?1783938587"
        ruling("2015-02-25", "If you control a creature with power less than 0, use its actual power when calculating the total power of creatures you control. For example, if you control three creatures with powers 4, 5, and -2, the total power of creatures you control is 7.")
        ruling("2015-02-25", "Other formidable abilities are triggered abilities with an \"intervening 'if'\" clause. Such abilities check the total power of creatures you control twice: once at the appropriate time to see if the ability will trigger, and again as the ability tries to resolve. If, at that time, the total power of creatures you control is no longer 8 or greater, the ability will have no effect.")
    }
}
