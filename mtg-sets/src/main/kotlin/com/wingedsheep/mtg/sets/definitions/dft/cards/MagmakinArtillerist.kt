package com.wingedsheep.mtg.sets.definitions.dft.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.ContextPropertyKey
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Magmakin Artillerist — Aetherdrift #137
 * {2}{R} · Creature — Elemental Pirate · 1/4
 *
 * Whenever you discard one or more cards, this creature deals that much damage to each opponent.
 * Cycling {1}{R}
 * When you cycle this card, it deals 1 damage to each opponent.
 *
 * The discard payoff is batch-worded (CR 603.2c), so it uses [Triggers.YouDiscardOneOrMore] —
 * one trigger per discard *event*, however many cards it contained — and reads the batch size
 * back through [ContextPropertyKey.TRIGGER_DISCARD_COUNT] ("that much"). Discarding three cards
 * to one effect deals 3; three sequential single discards fire three triggers for 1 each.
 *
 * The two abilities never double up on this card's own cycling: cycling it discards it from
 * hand, and the battlefield-only discard trigger doesn't function from hand — only the cycle
 * trigger fires. Cycling anything *else* while this is on the battlefield does fire the discard
 * trigger for 1.
 */
val MagmakinArtillerist = card("Magmakin Artillerist") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Elemental Pirate"
    power = 1
    toughness = 4
    oracleText = "Whenever you discard one or more cards, this creature deals that much damage " +
        "to each opponent.\n" +
        "Cycling {1}{R} ({1}{R}, Discard this card: Draw a card.)\n" +
        "When you cycle this card, it deals 1 damage to each opponent."

    triggeredAbility {
        trigger = Triggers.YouDiscardOneOrMore
        effect = Effects.DealDamage(
            amount = DynamicAmount.ContextProperty(ContextPropertyKey.TRIGGER_DISCARD_COUNT),
            target = EffectTarget.PlayerRef(Player.EachOpponent),
            damageSource = EffectTarget.Self,
        )
    }

    keywordAbility(KeywordAbility.cycling("{1}{R}"))

    triggeredAbility {
        trigger = Triggers.YouCycleThis
        effect = Effects.DealDamage(
            amount = 1,
            target = EffectTarget.PlayerRef(Player.EachOpponent),
            damageSource = EffectTarget.Self,
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "137"
        artist = "Madeline Boni"
        imageUri = "https://cards.scryfall.io/normal/front/2/a/2ac26341-a100-424d-be84-33fbbf1b4078.jpg?1783907878"
    }
}
