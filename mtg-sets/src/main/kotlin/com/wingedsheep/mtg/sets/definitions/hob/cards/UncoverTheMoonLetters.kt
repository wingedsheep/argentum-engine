package com.wingedsheep.mtg.sets.definitions.hob.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.MayEffect
import com.wingedsheep.sdk.scripting.values.ContextPropertyKey
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Uncover the Moon-Letters — The Hobbit #57
 * {3}{U} · Enchantment · Rare
 *
 * Whenever you cast a noncreature spell, you may draw X cards, where X is the amount of mana
 * spent to cast that spell. If you do, discard two cards.
 *
 * Modeling notes:
 *  - X is [ContextPropertyKey.MANA_SPENT_ON_TRIGGERING_SPELL] — the mana actually spent on the
 *    *triggering* spell, not its mana value. Cost reductions lower it; an X spell or a kicked
 *    spell raises it, and mana spent from Treasures or rituals counts all the same.
 *  - "You may … If you do" is one [MayEffect] over both halves: accepting draws and then discards,
 *    declining does neither. The discard is not a separate optional step — it rides on the same
 *    decision, so a player can't take the cards and skip the cost.
 *  - The enchantment is itself a noncreature spell, but it isn't on the battlefield while it's on
 *    the stack, so casting it doesn't trigger its own ability.
 */
val UncoverTheMoonLetters = card("Uncover the Moon-Letters") {
    manaCost = "{3}{U}"
    colorIdentity = "U"
    typeLine = "Enchantment"
    oracleText = "Whenever you cast a noncreature spell, you may draw X cards, where X is the " +
        "amount of mana spent to cast that spell. If you do, discard two cards."

    triggeredAbility {
        trigger = Triggers.YouCastNoncreature
        effect = MayEffect(
            Effects.Composite(
                Effects.DrawCards(
                    DynamicAmount.ContextProperty(ContextPropertyKey.MANA_SPENT_ON_TRIGGERING_SPELL)
                ),
                Patterns.Hand.discardCards(2)
            ),
            descriptionOverride = "Draw X cards, where X is the amount of mana spent to cast that " +
                "spell, then discard two cards?"
        )
        description = "Whenever you cast a noncreature spell, you may draw X cards, where X is " +
            "the amount of mana spent to cast that spell. If you do, discard two cards."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "57"
        artist = "Leon Tukker"
        flavorText = "Elrond took the map and gazed long at it, and he shook his head, until he " +
            "saw what the moon revealed."
        imageUri = "https://cards.scryfall.io/normal/front/7/9/79edf5f6-f6b6-4271-bd2a-14a980f30616.jpg?1785496445"
    }
}
