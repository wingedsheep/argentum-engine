package com.wingedsheep.mtg.sets.definitions.otj.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersAsCopy
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility

/**
 * Visage Bandit — Outlaws of Thunder Junction #76
 * {3}{U} · Creature — Shapeshifter Rogue · Uncommon
 * 2/2
 *
 * You may have this creature enter as a copy of a creature you control, except it's a
 * Shapeshifter Rogue in addition to its other types.
 * Plot {2}{U}
 *
 * The enter-as-copy clause is the standard [EntersAsCopy] clone replacement, restricted to a
 * creature *you control* and adding both Shapeshifter and Rogue to the copy's subtypes. Plot
 * uses the existing `KeywordAbility.plot` special action.
 */
val VisageBandit = card("Visage Bandit") {
    manaCost = "{3}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Shapeshifter Rogue"
    power = 2
    toughness = 2
    oracleText = "You may have this creature enter as a copy of a creature you control, except " +
        "it's a Shapeshifter Rogue in addition to its other types.\n" +
        "Plot {2}{U} (You may pay {2}{U} and exile this card from your hand. Cast it as a " +
        "sorcery on a later turn without paying its mana cost. Plot only as a sorcery.)"

    replacementEffect(
        EntersAsCopy(
            optional = true,
            copyFilter = GameObjectFilter.Creature.youControl(),
            additionalSubtypes = listOf("Shapeshifter", "Rogue")
        )
    )

    keywordAbility(KeywordAbility.plot("{2}{U}"))

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "76"
        artist = "Miranda Meeks"
        imageUri = "https://cards.scryfall.io/normal/front/6/8/685ec4c6-3332-498f-8b56-d7ad8fc5230c.jpg?1712355536"
    }
}
