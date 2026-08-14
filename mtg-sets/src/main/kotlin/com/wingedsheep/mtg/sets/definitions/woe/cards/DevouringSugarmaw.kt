package com.wingedsheep.mtg.sets.definitions.woe.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.SacrificeEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Devouring Sugarmaw // Have for Dinner
 * {2}{B}{B}
 * Creature — Horror
 * 6/6
 * Menace, trample
 * At the beginning of your upkeep, you may sacrifice an artifact, enchantment, or token. If you
 * don't, tap this creature.
 *
 * Adventure: Have for Dinner — {1}{W}, Instant — Adventure
 * Create a 1/1 white Human creature token and a Food token.
 *
 * The upkeep clause is the literal "you may [action]. If you don't, [consequence]" shape — an
 * `optional = true` trigger whose body is the sacrifice and whose `elseEffect` taps the Horror
 * (compare Yawgmoth Demon). The sacrifice filter is [GameObjectFilter.ArtifactEnchantmentOrToken],
 * the same permanent set bargain feeds on; with none of those on the battlefield the may-action is
 * infeasible, so the engine skips the prompt and taps the creature directly. Note it *taps* rather
 * than doesn't-untap: a Sugarmaw already tapped stays tapped, and one tapped this way untaps
 * normally next turn.
 *
 * The token half is fed by the shared WOE token helpers — [woeHumanToken] plus [Effects.CreateFood].
 *
 * (CR 715: Adventure cards. Casting the Adventure exiles the card on resolution and lets the
 * caster cast it as the creature spell while it remains in exile.)
 */
val DevouringSugarmaw = card("Devouring Sugarmaw") {
    manaCost = "{2}{B}{B}"
    colorIdentity = "BW"
    typeLine = "Creature — Horror"
    oracleText = "Menace, trample\n" +
        "At the beginning of your upkeep, you may sacrifice an artifact, enchantment, or token. " +
        "If you don't, tap this creature."
    power = 6
    toughness = 6

    keywords(Keyword.MENACE, Keyword.TRAMPLE)

    triggeredAbility {
        trigger = Triggers.YourUpkeep
        optional = true
        effect = SacrificeEffect(GameObjectFilter.ArtifactEnchantmentOrToken)
        elseEffect = Effects.Tap(EffectTarget.Self)
    }

    adventure("Have for Dinner") {
        manaCost = "{1}{W}"
        typeLine = "Instant — Adventure"
        oracleText = "Create a 1/1 white Human creature token and a Food token. " +
            "(Then exile this card. You may cast the creature later from exile.)"
        spell {
            effect = Effects.Composite(
                woeHumanToken(),
                Effects.CreateFood()
            )
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "224"
        artist = "Nino Vecia"
        imageUri = "https://cards.scryfall.io/normal/front/5/8/58c7f52e-a97d-4475-ae00-3149991e723e.jpg?1783915066"
    }
}
