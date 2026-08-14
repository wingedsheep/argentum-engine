package com.wingedsheep.mtg.sets.definitions.spm.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.TriggeredAbility
import com.wingedsheep.sdk.scripting.effects.GrantTriggeredAbilityEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetCreature
import com.wingedsheep.sdk.scripting.values.ContextPropertyKey
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Strength of Will
 * {1}{G}
 * Instant
 * Until end of turn, target creature you control gains indestructible and
 * "Whenever this creature is dealt damage, put that many +1/+1 counters on it."
 *
 * The granted "dealt damage → counters" ability is a self-bound triggered ability
 * ([Triggers.TakesDamage], SELF): at resolution it reads the damage amount off the
 * trigger context ([ContextPropertyKey.TRIGGER_DAMAGE_AMOUNT]) and puts that many
 * +1/+1 counters on the creature itself. Both grants share the EndOfTurn duration.
 */
val StrengthOfWill = card("Strength of Will") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Instant"
    oracleText = "Until end of turn, target creature you control gains indestructible and " +
        "\"Whenever this creature is dealt damage, put that many +1/+1 counters on it.\""

    spell {
        val creature = target(
            "target creature you control",
            TargetCreature(filter = TargetFilter.Creature.youControl())
        )
        effect = Effects.Composite(
            Effects.GrantKeyword(Keyword.INDESTRUCTIBLE, creature),
            GrantTriggeredAbilityEffect(
                ability = TriggeredAbility.create(
                    trigger = Triggers.TakesDamage.event,
                    binding = Triggers.TakesDamage.binding,
                    effect = Effects.AddDynamicCounters(
                        Counters.PLUS_ONE_PLUS_ONE,
                        DynamicAmount.ContextProperty(ContextPropertyKey.TRIGGER_DAMAGE_AMOUNT),
                        EffectTarget.Self
                    ),
                ),
                target = creature,
            ),
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "118"
        artist = "Ryan Pancoast"
        flavorText = "And then—as the agonizing ache in his limbs seems unendurable—" +
            "from out of the pain—from out of the agony—comes triumph!"
        imageUri = "https://cards.scryfall.io/normal/front/6/8/68f985c7-7765-46c3-ad31-edae3abb9fbf.jpg?1783905321"
    }
}
