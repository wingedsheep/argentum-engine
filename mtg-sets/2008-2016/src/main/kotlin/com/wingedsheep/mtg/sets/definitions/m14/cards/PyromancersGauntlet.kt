package com.wingedsheep.mtg.sets.definitions.m14.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifyDamageAmount
import com.wingedsheep.sdk.scripting.events.Recipient

/**
 * Pyromancer's Gauntlet
 * {5}
 * Artifact
 *
 * If a red instant or sorcery spell you control or a red planeswalker you control would deal damage
 * to a permanent or player, it deals that much damage plus 2 to that permanent or player instead.
 *
 * Torbran's additive [ModifyDamageAmount] (+2, dealt by the original source) with Chandra's Phoenix's
 * two-branch source filter, and the unscoped [Recipient.Any] for "a permanent or player".
 */
private val redSpellOrPlaneswalkerYouControl =
    GameObjectFilter.InstantOrSorcery.withColor(Color.RED).youControl() or
        GameObjectFilter.Planeswalker.withColor(Color.RED).youControl()

val PyromancersGauntlet = card("Pyromancer's Gauntlet") {
    manaCost = "{5}"
    colorIdentity = ""
    typeLine = "Artifact"
    oracleText = "If a red instant or sorcery spell you control or a red planeswalker you control would " +
        "deal damage to a permanent or player, it deals that much damage plus 2 to that permanent or " +
        "player instead."

    replacementEffect(
        ModifyDamageAmount(
            modifier = 2,
            appliesTo = EventPattern.DamageEvent(
                source = redSpellOrPlaneswalkerYouControl,
                recipient = Recipient.Any,
            ),
        )
    )

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "214"
        artist = "Christine Choi"
        flavorText = "An inferno is just a gesture away."
        imageUri = "https://cards.scryfall.io/normal/front/9/b/9bde6763-2102-4adb-8048-fc9fe921205b.jpg?1783939895"
        ruling("2013-07-01", "Pyromancer's Gauntlet doesn't change the source or recipient of the damage.")
        ruling("2013-07-01", "If all damage from a source is prevented (because of Pay No Heed, for example) or if a source would deal 0 damage, the effect of Pyromancer's Gauntlet will not apply as that source is no longer dealing damage.")
        ruling("2013-07-01", "If damage is being dealt to multiple players and/or permanents at the same time, the damage being dealt to each one is increased by 2.")
    }
}
