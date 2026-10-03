package com.wingedsheep.mtg.sets.definitions.m20.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.mode
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.ModalEffect
import com.wingedsheep.sdk.scripting.effects.Mode
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Dread Presence
 * {3}{B}
 * Creature — Nightmare
 * 3/3
 * Whenever a Swamp you control enters, choose one —
 * • You draw a card and you lose 1 life.
 * • This creature deals 2 damage to any target and you gain 2 life.
 *
 * A land-entry trigger (`Triggers.a(Filters.SwampCard.youControl()).enters()`, Battlewand Oak's
 * shape — the Swamp *subtype*, so nonbasic Swamps count) carrying a choose-one modal; only the
 * second mode targets, so it is the only one that needs a legal target to be chosen.
 */
val DreadPresence = card("Dread Presence") {
    manaCost = "{3}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Nightmare"
    power = 3
    toughness = 3
    oracleText = "Whenever a Swamp you control enters, choose one —\n" +
        "• You draw a card and you lose 1 life.\n" +
        "• This creature deals 2 damage to any target and you gain 2 life."

    triggeredAbility {
        trigger = Triggers.a(Filters.SwampCard.youControl()).enters()
        effect = ModalEffect.chooseOne(
            Mode.noTarget(
                Effects.DrawCards(1) then Effects.LoseLife(1, EffectTarget.Controller),
                "You draw a card and you lose 1 life"
            ),
            mode("This creature deals 2 damage to any target and you gain 2 life") {
                val anyTarget = target(Targets.Any)
                effect = Effects.DealDamage(2, anyTarget) then Effects.GainLife(2)
            },
        )
        description = "Whenever a Swamp you control enters, choose one — You draw a card and you " +
            "lose 1 life; or this creature deals 2 damage to any target and you gain 2 life."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "96"
        artist = "Anthony Palumbo"
        flavorText = "It beckons silently, waiting in the darkness."
        imageUri = "https://cards.scryfall.io/normal/front/0/4/0430db1a-5cad-4444-ba93-57fb32e65606.jpg?1783932995"
    }
}
