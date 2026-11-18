package com.wingedsheep.mtg.sets.definitions.m10.cards

import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Wall of Frost — Magic 2010 #80
 * {1}{U}{U} · Creature — Wall · 0/7
 *
 * Defender
 * Whenever this creature blocks a creature, that creature doesn't untap during its controller's
 * next untap step.
 *
 * Cleric of Chill Depths' block trigger: one trigger per blocked creature, granting `DOESNT_UNTAP`
 * to that attacker through its (current) controller's next untap step. The grant is a floating
 * effect keyed to the creature, so it persists if the Wall leaves the battlefield and follows the
 * creature across a control change; it never taps the creature.
 */
val WallOfFrost = card("Wall of Frost") {
    manaCost = "{1}{U}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Wall"
    power = 0
    toughness = 7
    oracleText = "Defender\nWhenever this creature blocks a creature, that creature doesn't untap during " +
        "its controller's next untap step."

    keywords(Keyword.DEFENDER)

    triggeredAbility {
        trigger = Triggers.self.blocks(attackerFilter = GameObjectFilter.Creature)
        effect = Effects.GrantKeyword(
            AbilityFlag.DOESNT_UNTAP,
            EffectTarget.TriggeringEntity,
            Duration.UntilAfterAffectedControllersNextUntap(),
        )
        description = "Whenever this creature blocks a creature, that creature doesn't untap during its " +
            "controller's next untap step."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "80"
        artist = "Mike Bierek"
        flavorText = "\"I welcome it. The pasture is cleared of weeds and the wolves are frozen solid.\"\n" +
            "—Lumi, goatherd"
        imageUri = "https://cards.scryfall.io/normal/front/1/7/17bc35a7-e38b-4c15-889a-d58c8b360315.jpg?1783942386"
        ruling(
            "2014-07-18",
            "Wall of Frost's ability tracks the creature, not the creature's controller. That is, if the " +
                "creature changes controllers before its first controller's next untap step, then it won't " +
                "untap during its new controller's next untap step.",
        )
        ruling(
            "2014-07-18",
            "If the creature isn't tapped during its controller's next untap step (perhaps because it was " +
                "untapped by a spell), Wall of Frost's ability has no effect at that time. It won't try to " +
                "keep the creature tapped on subsequent turns.",
        )
    }
}
