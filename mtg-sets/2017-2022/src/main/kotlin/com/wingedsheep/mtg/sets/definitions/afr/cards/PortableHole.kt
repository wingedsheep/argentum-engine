package com.wingedsheep.mtg.sets.definitions.afr.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Portable Hole
 * {W}
 * Artifact
 * When this artifact enters, exile target nonland permanent an opponent controls with mana value 2 or less
 * until this artifact leaves the battlefield.
 */
val PortableHole = card("Portable Hole") {
    manaCost = "{W}"
    colorIdentity = "W"
    typeLine = "Artifact"
    oracleText = "When this artifact enters, exile target nonland permanent an opponent controls with mana value 2 or less until this artifact leaves the battlefield."
    triggeredAbility {
        trigger = Triggers.self.enters()
        val t = target(TargetFilter.NonlandPermanentOpponentControls.manaValueAtMost(2))
        effect = Effects.ExileUntilLeaves(t)
    }
    triggeredAbility {
        trigger = Triggers.self.leaves()
        effect = Effects.ReturnLinkedExileUnderOwnersControl()
    }
    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "33"
        artist = "John Stanko"
        flavorText = "\"They fall for it every time.\""
        imageUri = "https://cards.scryfall.io/normal/front/8/0/80fca8c0-ae3e-439e-b202-228b9f360e9a.jpg?1783926525"
    }
}
