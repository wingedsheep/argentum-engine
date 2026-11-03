package com.wingedsheep.mtg.sets.definitions.xln.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Spell Swindle
 * {3}{U}{U}
 * Instant
 *
 * Counter target spell. Create X Treasure tokens, where X is that spell's mana value.
 *
 * The same shape as Electrosiphon: "that spell's mana value" is read while the spell is still on the
 * stack, where an {X} counts as the value chosen for it (CR 202.3e), so it is stored before the counter
 * and paid out after. Once countered the card is back in a graveyard with X at 0. The Treasures are
 * not conditional on the counter succeeding: a spell that can't be countered stays on the stack and
 * Spell Swindle still makes the Treasures (ruling 2017-09-29). Only an illegal target on resolution
 * stops the whole spell.
 */
val SpellSwindle = card("Spell Swindle") {
    manaCost = "{3}{U}{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Counter target spell. Create X Treasure tokens, where X is that spell's mana value. " +
        "(They're artifacts with \"{T}, Sacrifice this token: Add one mana of any color.\")"

    spell {
        val spell = target(TargetFilter.SpellOnStack)
        effect = Effects.StoreNumber(SPELL_MANA_VALUE, DynamicAmounts.manaValueOf(spell)) then
            Effects.CounterSpell() then
            Effects.CreateTreasure(DynamicAmounts.storedNumber(SPELL_MANA_VALUE))
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "82"
        artist = "Victor Adame Minguez"
        imageUri = "https://cards.scryfall.io/normal/front/6/e/6e619ada-e9ce-4758-afd8-8def853877eb.jpg?1783935770"
        ruling("2017-09-29", "For spells with {X} in their mana costs, use the value chosen for X to determine the spell's mana value.")
        ruling("2017-09-29", "You may target a spell that can't be countered. When Spell Swindle resolves, the target spell will be unaffected, but you'll still get Treasures.")
    }
}

private const val SPELL_MANA_VALUE = "spellSwindleManaValue"
