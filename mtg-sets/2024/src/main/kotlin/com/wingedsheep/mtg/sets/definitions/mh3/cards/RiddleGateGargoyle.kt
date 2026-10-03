package com.wingedsheep.mtg.sets.definitions.mh3.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Riddle Gate Gargoyle
 * {W}{U}
 * Artifact Creature — Gargoyle
 * 2/2
 *
 * Flying
 * When this creature enters, you get {E}{E}{E} (three energy counters).
 * Whenever you attack, you may pay {E}{E}. When you do, target creature you control gains
 * lifelink until end of turn.
 *
 * The attack ability is a reflexive trigger (CR 603.12), same shape as Guide of Souls: the
 * "may pay {E}{E}" is untargeted, and the lifelink grant chooses its target only once the
 * payment is made. The prompt is skipped entirely when fewer than two energy are available.
 */
val RiddleGateGargoyle = card("Riddle Gate Gargoyle") {
    manaCost = "{W}{U}"
    colorIdentity = "WU"
    typeLine = "Artifact Creature — Gargoyle"
    power = 2
    toughness = 2
    oracleText = "Flying\nWhen this creature enters, you get {E}{E}{E} (three energy counters).\n" +
        "Whenever you attack, you may pay {E}{E}. When you do, target creature you control gains " +
        "lifelink until end of turn."

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.GetEnergy(3)
    }

    triggeredAbility {
        trigger = Triggers.you.attacks()
        effect = Effects.ReflexiveTrigger(
            action = Effects.PayExactCounters(CounterType.ENERGY, 2),
            optional = true,
            descriptionOverride = "You may pay {E}{E}. When you do, target creature you control " +
                "gains lifelink until end of turn."
        ) {
            val creature = target(TargetFilter.CreatureYouControl)
            effect = Effects.GrantKeyword(Keyword.LIFELINK, creature, Duration.EndOfTurn)
        }
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "201"
        artist = "Bartek Fedyczak"
        imageUri = "https://cards.scryfall.io/normal/front/4/e/4e72a934-12e0-4e56-af67-566c2d5b01c1.jpg?1783911245"

        ruling(
            "2024-06-07",
            "You don't choose a target for Riddle Gate Gargoyle's last ability at the time it " +
                "triggers. Rather, a second \"reflexive\" ability triggers when you pay {E}{E} " +
                "this way. You choose a target for that ability as it goes on the stack. Each " +
                "player may respond to this triggered ability as normal."
        )
    }
}
