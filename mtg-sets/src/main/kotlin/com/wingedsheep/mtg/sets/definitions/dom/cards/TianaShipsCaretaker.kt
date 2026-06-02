package com.wingedsheep.mtg.sets.definitions.dom.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TriggerBinding
import com.wingedsheep.sdk.scripting.TriggerSpec
import com.wingedsheep.sdk.scripting.EventPattern.ZoneChangeEvent
import com.wingedsheep.sdk.scripting.effects.CreateDelayedTriggerEffect
import com.wingedsheep.sdk.scripting.effects.MayEffect
import com.wingedsheep.sdk.scripting.predicates.CardPredicate
import com.wingedsheep.sdk.scripting.predicates.ControllerPredicate
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.dsl.Effects

/**
 * Tiana, Ship's Caretaker
 * {3}{R}{W}
 * Legendary Creature — Angel Artificer
 * 3/3
 * Flying, first strike
 * Whenever an Aura or Equipment you control is put into a graveyard from the
 * battlefield, you may return that card to its owner's hand at the beginning
 * of the next end step.
 */
val TianaShipsCaretaker = card("Tiana, Ship's Caretaker") {
    manaCost = "{3}{R}{W}"
    colorIdentity = "WR"
    typeLine = "Legendary Creature — Angel Artificer"
    power = 3
    toughness = 3
    oracleText = "Flying, first strike\nWhenever an Aura or Equipment you control is put into a graveyard from the battlefield, you may return that card to its owner's hand at the beginning of the next end step."

    keywords(Keyword.FLYING, Keyword.FIRST_STRIKE)

    triggeredAbility {
        trigger = TriggerSpec(
            event = ZoneChangeEvent(
                filter = GameObjectFilter(
                    cardPredicates = listOf(
                        CardPredicate.Or(listOf(
                            CardPredicate.HasSubtype(Subtype.AURA),
                            CardPredicate.HasSubtype(Subtype.EQUIPMENT)
                        ))
                    ),
                    controllerPredicate = ControllerPredicate.ControlledByYou
                ),
                from = Zone.BATTLEFIELD,
                to = Zone.GRAVEYARD
            ),
            binding = TriggerBinding.ANY
        )
        effect = MayEffect(
            CreateDelayedTriggerEffect(
                step = Step.END,
                effect = Effects.Move(
                    target = EffectTarget.TriggeringEntity,
                    destination = Zone.HAND
                )
            )
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "208"
        artist = "Eric Deschamps"
        flavorText = "\"Nothing is too broken to mend.\""
        imageUri = "https://cards.scryfall.io/normal/front/9/a/9a8aea2f-1e1d-4e0d-8370-207b6cae76e3.jpg?1562911270"
        ruling("2018-04-27", "Tiana's last ability triggers and creates a delayed triggered ability that will let you return the Aura or Equipment during the next end step.")
        ruling("2018-04-27", "If Tiana leaves the battlefield at the same time as an Aura or Equipment you control, Tiana's ability won't trigger for that Aura or Equipment.")
        ruling("2018-04-27", "If an Aura or Equipment you control is put into a graveyard at the same time as Tiana, you'll be able to return it at the next end step.")
        ruling("2018-04-27", "If an Aura or Equipment enters the graveyard during an end step, you'll be able to return it during the next turn's end step.")
        ruling("2018-04-27", "If an Aura or Equipment leaves the graveyard after triggering Tiana's last ability, it won't be returned to its owner's hand.")
    }
}
