package com.wingedsheep.mtg.sets.definitions.emn.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.emerge
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Lashweed Lurker
 * {8}
 * Creature — Eldrazi Horror
 * 5/4
 *
 * Emerge {5}{G}{U}
 * When you cast this spell, you may put target nonland permanent on top of its owner's library.
 *
 * The tuck is a *cast* trigger: it resolves before the Lurker itself, and still resolves if the
 * spell is countered.
 */
val LashweedLurker = card("Lashweed Lurker") {
    manaCost = "{8}"
    colorIdentity = "GU"
    typeLine = "Creature — Eldrazi Horror"
    power = 5
    toughness = 4
    oracleText = "Emerge {5}{G}{U} (You may cast this spell by sacrificing a creature and paying the " +
        "emerge cost reduced by that creature's mana value.)\n" +
        "When you cast this spell, you may put target nonland permanent on top of its owner's library."

    emerge("{5}{G}{U}")

    triggeredAbility {
        trigger = Triggers.self.isCast()
        val permanent = target(TargetFilter.NonlandPermanent)
        effect = Effects.May(Effects.PutOnTopOfLibrary(permanent))
        description = "When you cast this spell, you may put target nonland permanent on top of its " +
            "owner's library."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "9"
        artist = "Igor Kieryluk"
        imageUri = "https://cards.scryfall.io/normal/front/a/1/a13f9a76-3370-4809-88ff-c300bca31c9e.jpg"
    }
}
