package com.wingedsheep.mtg.sets.definitions.woe.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.model.Rarity

/**
 * Spellscorn Coven // Take It Back
 * {3}{B}
 * Creature — Faerie Warlock
 * 2/3
 * Flying
 * When this creature enters, each opponent discards a card.
 *
 * Adventure: Take It Back — {2}{U}, Instant — Adventure
 * Return target spell to its owner's hand.
 *
 * The Adventure is a soft counter in the
 * [Reprieve][com.wingedsheep.mtg.sets.definitions.ltr.cards.Reprieve] mould:
 * `Effects.ReturnSpellToOwnersHand` moves the spell off the stack without *countering* it, so a
 * can't-be-countered spell is still caught and no "whenever a spell is countered" trigger fires
 * (CR 701.29 is a different action from the stack-to-hand move here). The card goes to its **owner's**
 * hand, which matters when you've stolen a spell's control.
 *
 * `Targets.Spell` is unrestricted — Take It Back can bounce your own spell, which is a real line
 * (rescuing something about to be countered).
 *
 * The two halves are the reason the card's colour identity is `UB` while the creature face is mono-black:
 * the Adventure's `{2}{U}` counts toward identity (CR 903.4).
 */
val SpellscornCoven = card("Spellscorn Coven") {
    manaCost = "{3}{B}"
    colorIdentity = "UB"
    typeLine = "Creature — Faerie Warlock"
    oracleText = "Flying\nWhen this creature enters, each opponent discards a card."
    power = 2
    toughness = 3

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = Effects.EachOpponentDiscards(1)
        description = "When this creature enters, each opponent discards a card."
    }

    adventure("Take It Back") {
        manaCost = "{2}{U}"
        typeLine = "Instant — Adventure"
        oracleText = "Return target spell to its owner's hand. " +
            "(Then exile this card. You may cast the creature later from exile.)"
        spell {
            target("spell", Targets.Spell)
            effect = Effects.ReturnSpellToOwnersHand()
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "237"
        artist = "Uriah Voth"
        imageUri = "https://cards.scryfall.io/normal/front/8/c/8c112f62-6034-4636-a75b-4a45bc916a91.jpg?1783915062"
    }
}
