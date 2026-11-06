package com.wingedsheep.mtg.sets.definitions.aer.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Thopter Arrest
 * {2}{W}
 * Enchantment
 * When this enchantment enters, exile target artifact or creature an opponent controls until this
 * enchantment leaves the battlefield.
 */
val ThopterArrest = card("Thopter Arrest") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Enchantment"
    oracleText = "When this enchantment enters, exile target artifact or creature an opponent controls until this enchantment leaves the battlefield."

    triggeredAbility {
        trigger = Triggers.self.enters()
        val t = target(TargetFilter((GameObjectFilter.Artifact or GameObjectFilter.Creature).opponentControls()))
        effect = Effects.ExileUntilLeaves(t)
    }

    triggeredAbility {
        trigger = Triggers.self.leaves()
        effect = Effects.ReturnLinkedExileUnderOwnersControl()
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "25"
        artist = "Kieran Yanner"
        flavorText = "The Consulate's eyes—unblinking, unsleeping—are everywhere."
        imageUri = "https://cards.scryfall.io/normal/front/a/4/a4e738cb-e4ea-41c2-99a1-55b6167eccb0.jpg"
        ruling("2017-02-09", "If Thopter Arrest leaves the battlefield before its triggered ability resolves, the target permanent won’t be exiled.")
        ruling("2017-02-09", "Auras attached to the exiled permanent will be put into their owners’ graveyards. Any Equipment will become unattached and remain on the battlefield. Any counters on the exiled permanent will cease to exist.")
        ruling("2017-02-09", "If a token is exiled this way, it will cease to exist and won’t return to the battlefield.")
        ruling("2017-02-09", "The exiled card returns to the battlefield immediately after Thopter Arrest leaves the battlefield. Nothing happens between the two events, including state-based actions.")
        ruling("2017-02-09", "In a multiplayer game, if Thopter Arrest’s owner leaves the game, the exiled card will return to the battlefield. Because the one-shot effect that returns the card isn’t an ability that goes on the stack, it won’t cease to exist along with the leaving player’s spells and abilities on the stack.")
    }
}
