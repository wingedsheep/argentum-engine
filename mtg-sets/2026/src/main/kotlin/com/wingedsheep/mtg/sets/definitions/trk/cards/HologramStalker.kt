package com.wingedsheep.mtg.sets.definitions.trk.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

private const val HOLOGRAM_STALKER_ILLUSION_ART =
    "https://cards.scryfall.io/normal/front/d/7/d760eee8-e49a-4662-8bfc-fcf8ed2ad2d8.jpg?1793364353"

/**
 * Hologram Stalker
 * {4}{G}{G}
 * Creature — Hirogen Warrior
 * 2/2
 *
 * When this creature enters, return target permanent card from your graveyard to your hand.
 * Whenever a card leaves your graveyard, create a 3/3 colorless Illusion artifact creature token.
 * This ability triggers only once each turn.
 *
 * Because the second ability triggers only once each turn, "whenever a card leaves" and the
 * batched `oneOrMore(...).leaveYourGraveyard()` trigger resolve identically: the first departure
 * of the turn (including the card the enters ability returns) makes one Illusion, later ones
 * that turn make none.
 */
val HologramStalker = card("Hologram Stalker") {
    manaCost = "{4}{G}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Hirogen Warrior"
    power = 2
    toughness = 2
    oracleText = "When this creature enters, return target permanent card from your graveyard to your hand.\n" +
        "Whenever a card leaves your graveyard, create a 3/3 colorless Illusion artifact creature token. " +
        "This ability triggers only once each turn."

    triggeredAbility {
        trigger = Triggers.self.enters()
        val permanentCard = target(TargetFilter.PermanentInYourGraveyard)
        effect = Effects.ReturnToHand(permanentCard)
    }

    triggeredAbility {
        trigger = Triggers.oneOrMore(GameObjectFilter.Any).leaveYourGraveyard()
        oncePerTurn = true
        effect = Effects.CreateToken(
            power = 3, toughness = 3,
            creatureTypes = setOf("Illusion"),
            artifactToken = true,
            imageUri = HOLOGRAM_STALKER_ILLUSION_ART
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "202"
        artist = "Hector Ortiz"
        imageUri = "https://cards.scryfall.io/normal/front/6/b/6bd8a87b-ca7a-4a15-b96f-1a673ee44f3e.jpg?1792668500"
    }
}
