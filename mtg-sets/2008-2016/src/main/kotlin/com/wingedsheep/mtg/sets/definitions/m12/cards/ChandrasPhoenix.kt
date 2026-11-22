package com.wingedsheep.mtg.sets.definitions.m12.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.events.Recipient
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Chandra's Phoenix
 * {1}{R}{R}
 * Creature — Phoenix
 * 2/2
 *
 * Flying, haste
 * Whenever an opponent is dealt damage by a red instant or sorcery spell you control or by a red
 * planeswalker you control, return this card from your graveyard to your hand.
 *
 * The trigger functions only from the graveyard (`triggerZone = Zone.GRAVEYARD`, as Squee, Goblin
 * Nabob), observing damage to an opponent (Chandra's Spitfire's `Triggers.a(...).dealsDamage(
 * Recipient.Opponent)` shape) with the damage source narrowed to the two red sources you control.
 */
private val redSpellOrPlaneswalkerYouControl =
    GameObjectFilter.InstantOrSorcery.withColor(Color.RED).youControl() or
        GameObjectFilter.Planeswalker.withColor(Color.RED).youControl()

val ChandrasPhoenix = card("Chandra's Phoenix") {
    manaCost = "{1}{R}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Phoenix"
    power = 2
    toughness = 2
    oracleText = "Flying\n" +
        "Haste (This creature can attack and {T} as soon as it comes under your control.)\n" +
        "Whenever an opponent is dealt damage by a red instant or sorcery spell you control or by a " +
        "red planeswalker you control, return this card from your graveyard to your hand."

    keywords(Keyword.FLYING, Keyword.HASTE)

    triggeredAbility {
        trigger = Triggers.a(redSpellOrPlaneswalkerYouControl).dealsDamage(Recipient.Opponent)
        triggerZone = Zone.GRAVEYARD
        effect = Effects.Move(EffectTarget.Self, Zone.HAND, fromZone = Zone.GRAVEYARD)
        description = "Whenever an opponent is dealt damage by a red instant or sorcery spell you control " +
            "or by a red planeswalker you control, return this card from your graveyard to your hand."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "126"
        artist = "Aleksi Briclot"
        imageUri = "https://cards.scryfall.io/normal/front/e/8/e8371c83-d7c5-4432-8511-cb3c1dc7d59f.jpg?1783941073"
    }
}
