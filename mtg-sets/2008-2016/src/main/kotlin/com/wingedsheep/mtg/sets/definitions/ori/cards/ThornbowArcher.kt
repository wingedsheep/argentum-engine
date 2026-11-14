package com.wingedsheep.mtg.sets.definitions.ori.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Thornbow Archer
 * {B}
 * Creature — Elf Archer
 * 1/2
 * Whenever this creature attacks, each opponent who doesn't control an Elf loses 1 life.
 *
 * The condition is checked per opponent at resolution: [Effects.ForEachPlayer] rebinds the
 * effect's controller to each opponent in turn, so `YouControl(…, negate = true)` asks whether
 * *that opponent* controls no Elf (the Archer, an Elf its own controller controls, never counts),
 * and the loss lands on that same opponent via [EffectTarget.Controller] — Nicol Bolas,
 * Dragon-God's −8 shape. "An Elf" is any permanent with the Elf subtype, not only creatures.
 */
val ThornbowArcher = card("Thornbow Archer") {
    manaCost = "{B}"
    colorIdentity = "B"
    typeLine = "Creature — Elf Archer"
    power = 1
    toughness = 2
    oracleText = "Whenever this creature attacks, each opponent who doesn't control an Elf loses 1 life."

    triggeredAbility {
        trigger = Triggers.self.attacks()
        effect = Effects.ForEachPlayer(
            Player.EachOpponent,
            Effects.If(
                condition = Conditions.YouControl(GameObjectFilter.Permanent.withSubtype("Elf"), negate = true),
                then = Effects.LoseLife(1, EffectTarget.Controller)
            )
        )
        description = "Whenever this creature attacks, each opponent who doesn't control an Elf loses 1 life."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "121"
        artist = "Kev Walker"
        flavorText = "\"I will not rest until the stench of eyeblights is eradicated from these woods.\""
        imageUri = "https://cards.scryfall.io/normal/front/3/2/3290c60a-f9a0-464d-a15e-8473a6d50d96.jpg?1783938339"
    }
}
