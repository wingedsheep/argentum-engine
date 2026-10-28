package com.wingedsheep.mtg.sets.definitions.znr.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.PreventActivatedAbilities
import com.wingedsheep.sdk.scripting.effects.EffectChoice
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Linvala, Shield of Sea Gate — Zendikar Rising #226
 * {1}{W}{U} · Legendary Creature — Angel Wizard · 3/3
 *
 * Flying
 * At the beginning of combat on your turn, if you have a full party, choose target nonland
 * permanent an opponent controls. Until your next turn, it can't attack or block, and its
 * activated abilities can't be activated.
 * Sacrifice Linvala: Choose hexproof or indestructible. Creatures you control gain that ability
 * until end of turn.
 *
 * The combat trigger is Archpriest of Iona's full-party intervening "if" (checked again on
 * resolution) wrapped around Academic Probation's lock. The sacrifice ability's "choose hexproof
 * or indestructible" is a resolution-time choice, so it's a [Effects.ChooseAction] between two
 * group grants; the creatures are locked in as the ability resolves.
 */
val LinvalaShieldOfSeaGate = card("Linvala, Shield of Sea Gate") {
    manaCost = "{1}{W}{U}"
    colorIdentity = "WU"
    typeLine = "Legendary Creature — Angel Wizard"
    oracleText = "Flying\n" +
        "At the beginning of combat on your turn, if you have a full party, choose target nonland " +
        "permanent an opponent controls. Until your next turn, it can't attack or block, and its " +
        "activated abilities can't be activated.\n" +
        "Sacrifice Linvala: Choose hexproof or indestructible. Creatures you control gain that " +
        "ability until end of turn."
    power = 3
    toughness = 3

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.BEGIN_COMBAT)
        interveningIf = Conditions.FullParty()
        val permanent = target(TargetFilter.NonlandPermanentOpponentControls)
        effect = Effects.CantAttackOrBlock(permanent, Duration.UntilYourNextTurn) then
            Effects.GrantStaticAbility(
                ability = PreventActivatedAbilities(GameObjectFilter.Permanent.sourceItself()),
                target = permanent,
                duration = Duration.UntilYourNextTurn
            )
    }

    activatedAbility {
        cost = Costs.SacrificeSelf
        effect = Effects.ChooseAction(listOf(
            EffectChoice(
                label = "Creatures you control gain hexproof",
                effect = Effects.ForEachInGroup(
                    GroupFilter(GameObjectFilter.Creature.youControl()),
                    Effects.GrantKeyword(Keyword.HEXPROOF, EffectTarget.IterationEntity)
                )
            ),
            EffectChoice(
                label = "Creatures you control gain indestructible",
                effect = Effects.ForEachInGroup(
                    GroupFilter(GameObjectFilter.Creature.youControl()),
                    Effects.GrantKeyword(Keyword.INDESTRUCTIBLE, EffectTarget.IterationEntity)
                )
            )
        ))
        description = "Sacrifice Linvala: Choose hexproof or indestructible. Creatures you control gain " +
            "that ability until end of turn."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "226"
        artist = "Wayne Reynolds"
        imageUri = "https://cards.scryfall.io/normal/front/4/9/49b66956-82e1-402f-9088-2201bdc0d4b8.jpg?1783929323"
        ruling("2020-09-25", "Linvala's middle ability won't trigger at all if you don't have a full party as your combat phase begins. It won't affect the target if you don't have a full party as it resolves. The number of creatures in your party may change between these times, however.")
        ruling("2020-09-25", "The nonland permanent remains affected until your next turn even if the number of creatures in your party changes or Linvala leaves the battlefield.")
        ruling("2020-09-25", "No abilities of the affected permanent can be activated, including mana abilities.")
    }
}
