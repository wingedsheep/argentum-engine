package com.wingedsheep.mtg.sets.definitions.mom.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CardOrder
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Nahiri's Warcrafting
 * {1}{R}{R}
 * Sorcery
 *
 * Nahiri's Warcrafting deals 5 damage to target creature, planeswalker, or battle. Look at the top X
 * cards of your library, where X is the excess damage dealt this way. You may exile one of those
 * cards. Put the rest on the bottom of your library in a random order. You may play the exiled card
 * this turn.
 */
val NahirisWarcrafting = card("Nahiri's Warcrafting") {
    manaCost = "{1}{R}{R}"
    colorIdentity = "R"
    typeLine = "Sorcery"
    oracleText = "Nahiri's Warcrafting deals 5 damage to target creature, planeswalker, or battle. " +
        "Look at the top X cards of your library, where X is the excess damage dealt this way. You may " +
        "exile one of those cards. Put the rest on the bottom of your library in a random order. You " +
        "may play the exiled card this turn."

    spell {
        val t = target(TargetFilter.CreaturePlaneswalkerOrBattle)
        effect = Effects.Pipeline {
            val excess = runStoringNumber { Effects.DealDamage(5, t, excessDamageVariable = it) }
            val top = gather(CardSource.TopOfLibrary(excess.amount))
            val (picked, rest) = chooseUpToSplit(
                1,
                from = top,
                prompt = "You may exile one of those cards",
                selectedLabel = "Exile",
                remainderLabel = "Put on bottom of library"
            )
            exile(picked)
            toLibraryBottom(rest, order = CardOrder.Random)
            run(Effects.GrantMayPlayFromExile(from = picked))
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "155"
        artist = "Zara Alfonso"
        flavorText = "\"Zendikar must be broken before it can be saved.\""
        imageUri = "https://cards.scryfall.io/normal/front/a/0/a0453fea-3c88-4eb1-818a-9efa01986852.jpg?1783916985"
    }
}
