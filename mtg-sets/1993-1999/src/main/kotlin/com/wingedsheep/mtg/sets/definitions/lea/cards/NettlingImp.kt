package com.wingedsheep.mtg.sets.definitions.lea.cards

import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ActivationRestriction
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

val NettlingImp = card("Nettling Imp") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Imp"
    power = 1
    toughness = 1
    oracleText = "{T}: Choose target non-Wall creature the active player has controlled continuously since the beginning of the turn. That creature attacks this turn if able. Destroy it at the beginning of the next end step if it didn't attack this turn. Activate only during an opponent's turn, before attackers are declared."

    activatedAbility {
        cost = Costs.Tap
        restrictions = listOf(ActivationRestriction.OnlyIfCondition(
            Conditions.All(Conditions.IsOpponentsTurn, Conditions.BeforeAttackersDeclared)
        ))
        val creature = target(TargetFilter.Creature.notSubtype(Subtype("Wall"))
            .controlledByActivePlayer().controlledSinceTurnBegan())
        effect = Effects.MarkMustAttackThisTurn(creature) then Effects.CreateDelayedTrigger(
            step = Step.END,
            watchedTarget = creature,
            effect = Effects.If(
                Conditions.EntityMatches(EffectTarget.TriggeringEntity, GameObjectFilter.Any.didntAttackThisTurn()),
                Effects.Destroy(EffectTarget.TriggeringEntity)
            )
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "117"
        artist = "Quinton Hoover"
        imageUri = "https://cards.scryfall.io/normal/front/8/1/8105973c-a94d-444c-ba20-ab0fa978bee8.jpg?1783948692"
        ruling("2013-09-20", "If a turn has multiple combat phases, the ability can only be activated before the beginning of the declare attackers step of the first combat phase in that turn.")
        ruling("2004-10-04", "If the Imp leaves the battlefield before the end of the turn, the creature still is destroyed.")
        ruling("2004-10-04", "You can use this effect on a creature you know won’t be able to attack. For example, you can use it on a tapped creature.")
        ruling("2004-10-04", "The creature is destroyed if it does not attack because it simply can’t do so legally.")
    }
}
