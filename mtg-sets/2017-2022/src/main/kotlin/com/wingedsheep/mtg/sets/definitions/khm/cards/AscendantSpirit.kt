package com.wingedsheep.mtg.sets.definitions.khm.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.grantedTriggeredAbility
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.events.Recipient
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Ascendant Spirit
 * {U}
 * Snow Creature — Spirit
 * 1/1
 *
 * {S}{S}: This creature becomes a Spirit Warrior with base power and toughness 2/3.
 * {S}{S}{S}: If this creature is a Warrior, put a flying counter on it and it becomes a Spirit
 * Warrior Angel with base power and toughness 4/4.
 * {S}{S}{S}{S}: If this creature is an Angel, put two +1/+1 counters on it and it gains
 * "Whenever this creature deals combat damage to a player, draw a card."
 *
 * The Evolved Sleeper upgrade chain paid in snow mana:
 * - No ability has a duration (first ruling), so every change is `Duration.Permanent`.
 * - "becomes a Spirit Warrior" overwrites creature types (second ruling) — `SetCreatureSubtypes`
 *   in layer 4; base P/T is a layer-7b set, so counters still apply on top (fifth ruling).
 * - The Warrior / Angel checks happen on resolution against projected subtypes.
 * - Each resolution of the last ability while an Angel grants another instance of the draw
 *   trigger (sixth ruling), so the grant is appended, never deduplicated.
 */
val AscendantSpirit = card("Ascendant Spirit") {
    manaCost = "{U}"
    colorIdentity = "U"
    typeLine = "Snow Creature — Spirit"
    power = 1
    toughness = 1
    oracleText = "{S}{S}: This creature becomes a Spirit Warrior with base power and toughness 2/3.\n" +
        "{S}{S}{S}: If this creature is a Warrior, put a flying counter on it and it becomes a Spirit " +
        "Warrior Angel with base power and toughness 4/4.\n" +
        "{S}{S}{S}{S}: If this creature is an Angel, put two +1/+1 counters on it and it gains " +
        "\"Whenever this creature deals combat damage to a player, draw a card.\""

    val drawOnCombatDamage = grantedTriggeredAbility {
        trigger = Triggers.self.dealsCombatDamage(Recipient.AnyPlayer)
        effect = Effects.DrawCards(1)
        description = "Whenever this creature deals combat damage to a player, draw a card."
    }

    // {S}{S}: This creature becomes a Spirit Warrior with base power and toughness 2/3.
    activatedAbility {
        cost = Costs.Mana("{S}{S}")
        effect = Effects.SetCreatureSubtypes(
            subtypes = setOf(Subtype.SPIRIT.value, Subtype.WARRIOR.value),
            target = EffectTarget.Self,
            duration = Duration.Permanent
        ) then
            Effects.SetBasePowerAndToughness(
                power = 2,
                toughness = 3,
                target = EffectTarget.Self,
                duration = Duration.Permanent
            )
        description = "This creature becomes a Spirit Warrior with base power and toughness 2/3."
    }

    // {S}{S}{S}: If this creature is a Warrior, put a flying counter on it and it becomes a Spirit
    // Warrior Angel with base power and toughness 4/4.
    activatedAbility {
        cost = Costs.Mana("{S}{S}{S}")
        effect = Effects.If(
            condition = Conditions.SourceHasSubtype(Subtype.WARRIOR),
            then = Effects.AddCounters(CounterType.FLYING, 1, EffectTarget.Self) then
                Effects.SetCreatureSubtypes(
                    subtypes = setOf(Subtype.SPIRIT.value, Subtype.WARRIOR.value, Subtype.ANGEL.value),
                    target = EffectTarget.Self,
                    duration = Duration.Permanent
                ) then
                Effects.SetBasePowerAndToughness(
                    power = 4,
                    toughness = 4,
                    target = EffectTarget.Self,
                    duration = Duration.Permanent
                )
        )
        description = "If this creature is a Warrior, put a flying counter on it and it becomes a " +
            "Spirit Warrior Angel with base power and toughness 4/4."
    }

    // {S}{S}{S}{S}: If this creature is an Angel, put two +1/+1 counters on it and it gains
    // "Whenever this creature deals combat damage to a player, draw a card."
    activatedAbility {
        cost = Costs.Mana("{S}{S}{S}{S}")
        effect = Effects.If(
            condition = Conditions.SourceHasSubtype(Subtype.ANGEL),
            then = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 2, EffectTarget.Self) then
                Effects.GrantTriggeredAbility(drawOnCombatDamage, EffectTarget.Self, Duration.Permanent)
        )
        description = "If this creature is an Angel, put two +1/+1 counters on it and it gains " +
            "\"Whenever this creature deals combat damage to a player, draw a card.\""
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "43"
        artist = "Lie Setiawan"
        imageUri = "https://cards.scryfall.io/normal/front/e/a/ea56de43-c0fd-4627-846d-41a962b95f17.jpg?1783928269"
        ruling(
            "2021-02-05",
            "None of Ascendant Spirit's abilities has a duration. If one of them resolves, it will " +
                "remain in effect until Ascendant Spirit leaves the battlefield or a subsequent effect " +
                "changes its characteristics.",
        )
        ruling(
            "2021-02-05",
            "The first and second abilities cause Ascendant Spirit to lose any other creature types " +
                "it may have. It retains any card types or supertypes it may have.",
        )
        ruling(
            "2021-02-05",
            "The first and second abilities overwrite any previous effects that set Ascendant " +
                "Spirit's base power and toughness to specific values. Any effects that set Ascendant " +
                "Spirit's base power and toughness that start to apply after one of these abilities " +
                "will override the effect of that ability.",
        )
        ruling(
            "2021-02-05",
            "Activating the second ability after Ascendant Spirit has already been legally blocked " +
                "by a creature without flying or reach won't change or undo that block.",
        )
        ruling(
            "2021-02-05",
            "Any of Ascendant Spirit's abilities may be activated repeatedly. Multiple flying " +
                "counters on the same permanent provide no additional benefit; however, each time the " +
                "last ability resolves while Ascendant Spirit is an Angel will give Ascendant Spirit " +
                "two more +1/+1 counters and cause it to gain another instance of the triggered " +
                "ability. All of those instances trigger separately.",
        )
    }
}
