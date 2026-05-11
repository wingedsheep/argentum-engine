package com.wingedsheep.mtg.sets.definitions.eoe.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.AddCountersEffect
import com.wingedsheep.sdk.scripting.effects.MoveToZoneEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Interceptor Mechan
 * {2}{B}{R}
 * Artifact Creature — Robot
 * 2/2
 * Flying
 * When this creature enters, return target artifact or creature card from your graveyard to your hand.
 * Void — At the beginning of your end step, if a nonland permanent left the battlefield this turn
 *   or a spell was warped this turn, put a +1/+1 counter on this creature.
 */
val InterceptorMechan = card("Interceptor Mechan") {
    manaCost = "{2}{B}{R}"
    colorIdentity = "BR"
    typeLine = "Artifact Creature — Robot"
    power = 2
    toughness = 2
    oracleText = "Flying\n" +
        "When this creature enters, return target artifact or creature card from your graveyard to your hand.\n" +
        "Void — At the beginning of your end step, if a nonland permanent left the battlefield this turn or a spell was warped this turn, put a +1/+1 counter on this creature."

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        val t = target(
            "target artifact or creature card in your graveyard",
            TargetObject(filter = TargetFilter(GameObjectFilter.CreatureOrArtifact.ownedByYou(), zone = Zone.GRAVEYARD))
        )
        effect = MoveToZoneEffect(target = t, destination = Zone.HAND)
        description = "When this creature enters, return target artifact or creature card from your graveyard to your hand."
    }

    triggeredAbility {
        trigger = Triggers.YourEndStep
        triggerCondition = Conditions.Void
        effect = AddCountersEffect(Counters.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)
        description = "At the beginning of your end step, if a nonland permanent left the battlefield this turn or a spell was warped this turn, put a +1/+1 counter on this creature."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "220"
        artist = "Leonardo Santanna"
        imageUri = "https://cards.scryfall.io/normal/front/1/9/198211af-f413-4e9b-9baf-4b4fcb81eadc.jpg?1752947458"
    }
}
