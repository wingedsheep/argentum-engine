package com.wingedsheep.mtg.sets.definitions.grn.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Blood Operative
 * {1}{B}{B}
 * Creature — Vampire Assassin
 * 3/1
 * Lifelink
 * When this creature enters, you may exile target card from a graveyard.
 * Whenever you surveil, if this card is in your graveyard, you may pay 3 life. If you do, return
 * this card to your hand.
 *
 * The recursion trigger functions from the graveyard (`triggerZone = Zone.GRAVEYARD`), the same
 * shape as Pyre Zombie. The surveil event is emitted after the surveil finishes, so a Blood
 * Operative the surveil itself put into the graveyard sees it and triggers (per its ruling).
 */
val BloodOperative = card("Blood Operative") {
    manaCost = "{1}{B}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Vampire Assassin"
    oracleText = "Lifelink\n" +
        "When this creature enters, you may exile target card from a graveyard.\n" +
        "Whenever you surveil, if this card is in your graveyard, you may pay 3 life. " +
        "If you do, return this card to your hand."
    power = 3
    toughness = 1

    keywords(Keyword.LIFELINK)

    triggeredAbility {
        trigger = Triggers.self.enters()
        optional = true
        val exiled = target(TargetFilter.CardInGraveyard)
        effect = Effects.Move(exiled, Zone.EXILE)
    }

    triggeredAbility {
        trigger = Triggers.you.surveils()
        triggerZone = Zone.GRAVEYARD
        effect = Effects.MayPay(
            cost = Effects.PayLife(3),
            then = Effects.ReturnToHandFromGraveyard(EffectTarget.Self),
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "63"
        artist = "Livia Prima"
        imageUri = "https://cards.scryfall.io/normal/front/0/0/000ac9e5-3c95-4e87-9424-109e2eea6b45.jpg?1783934178"
    }
}
