package com.wingedsheep.mtg.sets.definitions.ogw.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Isolation Zone
 * {2}{W}{W}
 * Enchantment
 * When this enchantment enters, exile target creature or enchantment an opponent controls until
 * this enchantment leaves the battlefield. (That permanent returns under its owner's control.)
 */
val IsolationZone = card("Isolation Zone") {
    manaCost = "{2}{W}{W}"
    colorIdentity = "W"
    typeLine = "Enchantment"
    oracleText = "When this enchantment enters, exile target creature or enchantment an opponent controls " +
        "until this enchantment leaves the battlefield. (That permanent returns under its owner's control.)"

    triggeredAbility {
        trigger = Triggers.self.enters()
        val t = target(TargetFilter.CreatureOrEnchantment.opponentControls())
        effect = Effects.ExileUntilLeaves(t)
    }

    triggeredAbility {
        trigger = Triggers.self.leaves()
        effect = Effects.ReturnLinkedExileUnderOwnersControl()
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "22"
        artist = "Chris Rallis"
        flavorText = "When properly aligned, hedrons become powerful tools of binding."
        imageUri = "https://cards.scryfall.io/normal/front/1/c/1ca698e3-1249-4a08-be04-bae3bd944e99.jpg"
    }
}
