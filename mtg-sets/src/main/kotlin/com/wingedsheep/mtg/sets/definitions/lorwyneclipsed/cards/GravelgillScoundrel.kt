package com.wingedsheep.mtg.sets.definitions.lorwyneclipsed.cards

import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CompositeEffect
import com.wingedsheep.sdk.scripting.effects.ReflexiveTriggerEffect
import com.wingedsheep.sdk.scripting.effects.SelectTargetEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject

val GravelgillScoundrel = card("Gravelgill Scoundrel") {
    manaCost = "{1}{U}"
    typeLine = "Creature — Merfolk Rogue"
    power = 1
    toughness = 3
    oracleText = "Vigilance\n" +
        "Whenever this creature attacks, you may tap another untapped creature you control. " +
        "If you do, this creature can't be blocked this turn."

    keywords(Keyword.VIGILANCE)

    triggeredAbility {
        trigger = Triggers.Attacks
        // The "may tap another untapped creature" is a resolution-time choice, not a target.
        // Selection happens via SelectTargetEffect after the player accepts the optional,
        // so declining doesn't force them to commit to one.
        effect = ReflexiveTriggerEffect(
            action = CompositeEffect(listOf(
                SelectTargetEffect(
                    requirement = TargetObject(
                        filter = TargetFilter.OtherCreatureYouControl.untapped()
                    ),
                    storeAs = "creatureToTap"
                ),
                Effects.Tap(EffectTarget.PipelineTarget("creatureToTap"))
            )),
            optional = true,
            reflexiveEffect = Effects.GrantKeyword(AbilityFlag.CANT_BE_BLOCKED, EffectTarget.Self)
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "53"
        artist = "John Tedrick"
        flavorText = "\"Cut the line and take the catch. Then find a little kith to snatch.\""
        imageUri = "https://cards.scryfall.io/normal/front/3/2/320bc30c-7a8a-411a-9f36-9c69126e131b.jpg?1767871759"
    }
}
