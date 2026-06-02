package com.wingedsheep.mtg.sets.definitions.ecl.cards

import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.AddCreatureTypeEffect
import com.wingedsheep.sdk.scripting.effects.RemoveAllAbilitiesEffect
import com.wingedsheep.sdk.scripting.effects.SetBasePowerToughnessEffect
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetOpponent
import com.wingedsheep.sdk.dsl.Effects

/**
 * Curious Colossus
 * {5}{W}{W}
 * Creature — Giant Warrior
 * 7/7
 *
 * When this creature enters, each creature target opponent controls loses all
 * abilities, becomes a Coward in addition to its other types, and has base
 * power and toughness 1/1.
 *
 * The transformation lasts indefinitely (Duration.Permanent) — it does not
 * expire when Curious Colossus leaves the battlefield, and the creatures
 * affected are snapshotted at resolution.
 */
val CuriousColossus = card("Curious Colossus") {
    manaCost = "{5}{W}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Giant Warrior"
    power = 7
    toughness = 7
    oracleText = "When this creature enters, each creature target opponent controls loses all abilities, becomes a Coward in addition to its other types, and has base power and toughness 1/1."

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        val opponent = target("target opponent", TargetOpponent())
        effect = Effects.ForEachInGroup(
            filter = GroupFilter(GameObjectFilter.Creature.targetPlayerControls(opponent)),
            effect = Effects.Composite(
                listOf(
                    RemoveAllAbilitiesEffect(EffectTarget.Self, Duration.Permanent),
                    AddCreatureTypeEffect("Coward", EffectTarget.Self, Duration.Permanent),
                    SetBasePowerToughnessEffect(EffectTarget.Self, 1, 1, Duration.Permanent)
                )
            )
        )
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "12"
        artist = "Raoul Vitale"
        flavorText = "Kithkin respect giants, but not usually from such close range."
        imageUri = "https://cards.scryfall.io/normal/front/5/8/582b6e5d-0bab-471d-af4d-19438c5fd524.jpg?1767658165"

        ruling("2025-11-17", "If one of the affected creatures gains an ability after Curious Colossus's ability resolves, it will keep that ability.")
        ruling("2025-11-17", "Curious Colossus's ability overwrites all previous effects that set an affected creature's base power and toughness to specific values. Any power- or toughness-setting effects that start to apply after the ability resolves will overwrite this effect.")
        ruling("2025-11-17", "Effects that modify a creature's power and/or toughness, such as the effect of Appeal to Eirdu, will apply to the creature no matter when they started to take effect. The same is true for any counters that change its power and/or toughness and effects that switch its power and toughness.")
        ruling("2025-11-17", "The effects of Curious Colossus's ability last indefinitely. They don't expire during the cleanup step or when Curious Colossus leaves the battlefield.")
        ruling("2025-11-17", "The effects of Curious Colossus's ability apply only to creatures the target opponent controls as the ability resolves. Creatures they begin to control later won't be affected.")
    }
}
