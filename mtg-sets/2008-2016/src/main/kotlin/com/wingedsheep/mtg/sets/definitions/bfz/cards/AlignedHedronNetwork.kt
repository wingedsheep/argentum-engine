package com.wingedsheep.mtg.sets.definitions.bfz.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Aligned Hedron Network
 * {4}
 * Artifact
 * When this artifact enters, exile all creatures with power 5 or greater until this artifact
 * leaves the battlefield. (Those creatures return under their owners' control.)
 *
 * The group form of `MoveUntilSourceLeaves`: every creature with power 5 or greater (projected,
 * checked on resolution, whoever controls it) is exiled in one move and they all return at once,
 * under their owners' control, the moment the Network leaves — a one-shot return, not a trigger.
 * If the Network has already left when the ability resolves, nothing is exiled; if it is itself a
 * creature with power 5 or greater, it exiles itself with the rest and they all return immediately.
 */
val AlignedHedronNetwork = card("Aligned Hedron Network") {
    manaCost = "{4}"
    colorIdentity = ""
    typeLine = "Artifact"
    oracleText = "When this artifact enters, exile all creatures with power 5 or greater until this artifact leaves the battlefield. (Those creatures return under their owners' control.)"

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.MoveUntilSourceLeaves(
            GroupFilter(GameObjectFilter.Creature.powerAtLeast(5)),
            Zone.EXILE
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "222"
        artist = "Richard Wright"
        flavorText = "The last hedron slotted into place, locking Ulamog in an infinite loop of binding energy."
        imageUri = "https://cards.scryfall.io/normal/front/c/0/c0ef436b-e82e-4e1f-9da0-5e35f0c37bde.jpg?1783938178"
        ruling("2015-08-25", "Check the power of each creature as Aligned Hedron Network's ability resolves to determine if it's exiled. It doesn't matter who controls the creature.")
        ruling("2015-08-25", "If Aligned Hedron Network exiles multiple creatures, those cards all return to the battlefield at the same time.")
        ruling("2015-08-25", "If Aligned Hedron Network leaves the battlefield before its triggered ability resolves, no creatures will be exiled.")
        ruling("2015-08-25", "Auras attached to the exiled creatures will be put into their owners' graveyards. Equipment attached to the exiled creatures will become unattached and remain on the battlefield. Any counters on the exiled creatures will cease to exist.")
        ruling("2015-08-25", "If a creature token is exiled, it ceases to exist. It won't be returned to the battlefield.")
        ruling("2015-08-25", "The exiled cards return to the battlefield immediately after Aligned Hedron Network leaves the battlefield. Nothing happens between the two events, including state-based actions.")
        ruling("2015-08-25", "In a multiplayer game, if Aligned Hedron Network's owner leaves the game, the exiled cards will return to the battlefield. Because the one-shot effect that returns the cards isn't an ability that goes on the stack, it won't cease to exist along with the leaving player's spells and abilities on the stack.")
        ruling("2015-08-25", "In some very rare situations, Aligned Hedron Network may enter the battlefield as a creature with power 5 or greater. If this happens, Aligned Hedron Network will exile itself along with other creatures with power 5 or greater. Those cards will immediately return to the battlefield. If this causes a loop with Aligned Hedron Network continually exiling and returning itself, the game will be a draw unless a player breaks the loop somehow.")
    }
}
