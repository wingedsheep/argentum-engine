package com.wingedsheep.mtg.sets.definitions.mom.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.effects.WardCost
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Seal from Existence
 * {1}{W}{W}
 * Enchantment
 * Ward {3}
 * When this enchantment enters, exile target nonland permanent an opponent controls until this
 * enchantment leaves the battlefield.
 */
val SealFromExistence = card("Seal from Existence") {
    manaCost = "{1}{W}{W}"
    colorIdentity = "W"
    typeLine = "Enchantment"
    oracleText = "Ward {3} (Whenever this enchantment becomes the target of a spell or ability an " +
        "opponent controls, counter it unless that player pays {3}.)\n" +
        "When this enchantment enters, exile target nonland permanent an opponent controls until " +
        "this enchantment leaves the battlefield."

    keywordAbility(KeywordAbility.Ward(WardCost.Mana("{3}")))

    triggeredAbility {
        trigger = Triggers.self.enters()
        val t = target(TargetFilter.NonlandPermanentOpponentControls)
        effect = Effects.ExileUntilLeaves(t)
    }
    triggeredAbility {
        trigger = Triggers.self.leaves()
        effect = Effects.ReturnLinkedExileUnderOwnersControl()
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "35"
        artist = "Anato Finnstark"
        imageUri = "https://cards.scryfall.io/normal/front/2/c/2ccc29cc-025d-402e-9fe3-75998eb290c9.jpg?1783917049"
    }
}
