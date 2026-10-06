package com.wingedsheep.mtg.sets.definitions.akh.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ActivationRestriction
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.PreventActivatedAbilities
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

val EdificeOfAuthority = card("Edifice of Authority") {
    manaCost = "{3}"
    typeLine = "Artifact"
    oracleText = "{1}, {T}: Target creature can't attack this turn. Put a brick counter on this artifact.\n" +
        "{1}, {T}: Until your next turn, target creature can't attack or block and its activated abilities " +
        "can't be activated. Activate only if there are three or more brick counters on this artifact."

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{1}"), Costs.Tap)
        val creature = target(TargetFilter.Creature)
        effect = Effects.CantAttack(creature) then
            Effects.AddCounters(CounterType.BRICK, 1, EffectTarget.Self)
        description = "Target creature can't attack this turn. Put a brick counter on this artifact."
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{1}"), Costs.Tap)
        restrictions = listOf(
            ActivationRestriction.OnlyIfCondition(Conditions.SourceCounterCountAtLeast(CounterType.BRICK, 3))
        )
        val creature = target(TargetFilter.Creature)
        effect = Effects.CantAttackOrBlock(creature, Duration.UntilYourNextTurn) then
            Effects.GrantStaticAbility(
                ability = PreventActivatedAbilities(GameObjectFilter.Permanent.sourceItself()),
                target = creature,
                duration = Duration.UntilYourNextTurn
            )
        description = "Until your next turn, target creature can't attack or block and its activated abilities can't be activated."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "226"
        artist = "Florian de Gesincourt"
        imageUri = "https://cards.scryfall.io/normal/front/6/6/66e6fa29-087f-4da6-9114-30feec708560.jpg?1783936452"
        ruling(
            "2017-04-18",
            "Once a player has announced that they are activating the ability of a creature, " +
                "Edifice of Authority can’t be used to undo it. Its last ability must be activated " +
                "before the player activates that creature’s ability. The player may respond to " +
                "Edifice of Authority’s ability with the target creature’s ability if able."
        )
    }
}
