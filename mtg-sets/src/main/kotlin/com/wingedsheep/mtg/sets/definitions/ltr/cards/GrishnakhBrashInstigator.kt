package com.wingedsheep.mtg.sets.definitions.ltr.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.effects.ReflexiveTriggerEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetCreature
import com.wingedsheep.sdk.scripting.values.EntityReference

/**
 * Grishnákh, Brash Instigator
 * {2}{R}
 * Legendary Creature — Goblin Soldier
 * 1/1
 *
 * When Grishnákh enters, amass Orcs 2. When you do, until end of turn, gain control of target
 * nonlegendary creature an opponent controls with power less than or equal to the amassed Army's
 * power. Untap that creature. It gains haste until end of turn.
 *
 * Modeled as a [ReflexiveTriggerEffect] — the "when you do" half is a second, reflexive triggered
 * ability that fires after the amass resolves (Scryfall ruling 2023-06-16). Its target is chosen as
 * the reflexive ability goes on the stack, and players may respond.
 *
 * The reflexive target filter — "creature with power <= the amassed Army's power" — references a
 * resolution-time pipeline value: the amass step stashes the just-amassed Army under
 * [EntityReference.AmassedArmy], and [TargetFilter.powerAtMostEntity] compares each candidate's
 * projected power against it. The pipeline is threaded into the target search via
 * `findLegalTargets(..., pipelineContext = ...)`, so a power-3 creature is excluded from the legal
 * targets after "amass Orcs 2" produced a 2/2 Army while a power-2 creature is included.
 */
val GrishnakhBrashInstigator = card("Grishnákh, Brash Instigator") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Legendary Creature — Goblin Soldier"
    oracleText = "When Grishnákh enters, amass Orcs 2. When you do, until end of turn, gain control " +
        "of target nonlegendary creature an opponent controls with power less than or equal to the " +
        "amassed Army's power. Untap that creature. It gains haste until end of turn."
    power = 1
    toughness = 1

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        val controlled = EffectTarget.ContextTarget(0)
        effect = ReflexiveTriggerEffect(
            action = Effects.Amass(2, "Orc"),
            optional = false,
            reflexiveEffect = Effects.Composite(
                Effects.GainControl(controlled, Duration.EndOfTurn),
                Effects.Untap(controlled),
                Effects.GrantKeyword(Keyword.HASTE, controlled, Duration.EndOfTurn)
            ),
            reflexiveTargetRequirements = listOf(
                TargetCreature(
                    filter = TargetFilter.CreatureOpponentControls
                        .nonlegendary()
                        .powerAtMostEntity(EntityReference.AmassedArmy)
                )
            )
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "134"
        artist = "Victor Harmatiuk"
        imageUri = "https://cards.scryfall.io/normal/front/6/3/6385e769-f805-499d-9f47-494533362152.jpg?1686969015"
        ruling("2023-06-16", "You don't choose a target for Grishnákh, Brash Instigator's ability at the time it triggers. Rather, a second \"reflexive\" ability triggers when you amass Orcs this way. You choose a target for that ability as it goes on the stack. Each player may respond to this triggered ability as normal.")
        ruling("2023-06-16", "Some cards refer to the \"amassed Army.\" That means the Army creature you chose to receive counters, even if no counters were placed on it for some reason.")
    }
}
