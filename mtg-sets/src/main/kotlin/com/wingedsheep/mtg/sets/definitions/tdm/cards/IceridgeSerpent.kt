package com.wingedsheep.mtg.sets.definitions.tdm.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Iceridge Serpent — Tarkir: Dragonstorm #49
 * {4}{U} · Creature — Serpent · 3/3
 *
 * When this creature enters, return target creature an opponent controls to its
 * owner's hand.
 */
val IceridgeSerpent = card("Iceridge Serpent") {
    manaCost = "{4}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Serpent"
    power = 3
    toughness = 3
    oracleText = "When this creature enters, return target creature an opponent controls to its owner's hand."

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        val creature = target("creature", Targets.CreatureOpponentControls)
        effect = Effects.ReturnToHand(creature)
        description = "When this creature enters, return target creature an opponent controls to its owner's hand."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "49"
        artist = "Brian Valeza"
        flavorText = "\"Even dragons avoid Glintglaze Lake in this season. Which of you youngsters can tell me why?\"\n—Teacher Eliam"
        imageUri = "https://cards.scryfall.io/normal/front/d/1/d13f117b-b8e4-48db-8ce9-5da9c7ce23a5.jpg?1743204156"
    }
}
