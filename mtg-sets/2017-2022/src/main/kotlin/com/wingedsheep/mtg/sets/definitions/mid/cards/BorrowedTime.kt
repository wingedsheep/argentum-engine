package com.wingedsheep.mtg.sets.definitions.mid.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Borrowed Time (Innistrad: Midnight Hunt #6)
 * {2}{W}
 * Enchantment
 * When this enchantment enters, exile target nonland permanent an opponent controls until this
 * enchantment leaves the battlefield.
 */
val BorrowedTime = card("Borrowed Time") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Enchantment"
    oracleText = "When this enchantment enters, exile target nonland permanent an opponent controls until this enchantment leaves the battlefield."

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
        collectorNumber = "6"
        artist = "Andreas Zafiratos"
        flavorText = "Teferi stepped calmly into the fray. Suddenly the sun paused in the sky and the werewolves' snarls froze on their faces."
        imageUri = "https://cards.scryfall.io/normal/front/3/e/3eeef24b-b937-408e-a32e-1a546d3e7b0f.jpg"
    }
}
