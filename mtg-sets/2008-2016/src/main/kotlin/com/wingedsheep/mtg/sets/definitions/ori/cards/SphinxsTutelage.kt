package com.wingedsheep.mtg.sets.definitions.ori.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.CollectionSlot
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.effects.RepeatCondition

/**
 * Sphinx's Tutelage
 * {2}{U}
 * Enchantment
 *
 * Whenever you draw a card, target opponent mills two cards. If two nonland cards that share a
 * color were milled this way, repeat this process.
 * {5}{U}: Draw a card, then discard a card.
 *
 * The trigger is a do-while loop ([Effects.RepeatWhile]), the shape The Tale of Tamiyo uses for
 * "mill two; if they share a card type, repeat". Each pass mills two into `milled`, then splits
 * that pass's milled cards into one collection per colour holding the *nonland* cards of that
 * colour. "Two nonland cards that share a color" is exactly "some colour's collection holds both
 * cards", so the loop repeats while any of the five per-colour collections reaches two. Colourless
 * cards land in none of them (colourless is not a colour — 2015-06-22 ruling), a land never counts
 * even when it has a colour, and when the library can't supply two cards no collection reaches two,
 * so an emptied library always ends the loop. The repeat condition reads only the pass just run, and
 * every pass mills the same target opponent.
 */
val SphinxsTutelage = card("Sphinx's Tutelage") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Enchantment"
    oracleText = "Whenever you draw a card, target opponent mills two cards. If two nonland cards that " +
        "share a color were milled this way, repeat this process.\n" +
        "{5}{U}: Draw a card, then discard a card."

    // One collection per colour: this pass's milled nonland cards of that colour.
    val nonlandOfColor = Color.entries.map { "tutelageNonland${it.displayName}" }

    triggeredAbility {
        trigger = Triggers.you.draws()
        val opponent = target(Targets.Opponent)
        effect = Effects.RepeatWhile(
            body = Effects.Pipeline {
                run(Patterns.Library.mill(2, opponent))
                Color.entries.forEachIndexed { i, color ->
                    filter(
                        Patterns.Library.milled,
                        GameObjectFilter.Nonland.withColor(color),
                        name = nonlandOfColor[i]
                    )
                }
            },
            repeatCondition = RepeatCondition.WhileCondition(
                Conditions.Any(
                    *nonlandOfColor.map { name ->
                        Conditions.CompareAmounts(
                            DynamicAmounts.distinctEntitiesIn(CollectionSlot(name)),
                            ComparisonOperator.GTE,
                            2
                        )
                    }.toTypedArray()
                )
            )
        )
        description = "Whenever you draw a card, target opponent mills two cards. If two nonland cards " +
            "that share a color were milled this way, repeat this process."
    }

    activatedAbility {
        cost = Costs.Mana("{5}{U}")
        effect = Patterns.Hand.loot()
        description = "{5}{U}: Draw a card, then discard a card."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "76"
        artist = "Slawomir Maniak"
        flavorText = "\"You've never considered the implications of dealing with another telepath, Jace.\""
        imageUri = "https://cards.scryfall.io/normal/front/4/d/4d90c3b0-f958-4a15-94a1-9bf1b0a9ac2a.jpg?1783938346"
        ruling("2015-06-22", "Colorless is not a color, so putting two colorless cards into a graveyard won't cause the process to repeat.")
        ruling("2015-06-22", "The process will keep repeating as long as both cards share a color and neither is a land card.")
    }
}
