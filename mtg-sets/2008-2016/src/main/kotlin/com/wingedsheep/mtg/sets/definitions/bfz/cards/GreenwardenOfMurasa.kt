package com.wingedsheep.mtg.sets.definitions.bfz.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Greenwarden of Murasa
 * {4}{G}{G}
 * Creature — Elemental
 * 5/4
 * When this creature enters, you may return target card from your graveyard to your hand.
 * When this creature dies, you may exile it. If you do, return target card from your graveyard to
 * your hand.
 *
 * The dies trigger targets as it goes on the stack, while Greenwarden is already in the graveyard, so
 * it may target itself. Both moves read `fromZone = GRAVEYARD`: "it" is the card that died, so if it
 * has already left the graveyard it is a new object (CR 400.7), the exile does nothing and "if you do"
 * fails; and a self-target exiled by the "if you do" is no longer in the graveyard, so nothing returns.
 */
val GreenwardenOfMurasa = card("Greenwarden of Murasa") {
    manaCost = "{4}{G}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Elemental"
    power = 5
    toughness = 4
    oracleText = "When this creature enters, you may return target card from your graveyard to your hand.\n" +
        "When this creature dies, you may exile it. If you do, return target card from your graveyard to your hand."

    triggeredAbility {
        trigger = Triggers.self.enters()
        val card = target(TargetFilter(GameObjectFilter.Any.ownedByYou(), zone = Zone.GRAVEYARD))
        effect = Effects.May(Effects.Move(card, Zone.HAND))
    }

    triggeredAbility {
        trigger = Triggers.self.dies()
        val card = target(TargetFilter(GameObjectFilter.Any.ownedByYou(), zone = Zone.GRAVEYARD))
        effect = Effects.May(
            Effects.IfYouDo(
                action = Effects.Move(EffectTarget.Self, Zone.EXILE, fromZone = Zone.GRAVEYARD),
                then = Effects.Move(card, Zone.HAND, fromZone = Zone.GRAVEYARD),
            ),
        )
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "174"
        artist = "Eric Deschamps"
        imageUri = "https://cards.scryfall.io/normal/front/5/b/5b03132c-0bb7-48f9-babd-6e106d8f202c.jpg?1783938188"
    }
}
