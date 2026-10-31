package com.wingedsheep.mtg.sets.definitions.dsk.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardLayout
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.NoMaximumHandSize
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Roaring Furnace // Steaming Sauna (DSK 230) — split-layout Room (CR 709.5).
 *
 * Roaring Furnace {1}{R} — Enchantment — Room
 *   When you unlock this door, this Room deals damage equal to the number of cards in your hand
 *   to target creature an opponent controls.
 *
 * Steaming Sauna {3}{U}{U} — Enchantment — Room
 *   You have no maximum hand size.
 *   At the beginning of your end step, draw a card.
 *
 * Cast each half separately; the cast face enters unlocked, the other locked. Pay the locked
 * face's printed mana cost as a sorcery-speed special action to unlock it (CR 709.5e). The
 * Roaring Furnace door-unlock trigger (`Triggers.self.doorUnlocked()`) targets a creature an opponent
 * controls and deals damage equal to [DynamicAmounts.cardsInYourHand]; the Room itself is the
 * damage source (the default when no explicit `damageSource` is given to [Effects.DealDamage]).
 * Steaming Sauna pairs the [NoMaximumHandSize] static with an end-step draw.
 */
val RoaringFurnaceSteamingSauna = card("Roaring Furnace // Steaming Sauna") {
    layout = CardLayout.SPLIT
    colorIdentity = "UR"

    face("Roaring Furnace") {
        manaCost = "{1}{R}"
        typeLine = "Enchantment — Room"
        oracleText = "When you unlock this door, this Room deals damage equal to the number of " +
            "cards in your hand to target creature an opponent controls."

        triggeredAbility {
            val creatureOpponentControls = target(TargetFilter.CreatureOpponentControls)
            trigger = Triggers.self.doorUnlocked()
            effect = Effects.DealDamage(DynamicAmounts.cardsInYourHand(), creatureOpponentControls)
            description = "When you unlock this door, this Room deals damage equal to the number " +
                "of cards in your hand to target creature an opponent controls."
        }
    }

    face("Steaming Sauna") {
        manaCost = "{3}{U}{U}"
        typeLine = "Enchantment — Room"
        oracleText = "You have no maximum hand size.\nAt the beginning of your end step, draw a card."

        staticAbility {
            ability = NoMaximumHandSize()
        }

        triggeredAbility {
            trigger = Triggers.you.beginningOf(Step.END)
            effect = Effects.DrawCards(1)
            description = "At the beginning of your end step, draw a card."
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "230"
        artist = "Miklós Ligeti"
        imageUri = "https://cards.scryfall.io/normal/front/9/4/94352ffb-d716-484d-b018-4e1c033ef2f3.jpg?1726867698"
    }
}
