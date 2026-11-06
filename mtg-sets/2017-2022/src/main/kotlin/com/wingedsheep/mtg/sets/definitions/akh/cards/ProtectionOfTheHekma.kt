package com.wingedsheep.mtg.sets.definitions.akh.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.PreventDamage
import com.wingedsheep.sdk.scripting.events.Recipient

/**
 * Protection of the Hekma — Amonkhet #23
 * {4}{W} · Enchantment · Uncommon
 *
 * If a source an opponent controls would deal damage to you, prevent 1 of that damage.
 *
 * Same prevention shape as Valkmira, Protector's Shield, narrowed to the player only: each
 * opponent-controlled source loses 1 of the damage it would deal to you.
 */
val ProtectionOfTheHekma = card("Protection of the Hekma") {
    manaCost = "{4}{W}"
    colorIdentity = "W"
    typeLine = "Enchantment"
    oracleText = "If a source an opponent controls would deal damage to you, prevent 1 of that damage."

    replacementEffect(
        PreventDamage(
            amount = 1,
            appliesTo = EventPattern.DamageEvent(
                recipient = Recipient.You,
                source = GameObjectFilter.Any.opponentControls(),
            ),
        )
    )

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "23"
        artist = "Ryan Alexander Lee"
        flavorText = "Part of the acolytes' training is to walk along the Hekma, staring out at the " +
            "ravenous horrors kept at bay by the God-Pharaoh's barrier."
        imageUri = "https://cards.scryfall.io/normal/front/1/5/15d01e04-7c40-4af0-b5bd-5fa267d10378.jpg?1783936535"
        ruling("2017-04-18", "If multiple sources would deal damage to you at once (for example, several attacking creatures), 1 damage from each of those sources is prevented.")
        ruling("2017-04-18", "Damage that would be dealt to creatures you control is unaffected.")
    }
}
