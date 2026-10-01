package com.wingedsheep.mtg.sets.definitions.one.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.EntersWithCounters
import com.wingedsheep.sdk.scripting.effects.Mode
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Atraxa's Skitterfang — Phyrexia: All Will Be One #223
 * {3} · Artifact Creature — Phyrexian Insect · 2/2 · Uncommon
 *
 * This creature enters with three oil counters on it.
 * At the beginning of combat on your turn, you may remove an oil counter from this creature.
 * When you do, target creature you control gains your choice of flying, vigilance, deathtouch,
 * or lifelink until end of turn.
 *
 * "When you do" is a reflexive trigger (CR 603.12): its target is chosen as it goes on the stack,
 * after the counter is removed. Per the 2023-02-04 ruling the keyword is chosen as the reflexive
 * ability *resolves*, so the [Effects.Modal] is nested in a one-element [Effects.Composite] — a
 * top-level modal on a triggered ability has its mode announced as it's put on the stack, while a
 * nested one is chosen on resolution by `ModalEffectExecutor` (which carries the outer target).
 */
val AtraxasSkitterfang = card("Atraxa's Skitterfang") {
    manaCost = "{3}"
    colorIdentity = ""
    typeLine = "Artifact Creature — Phyrexian Insect"
    power = 2
    toughness = 2
    oracleText = "This creature enters with three oil counters on it.\n" +
        "At the beginning of combat on your turn, you may remove an oil counter from this creature. " +
        "When you do, target creature you control gains your choice of flying, vigilance, deathtouch, " +
        "or lifelink until end of turn."

    replacementEffect(EntersWithCounters(counterType = CounterType.OIL, count = 3, selfOnly = true))

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.BEGIN_COMBAT)
        effect = Effects.ReflexiveTrigger(
            action = Effects.RemoveCounters(CounterType.OIL, 1, EffectTarget.Self),
            optional = true,
            descriptionOverride = "You may remove an oil counter from Atraxa's Skitterfang. When you do, " +
                "target creature you control gains your choice of flying, vigilance, deathtouch, or " +
                "lifelink until end of turn."
        ) {
            val creature = target(TargetFilter.CreatureYouControl)
            fun grant(keyword: Keyword, label: String) = Mode.noTarget(
                Effects.GrantKeyword(keyword, creature, Duration.EndOfTurn),
                "Target creature gains $label until end of turn"
            )
            // Wrapped (not top-level) so the keyword is chosen on resolution — see the KDoc.
            val chosenOnResolution = listOf(Effects.Modal(
                modes = listOf(
                    grant(Keyword.FLYING, "flying"),
                    grant(Keyword.VIGILANCE, "vigilance"),
                    grant(Keyword.DEATHTOUCH, "deathtouch"),
                    grant(Keyword.LIFELINK, "lifelink"),
                ),
                chooseCount = 1,
                countsAsModalSpell = false
            ))
            effect = Effects.Composite(
                chosenOnResolution,
                descriptionOverride = "Target creature you control gains your choice of flying, vigilance, " +
                    "deathtouch, or lifelink until end of turn."
            )
        }
        description = "At the beginning of combat on your turn, you may remove an oil counter from this " +
            "creature. When you do, target creature you control gains your choice of flying, vigilance, " +
            "deathtouch, or lifelink until end of turn."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "223"
        artist = "Billy Christian"
        imageUri = "https://cards.scryfall.io/normal/front/c/d/cdedebad-f71e-4434-871e-5bbbd3c07a12.jpg?1783917994"

        ruling(
            "2023-02-04",
            "You don't choose a target for Atraxa's Skitterfang's last ability at the time it triggers. " +
                "Rather, a second \"reflexive\" ability triggers when you remove an oil counter this way. " +
                "You choose a target for that ability as it goes on the stack. Each player may respond to " +
                "this triggered ability as normal."
        )
        ruling(
            "2023-02-04",
            "You choose which ability the target creature gets as the reflexive ability resolves. " +
                "Players may not wait to see what ability you choose before deciding whether to respond to it."
        )
    }
}
