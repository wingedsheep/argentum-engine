package com.wingedsheep.mtg.sets.definitions.akh.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.embalm
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Angel of Sanctions
 * {3}{W}{W}
 * Creature — Angel
 * 3/4
 * Flying
 * When this creature enters, you may exile target nonland permanent an opponent controls until
 * this creature leaves the battlefield.
 * Embalm {5}{W}
 *
 * The [CastOut] linked-exile pair (optional, like Suspension Field); the embalmed token carries
 * both triggers, so it exiles and returns on its own.
 */
val AngelOfSanctions = card("Angel of Sanctions") {
    manaCost = "{3}{W}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Angel"
    power = 3
    toughness = 4
    oracleText = "Flying\n" +
        "When this creature enters, you may exile target nonland permanent an opponent controls until this creature leaves the battlefield.\n" +
        "Embalm {5}{W} ({5}{W}, Exile this card from your graveyard: Create a token that's a copy of it, " +
        "except it's a white Zombie Angel with no mana cost. Embalm only as a sorcery.)"

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.self.enters()
        val permanent = target(TargetFilter.NonlandPermanentOpponentControls)
        effect = Effects.ExileUntilLeaves(permanent)
        optional = true
    }

    triggeredAbility {
        trigger = Triggers.self.leaves()
        effect = Effects.ReturnLinkedExileUnderOwnersControl()
    }

    embalm("{5}{W}")

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "1"
        artist = "Min Yum"
        imageUri = "https://cards.scryfall.io/normal/front/6/6/66b1cbf0-1273-406a-bc99-ce20ce53e5aa.jpg?1783936545"
    }
}
