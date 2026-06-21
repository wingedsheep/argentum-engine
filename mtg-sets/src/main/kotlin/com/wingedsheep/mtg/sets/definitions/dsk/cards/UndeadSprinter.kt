package com.wingedsheep.mtg.sets.definitions.dsk.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersWithCounters
import com.wingedsheep.sdk.scripting.MayCastSelfFromZones
import com.wingedsheep.sdk.scripting.events.CounterTypeFilter

/**
 * Undead Sprinter
 * {B}{R}
 * Creature — Zombie
 * 2/2
 * Trample, haste
 * You may cast this card from your graveyard if a non-Zombie creature died this turn.
 * If you do, this creature enters with a +1/+1 counter on it.
 */
val UndeadSprinter = card("Undead Sprinter") {
    manaCost = "{B}{R}"
    colorIdentity = "BR"
    typeLine = "Creature — Zombie"
    power = 2
    toughness = 2
    oracleText = "Trample, haste\nYou may cast this card from your graveyard if a non-Zombie creature died this turn. If you do, this creature enters with a +1/+1 counter on it."

    keywords(Keyword.TRAMPLE, Keyword.HASTE)

    // "You may cast this card from your graveyard if a non-Zombie creature died this turn."
    // A self-referential, conditional cast-from-graveyard permission. The card grants itself
    // permission to be cast from its own graveyard, gated on the global death tracker. Normal
    // (sorcery-speed) timing and the {B}{R} mana cost still apply.
    staticAbility {
        ability = MayCastSelfFromZones(
            zones = listOf(Zone.GRAVEYARD),
            condition = Conditions.NonSubtypeCreatureDiedThisTurn(Subtype.ZOMBIE)
        )
    }

    // "If you do, this creature enters with a +1/+1 counter on it." The counter rider is tied to
    // the graveyard cast — it only applies when this creature was cast from the graveyard, not when
    // cast normally from hand.
    replacementEffect(
        EntersWithCounters(
            counterType = CounterTypeFilter.PlusOnePlusOne,
            count = 1,
            selfOnly = true,
            condition = Conditions.WasCastFromGraveyard
        )
    )

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "237"
        artist = "Nino Vecia"
        flavorText = "Farley had always been the fastest on the track team. But no one was cheering anymore."
        imageUri = "https://cards.scryfall.io/normal/front/c/6/c6b96951-4884-4df7-bdf0-94be2bc044f0.jpg?1726286754"
    }
}
