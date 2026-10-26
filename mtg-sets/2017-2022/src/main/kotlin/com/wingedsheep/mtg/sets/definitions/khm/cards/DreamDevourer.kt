package com.wingedsheep.mtg.sets.definitions.khm.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantForetellToCardsInHand
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Dream Devourer (Kaldheim #90)
 * {1}{B} · Creature — Demon Cleric 0/3
 * Each nonland card in your hand without foretell has foretell. Its foretell cost is equal to its
 * mana cost reduced by {2}.
 * Whenever you foretell a card, this creature gets +2/+0 until end of turn.
 *
 * [GrantForetellToCardsInHand] grants foretell to the nonland cards in hand; the cost is fixed as the card
 * is foretold, so it can still be cast later after Dream Devourer is gone. The trigger is the
 * foretell special action itself (`Triggers.you.foretells()`), not casting a foretold card.
 */
val DreamDevourer = card("Dream Devourer") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Demon Cleric"
    power = 0
    toughness = 3
    oracleText = "Each nonland card in your hand without foretell has foretell. Its foretell cost is equal to its mana cost reduced by {2}. " +
        "(During your turn, you may pay {2} and exile it from your hand face down. Cast it on a later turn for its foretell cost.)\n" +
        "Whenever you foretell a card, this creature gets +2/+0 until end of turn."

    staticAbility {
        ability = GrantForetellToCardsInHand(GameObjectFilter.Nonland, costReduction = 2)
    }

    triggeredAbility {
        trigger = Triggers.you.foretells()
        effect = Effects.ModifyStats(2, 0, EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "90"
        artist = "David Rapoza"
        imageUri = "https://cards.scryfall.io/normal/front/f/f/ff1d88a6-a74d-44b5-b7a8-866e866807f1.jpg?1783928250"
    }
}
