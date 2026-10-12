package com.wingedsheep.mtg.sets.definitions.bro.cards

import com.wingedsheep.sdk.core.CardType
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CopyExceptions
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Tawnos, the Toymaker — The Brothers' War #222.
 * {3}{G}{U} · Legendary Creature — Human Artificer · 3/5
 *
 * The copy is a copy of a permanent spell, so it resolves into a token (CR 707.10f) without being
 * "created". The artifact type is a copy exception, applied to the copy's copiable values before it
 * goes on the stack, so the resulting token stays an artifact for its whole life.
 */
val TawnosTheToymaker = card("Tawnos, the Toymaker") {
    manaCost = "{3}{G}{U}"
    colorIdentity = "GU"
    typeLine = "Legendary Creature — Human Artificer"
    power = 3
    toughness = 5
    oracleText = "Whenever you cast a Beast or Bird creature spell, you may copy it, except the copy is an artifact in addition to its other types. (The copy becomes a token.)"

    triggeredAbility {
        trigger = Triggers.you.casts(GameObjectFilter.Creature.withAnySubtype("Beast", "Bird"))
        effect = Effects.May(
            Effects.CopyTargetSpell(
                target = EffectTarget.TriggeringEntity,
                exceptions = CopyExceptions(addedCardTypes = setOf(CardType.ARTIFACT))
            )
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "222"
        artist = "Livia Prima"
        flavorText = "\"Don't thank me. Thank nature for its wondrous designs!\""
        imageUri = "https://cards.scryfall.io/normal/front/1/f/1f855a7e-3a97-4b62-a2a7-06ff1fbb2c4c.jpg?1783920024"
        ruling("2022-10-14", "A resolving copy of a permanent spell becomes a token, so the token isn't \"created.\" Effects that care about a token being created won't interact with a token that enters the battlefield from Tawnos, the Toymaker's ability.")
        ruling("2022-10-14", "The copy remembers any decisions that were made for the original spell as it was cast, including values chosen for X in its mana cost and whether any alternative or additional costs were chosen. For example, if the original creature spell had kicker and its kicker cost was paid, the copy will also be kicked.")
    }
}
