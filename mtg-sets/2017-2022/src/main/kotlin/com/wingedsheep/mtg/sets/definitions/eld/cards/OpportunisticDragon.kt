package com.wingedsheep.mtg.sets.definitions.eld.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Opportunistic Dragon — Throne of Eldraine #133
 * {2}{R}{R} · Creature — Dragon · 4/3
 *
 * Flying
 * When this creature enters, choose target Human or artifact an opponent controls. For as long as
 * this creature remains on the battlefield, gain control of that permanent, it loses all
 * abilities, and it can't attack or block.
 *
 * All three halves share one target and one CR 611.2b duration, [Duration.WhileSourceOnBattlefield]
 * keyed to the Dragon: the control change (layer 2), the ability loss (layer 6) and the combat lock
 * all end together the instant the Dragon leaves. If the Dragon is gone before the trigger
 * resolves, the duration has already ended and nothing happens at all (the card's ruling).
 */
val OpportunisticDragon = card("Opportunistic Dragon") {
    manaCost = "{2}{R}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Dragon"
    power = 4
    toughness = 3
    oracleText = "Flying\n" +
        "When this creature enters, choose target Human or artifact an opponent controls. For as " +
        "long as this creature remains on the battlefield, gain control of that permanent, it " +
        "loses all abilities, and it can't attack or block."

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.self.enters()
        val stolen = target(
            TargetFilter((GameObjectFilter.Artifact or GameObjectFilter.Any.withSubtype("Human")).opponentControls())
        )
        val whileOnBattlefield = Duration.WhileSourceOnBattlefield("this creature")
        effect = Effects.GainControl(stolen, whileOnBattlefield) then
            Effects.RemoveAllAbilities(stolen, whileOnBattlefield) then
            Effects.CantAttackOrBlock(stolen, whileOnBattlefield)
        description = "When this creature enters, choose target Human or artifact an opponent " +
            "controls. For as long as this creature remains on the battlefield, gain control of " +
            "that permanent, it loses all abilities, and it can't attack or block."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "133"
        artist = "Chris Rahn"
        imageUri = "https://cards.scryfall.io/normal/front/3/f/3fa16922-3583-4f5b-8805-509b95a8da49.jpg?1783932621"
    }
}
