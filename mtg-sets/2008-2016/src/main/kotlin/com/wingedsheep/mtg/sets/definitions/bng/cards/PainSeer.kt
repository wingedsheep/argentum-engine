package com.wingedsheep.mtg.sets.definitions.bng.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Pain Seer
 * {1}{B}
 * Creature — Human Wizard
 * 2/2
 *
 * Inspired — Whenever this creature becomes untapped, reveal the top card of your library and put
 * that card into your hand. You lose life equal to that card's mana value.
 *
 * Canonical printing: Born of the Gods, the card's earliest real printing.
 */
val PainSeer = card("Pain Seer") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Human Wizard"
    power = 2
    toughness = 2
    oracleText = "Inspired — Whenever this creature becomes untapped, reveal the top card of your library " +
        "and put that card into your hand. You lose life equal to that card's mana value."

    triggeredAbility {
        trigger = Triggers.self.becomesUntapped()
        effect = Effects.Pipeline {
            val revealed = gather(CardSource.TopOfLibrary(1, Player.You))
            toHand(revealed, revealed = true)
            run(Effects.LoseLife(
                DynamicAmounts.manaValueOf(revealed),
                EffectTarget.Controller
            ))
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "80"
        artist = "Tyler Jacobson"
        flavorText = "Every twitching nerve and pulsing vein carries a message, discernible with the right tools."
        imageUri = "https://cards.scryfall.io/normal/front/8/c/8ce8891f-b44c-4be4-878e-9fc45a9dc9cb.jpg?1783939553"
    }
}
