package com.wingedsheep.mtg.sets.definitions.frf.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Friendly Fire — Fate Reforged #101
 * {3}{R} · Instant
 *
 * Target creature's controller reveals a card at random from their hand. Friendly Fire deals
 * damage to that creature and that player equal to the revealed card's mana value.
 *
 * Canonical printing: Fate Reforged, the card's only printing.
 *
 * The Wand of Ith reveal shape: gather the creature controller's hand, pick one card at random,
 * reveal it. Both damage events read that one-card pile's mana value (lands and {X} count as 0, so
 * an empty hand deals no damage). No state-based actions run mid-resolution, so the creature is
 * still on the battlefield when its controller is looked up for the second damage event.
 */
val FriendlyFire = card("Friendly Fire") {
    manaCost = "{3}{R}"
    colorIdentity = "R"
    typeLine = "Instant"
    oracleText = "Target creature's controller reveals a card at random from their hand. Friendly " +
        "Fire deals damage to that creature and that player equal to the revealed card's mana value."

    spell {
        val creature = target(TargetFilter.Creature)
        val thatPlayer = Player.ControllerOf("target creature")
        effect = Effects.Pipeline {
            val hand = gather(CardSource.FromZone(Zone.HAND, player = thatPlayer))
            val revealed = chooseRandom(1, from = hand)
            reveal(revealed)
            val damage = DynamicAmounts.manaValueSumOf(revealed)
            run(Effects.DealDamage(damage, creature))
            run(Effects.DealDamage(damage, EffectTarget.TargetController))
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "101"
        artist = "Anthony Palumbo"
        flavorText = "\"Never tell goblins to 'fire at will.'\"\n—Urut Barzeel, Mardu hordechief"
        imageUri = "https://cards.scryfall.io/normal/front/a/c/ac4272ca-bb15-415c-a589-a472953a0dd9.jpg?1783938688"
        ruling("2014-11-24", "Cards that don't have mana costs, such as land cards, have a mana value of 0.")
        ruling("2014-11-24", "If the card has {X} in its mana cost, X is 0.")
    }
}
