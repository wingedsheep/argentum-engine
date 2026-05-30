package com.wingedsheep.mtg.sets.definitions.tdm.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Wayspeaker Bodyguard
 * {3}{W}
 * Creature — Orc Monk
 * 3/4
 *
 * When this creature enters, return target nonland permanent card with mana value 2 or
 * less from your graveyard to your hand.
 * Flurry — Whenever you cast your second spell each turn, tap target creature an opponent
 * controls.
 */
val WayspeakerBodyguard = card("Wayspeaker Bodyguard") {
    manaCost = "{3}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Orc Monk"
    power = 3
    toughness = 4
    oracleText = "When this creature enters, return target nonland permanent card with mana value 2 or less from your graveyard to your hand.\nFlurry — Whenever you cast your second spell each turn, tap target creature an opponent controls."

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        val card = target(
            "card",
            TargetObject(
                filter = TargetFilter(
                    baseFilter = GameObjectFilter.NonlandPermanent.ownedByYou().manaValueAtMost(2),
                    zone = Zone.GRAVEYARD
                )
            )
        )
        effect = Effects.ReturnToHand(card)
    }

    flurry {
        val creature = target("creature", Targets.CreatureOpponentControls)
        effect = Effects.Tap(creature)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "34"
        artist = "Inkognit"
        imageUri = "https://cards.scryfall.io/normal/front/5/e/5e5b2324-69fe-4105-b6f8-14dfbe359d59.jpg?1743204097"
    }
}
