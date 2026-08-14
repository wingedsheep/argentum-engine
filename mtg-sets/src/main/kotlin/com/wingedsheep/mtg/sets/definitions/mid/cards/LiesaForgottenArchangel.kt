package com.wingedsheep.mtg.sets.definitions.mid.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.RedirectZoneChange
import com.wingedsheep.sdk.scripting.TriggerBinding
import com.wingedsheep.sdk.scripting.effects.CreateDelayedTriggerEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Liesa, Forgotten Archangel
 * {2}{W}{W}{B}
 * Legendary Creature — Angel
 * 4/5
 *
 * Flying, lifelink
 * Whenever another nontoken creature you control dies, return that card to its owner's hand at the
 * beginning of the next end step.
 * If a creature an opponent controls would die, exile it instead.
 */
val LiesaForgottenArchangel = card("Liesa, Forgotten Archangel") {
    manaCost = "{2}{W}{W}{B}"
    colorIdentity = "WB"
    typeLine = "Legendary Creature — Angel"
    power = 4
    toughness = 5
    oracleText = "Flying, lifelink\n" +
        "Whenever another nontoken creature you control dies, return that card to its owner's hand " +
        "at the beginning of the next end step.\n" +
        "If a creature an opponent controls would die, exile it instead."

    keywords(Keyword.FLYING, Keyword.LIFELINK)

    triggeredAbility {
        trigger = Triggers.leavesBattlefield(
            filter = GameObjectFilter.Creature.nontoken().youControl(),
            to = Zone.GRAVEYARD,
            binding = TriggerBinding.OTHER,
        )
        effect = CreateDelayedTriggerEffect(
            step = Step.END,
            effect = Effects.Move(
                EffectTarget.TriggeringEntity,
                Zone.HAND,
                fromZone = Zone.GRAVEYARD,
            ),
        )
    }

    replacementEffect(
        RedirectZoneChange(
            newDestination = Zone.EXILE,
            appliesTo = com.wingedsheep.sdk.scripting.EventPattern.ZoneChangeEvent(
                filter = GameObjectFilter.Creature.opponentControls(),
                from = Zone.BATTLEFIELD,
                to = Zone.GRAVEYARD,
            ),
        ),
    )

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "232"
        artist = "Dmitry Burmak"
        imageUri = "https://cards.scryfall.io/normal/front/5/3/532fca6b-f788-43f8-b29f-7273e7a48449.jpg?1783925558"
    }
}
