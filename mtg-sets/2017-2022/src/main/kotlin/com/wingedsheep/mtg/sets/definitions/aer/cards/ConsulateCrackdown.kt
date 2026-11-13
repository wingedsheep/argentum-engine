package com.wingedsheep.mtg.sets.definitions.aer.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Consulate Crackdown — Aether Revolt #11
 * {3}{W}{W}
 * Enchantment
 * When this enchantment enters, exile all artifacts your opponents control until this
 * enchantment leaves the battlefield.
 *
 * Same shape as Temporary Lockdown: the group form of `MoveUntilSourceLeaves` exiles every
 * artifact in one move and returns them all under their owners' control the moment the
 * enchantment leaves — a one-shot return, not a trigger. Nothing is exiled if the enchantment has
 * already left by the time its enters ability resolves.
 */
val ConsulateCrackdown = card("Consulate Crackdown") {
    manaCost = "{3}{W}{W}"
    colorIdentity = "W"
    typeLine = "Enchantment"
    oracleText = "When this enchantment enters, exile all artifacts your opponents control until this enchantment leaves the battlefield."

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.MoveUntilSourceLeaves(
            GroupFilter(GameObjectFilter.Artifact.opponentControls()),
            Zone.EXILE
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "11"
        artist = "Jonas De Ro"
        flavorText = "\"The workshops are silent. Our creations have been seized. They have killed what made us alive.\"\n—Pia Nalaar"
        imageUri = "https://cards.scryfall.io/normal/front/0/2/02fb7a26-fbf0-4b91-847b-cfc46e01a342.jpg?1783936782"
        ruling("2017-02-09", "If Consulate Crackdown leaves the battlefield before its triggered ability resolves, no artifacts will be exiled.")
        ruling("2017-02-09", "Auras attached to the exiled artifacts will be put into their owners' graveyards. Any counters on the exiled artifacts will cease to exist.")
        ruling("2017-02-09", "If an artifact token is exiled, it ceases to exist. It won't be returned to the battlefield.")
        ruling("2017-02-09", "The exiled cards return to the battlefield immediately after Consulate Crackdown leaves the battlefield. Nothing happens between the two events, including state-based actions. If Consulate Crackdown exiles multiple artifacts, those cards all return to the battlefield at the same time.")
        ruling("2017-02-09", "In a multiplayer game, if Consulate Crackdown's owner leaves the game, the exiled cards will return to the battlefield.")
    }
}
