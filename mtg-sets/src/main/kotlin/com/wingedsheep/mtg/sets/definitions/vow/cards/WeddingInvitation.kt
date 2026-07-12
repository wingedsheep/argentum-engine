package com.wingedsheep.mtg.sets.definitions.vow.cards

import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.ConditionalEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Wedding Invitation
 * {2}
 * Artifact
 *
 * When this artifact enters, draw a card.
 * {T}, Sacrifice this artifact: Target creature can't be blocked this turn. If it's a Vampire,
 * it also gains lifelink until end of turn.
 */
val WeddingInvitation = card("Wedding Invitation") {
    manaCost = "{2}"
    colorIdentity = ""
    typeLine = "Artifact"
    oracleText = "When this artifact enters, draw a card.\n" +
        "{T}, Sacrifice this artifact: Target creature can't be blocked this turn. If it's a " +
        "Vampire, it also gains lifelink until end of turn."

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = Effects.DrawCards(1)
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Tap, Costs.SacrificeSelf)
        val creature = target("target creature", Targets.Creature)
        effect = Effects.Composite(
            Effects.GrantKeyword(AbilityFlag.CANT_BE_BLOCKED, creature, Duration.EndOfTurn),
            ConditionalEffect(
                condition = Conditions.TargetMatchesFilter(
                    GameObjectFilter.Creature.withSubtype(Subtype("Vampire"))
                ),
                effect = Effects.GrantKeyword(Keyword.LIFELINK, creature, Duration.EndOfTurn),
            ),
        )
        description = "{T}, Sacrifice this artifact: Target creature can't be blocked this turn. " +
            "If it's a Vampire, it also gains lifelink until end of turn."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "260"
        artist = "Justyna Dura"
        flavorText = "RSVP at your own risk."
        imageUri = "https://cards.scryfall.io/normal/front/d/d/ddc22ff6-4081-47ce-bc8a-e063f5f4d044.jpg?1782703014"
    }
}
