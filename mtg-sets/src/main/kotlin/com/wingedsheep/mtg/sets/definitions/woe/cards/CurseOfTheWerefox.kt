package com.wingedsheep.mtg.sets.definitions.woe.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.ReflexiveTriggerEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetCreature

/**
 * Curse of the Werefox
 * {2}{G}
 * Sorcery
 * Create a Monster Role token attached to target creature you control. When you do, that creature
 * fights up to one target creature you don't control.
 *
 * The fight is a **reflexive** triggered ability (CR 603.12, and the 2023-09-01 ruling): its target
 * isn't chosen when the sorcery is cast, but when the reflexive ability goes on the stack after the
 * Role is created — so opponents get a chance to respond in between, and the Role's +1/+1 is already
 * applied (and any older Role already fell off as an SBA) by the time the creatures fight.
 *
 * Wiring: the spell's own target is snapshotted into a pipeline collection *before* the reflexive
 * effect runs, because the reflexive resolution replaces `EffectContext.targets` with the reflexive
 * target. So the fight reads its first combatant from [EffectTarget.PipelineTarget] and its second
 * from the reflexive `ContextTarget(0)`. `optional = true` on the reflexive requirement is the
 * "up to one" — declining simply skips the fight.
 */
val CurseOfTheWerefox = card("Curse of the Werefox") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Sorcery"
    oracleText = "Create a Monster Role token attached to target creature you control. When you do, " +
        "that creature fights up to one target creature you don't control. (If you control another " +
        "Role on it, put that one into the graveyard. Enchanted creature gets +1/+1 and has trample. " +
        "Creatures that fight each deal damage equal to their power to the other.)"

    spell {
        val host = target("target creature you control", Targets.CreatureYouControl)
        effect = Effects.Pipeline(
            descriptionOverride = "Create a Monster Role token attached to target creature you " +
                "control. When you do, that creature fights up to one target creature you don't control."
        ) {
            val enchanted = gather(CardSource.ChosenTargets, name = "roleHost")
            run(
                ReflexiveTriggerEffect(
                    action = Effects.CreateRoleToken("Monster Role", host),
                    optional = false,
                    reflexiveEffect = Effects.Fight(
                        EffectTarget.PipelineTarget(enchanted.key),
                        EffectTarget.ContextTarget(0)
                    ),
                    reflexiveTargetRequirements = listOf(
                        TargetCreature(optional = true, filter = TargetFilter.CreatureOpponentControls)
                    ),
                    descriptionOverride = "When you do, that creature fights up to one target " +
                        "creature you don't control."
                )
            )
        }
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "167"
        artist = "Andrew Mar"
        imageUri = "https://cards.scryfall.io/normal/front/8/9/89148458-1fd6-48ef-a2d9-7b434c9723ec.jpg?1783915083"
        ruling(
            "2023-09-01",
            "You don't choose a target for the creature to fight at the time you cast this spell. " +
                "Rather, a second \"reflexive\" ability triggers when you create a Monster Role token " +
                "attached to the creature that was targeted by the spell. You choose a target for that " +
                "ability as it goes on the stack. Each player may respond to this triggered ability as normal."
        )
        ruling(
            "2023-09-01",
            "If you can't attach a Monster Role token to the target creature you control when Curse of " +
                "the Werefox resolves, the reflexive trigger that causes the fight won't happen."
        )
        ruling(
            "2023-09-01",
            "If the creature you control already has a Role that you control attached to it, that Role " +
                "will be put into its owner's graveyard as a state-based action after the Monster Role " +
                "is attached to it but before the reflexive triggered ability resolves."
        )
    }
}
