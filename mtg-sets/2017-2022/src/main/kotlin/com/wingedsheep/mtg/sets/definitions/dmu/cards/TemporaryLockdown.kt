package com.wingedsheep.mtg.sets.definitions.dmu.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Temporary Lockdown
 * {1}{W}{W}
 * Enchantment
 * When this enchantment enters, exile each nonland permanent with mana value 2 or less until this
 * enchantment leaves the battlefield.
 *
 * The group form of `MoveUntilSourceLeaves` exiles the pile in one move and returns it under its
 * owners' control the moment the enchantment leaves (a one-shot return, not a trigger). Per the
 * ruling nothing is exiled if Temporary Lockdown has already left by the time its enters ability
 * resolves.
 */
val TemporaryLockdown = card("Temporary Lockdown") {
    manaCost = "{1}{W}{W}"
    colorIdentity = "W"
    typeLine = "Enchantment"
    oracleText = "When this enchantment enters, exile each nonland permanent with mana value 2 or less until this enchantment leaves the battlefield."

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.MoveUntilSourceLeaves(
            GroupFilter(GameObjectFilter.NonlandPermanent.manaValueAtMost(2)),
            Zone.EXILE
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "36"
        artist = "Bryan Sola"
        flavorText = "\"Any one of us could be compromised,\" said Karn. \"No one leaves until we know who can be trusted.\""
        imageUri = "https://cards.scryfall.io/normal/front/8/2/82b3088f-7b49-45e9-b447-129a597ceb75.jpg?1783921358"
        ruling("2022-09-09", "If Temporary Lockdown leaves the battlefield before its enters-the-battlefield ability resolves, no permanents will be exiled.")
        ruling("2022-09-09", "Auras with mana value 3 or greater attached to an exiled permanent will be put into their owners' graveyards. Equipment with mana value 3 or greater attached to an exiled creature will become unattached and remain on the battlefield. Any counters on the exiled permanent will cease to exist.")
        ruling("2022-09-09", "Aura cards exiled this way will return to the battlefield attached to a permanent that they could enchant (based on their enchant ability) chosen by their owner. They cannot enchant any permanents that are entering the battlefield at the same time.")
        ruling("2022-09-09", "If a token is exiled, it ceases to exist. It won't be returned to the battlefield. The mana value of a token that isn't a copy of another permanent is always 0.")
        ruling("2022-09-09", "In a multiplayer game, if Temporary Lockdown's owner leaves the game, the exiled cards will return to the battlefield.")
    }
}
