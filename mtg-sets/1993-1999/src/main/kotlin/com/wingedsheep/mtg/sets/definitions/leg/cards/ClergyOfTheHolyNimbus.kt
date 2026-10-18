package com.wingedsheep.mtg.sets.definitions.leg.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.scripting.ActivationRestriction
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.RegenerateInsteadOfDestruction
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Clergy of the Holy Nimbus
 * {W}
 * Creature — Human Cleric
 * 1/1
 * If this creature would be destroyed, regenerate it.
 * {1}: This creature can't be regenerated this turn. Only your opponents may activate this ability.
 *
 * Static regeneration (CR 701.19b) is never spent; the opponents' {1} switches it off for the turn.
 * "Only your opponents" opens the ability to every player and narrows it back with a condition whose
 * "you" is the activating player: they must be an opponent of the Clergy's controller.
 */
val ClergyOfTheHolyNimbus = card("Clergy of the Holy Nimbus") {
    manaCost = "{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Cleric"
    power = 1
    toughness = 1
    oracleText = "If this creature would be destroyed, regenerate it.\n" +
        "{1}: This creature can't be regenerated this turn. Only your opponents may activate this ability."

    replacementEffect(RegenerateInsteadOfDestruction())

    activatedAbility {
        cost = Costs.Mana("{1}")
        restrictions = listOf(
            ActivationRestriction.All(
                ActivationRestriction.AnyPlayerMay,
                ActivationRestriction.OnlyIfCondition(
                    Conditions.SourceMatches(GameObjectFilter.Any.opponentControls())
                )
            )
        )
        effect = Effects.CantBeRegenerated(EffectTarget.Self)
        description = "{1}: This creature can't be regenerated this turn. Only your opponents may activate this ability."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "6"
        artist = "Daniel Gelon"
        imageUri = "https://cards.scryfall.io/normal/front/d/b/db1f578f-fa3b-4447-953b-1490852b6c80.jpg?1783948087"
    }
}
