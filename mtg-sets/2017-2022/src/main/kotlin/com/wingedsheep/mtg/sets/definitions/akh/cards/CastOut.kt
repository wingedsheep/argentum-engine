package com.wingedsheep.mtg.sets.definitions.akh.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Cast Out
 * {3}{W}
 * Enchantment
 * Flash
 * When this enchantment enters, exile target nonland permanent an opponent controls until this
 * enchantment leaves the battlefield.
 * Cycling {W} ({W}, Discard this card: Draw a card.)
 *
 * The [com.wingedsheep.mtg.sets.definitions.jou.cards.BanishingLight] pair: an enters trigger
 * exiling into the source's linked-exile set, and a leaves trigger returning it under its owner's
 * control.
 */
val CastOut = card("Cast Out") {
    manaCost = "{3}{W}"
    colorIdentity = "W"
    typeLine = "Enchantment"
    oracleText = "Flash\n" +
        "When this enchantment enters, exile target nonland permanent an opponent controls until this enchantment leaves the battlefield.\n" +
        "Cycling {W} ({W}, Discard this card: Draw a card.)"

    keywords(Keyword.FLASH)

    triggeredAbility {
        trigger = Triggers.self.enters()
        val permanent = target(TargetFilter.NonlandPermanentOpponentControls)
        effect = Effects.ExileUntilLeaves(permanent)
    }

    triggeredAbility {
        trigger = Triggers.self.leaves()
        effect = Effects.ReturnLinkedExileUnderOwnersControl()
    }

    keywordAbility(KeywordAbility.cycling("{W}"))

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "8"
        artist = "Magali Villeneuve"
        imageUri = "https://cards.scryfall.io/normal/front/7/6/76ff0dd0-489e-4773-93f6-b77fc3b8ef2e.jpg"
    }
}
