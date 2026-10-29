package com.wingedsheep.mtg.sets.definitions.nph.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Suture Priest
 * {1}{W}
 * Creature — Phyrexian Cleric
 * 1/1
 * Whenever another creature you control enters, you may gain 1 life.
 * Whenever a creature an opponent controls enters, you may have that player lose 1 life.
 *
 * "That player" is the entering creature's controller ([EffectTarget.ControllerOfTriggeringEntity],
 * Ankh of Mishra's shape); both triggers are optional.
 */
val SuturePriest = card("Suture Priest") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Phyrexian Cleric"
    power = 1
    toughness = 1
    oracleText = "Whenever another creature you control enters, you may gain 1 life.\n" +
        "Whenever a creature an opponent controls enters, you may have that player lose 1 life."

    triggeredAbility {
        trigger = Triggers.another(GameObjectFilter.Creature.youControl()).enters()
        optional = true
        effect = Effects.GainLife(1)
        description = "Whenever another creature you control enters, you may gain 1 life."
    }

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Creature.opponentControls()).enters()
        optional = true
        effect = Effects.LoseLife(1, EffectTarget.ControllerOfTriggeringEntity)
        description = "Whenever a creature an opponent controls enters, you may have that player lose 1 life."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "25"
        artist = "Igor Kieryluk"
        imageUri = "https://cards.scryfall.io/normal/front/3/1/31432e98-86cd-42ea-ad37-eb4383dc6a81.jpg?1783941323"
    }
}
