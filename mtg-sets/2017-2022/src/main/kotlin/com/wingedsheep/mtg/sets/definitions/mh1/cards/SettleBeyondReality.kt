package com.wingedsheep.mtg.sets.definitions.mh1.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Settle Beyond Reality
 * {4}{W}
 * Sorcery
 * Choose one or both —
 * • Exile target creature you don't control.
 * • Exile target creature you control, then return it to the battlefield under its owner's control.
 *
 * "Choose one or both" is the mode *count* (`chooseCount = 2, minChooseCount = 1`, CR 700.2), not a
 * third "both" mode — same shape as Winterflame. "You don't control" is
 * [TargetFilter.CreatureOpponentControls], whose controller predicate is "controller isn't you".
 * The blink half is Flicker of Fate's `Exile then Move(BATTLEFIELD)`; the move returns the card under
 * its owner's control, and a token that left the battlefield ceases to exist rather than returning.
 */
val SettleBeyondReality = card("Settle Beyond Reality") {
    manaCost = "{4}{W}"
    colorIdentity = "W"
    typeLine = "Sorcery"
    oracleText = "Choose one or both —\n• Exile target creature you don't control.\n" +
        "• Exile target creature you control, then return it to the battlefield under its owner's control."

    spell {
        modal(chooseCount = 2, minChooseCount = 1) {
            mode("Exile target creature you don't control") {
                val theirs = target(TargetFilter.CreatureOpponentControls)
                effect = Effects.Exile(theirs)
            }
            mode("Exile target creature you control, then return it to the battlefield under its owner's control") {
                val yours = target(TargetFilter.CreatureYouControl)
                effect = Effects.Exile(yours) then Effects.Move(yours, Zone.BATTLEFIELD)
            }
        }
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "27"
        artist = "Anthony Palumbo"
        flavorText = "Two taken, two judged, one returned."
        imageUri = "https://cards.scryfall.io/normal/front/7/2/72ed8e57-61bb-4e89-9484-ff2be800a449.jpg?1783933156"

        ruling("2019-06-14", "Once the exiled creature you control returns, it's considered a new object with no relation to the object that it was. Auras attached to the exiled creatures will be put into their owners' graveyards. Equipment attached to the exiled creatures will become unattached and remain on the battlefield. Any counters on the exiled creatures will cease to exist.")
        ruling("2019-06-14", "If a token you control is exiled this way, it will cease to exist and won't return to the battlefield.")
    }
}
