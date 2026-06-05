package com.wingedsheep.mtg.sets.definitions.otj.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersWithCounters
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.events.CounterTypeFilter

/**
 * Freestrider Commando
 * {2}{G}
 * Creature — Centaur Mercenary
 * 3/3
 * This creature enters with two +1/+1 counters on it if it wasn't cast or no mana was spent to cast it.
 * Plot {3}{G}
 *
 * The free-cast payoff: a plotted Freestrider Commando (cast from exile without paying its mana
 * cost) enters as a 5/5. Cast normally for {2}{G}, mana was spent, so it enters as a vanilla 3/3.
 */
val FreestriderCommando = card("Freestrider Commando") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Centaur Mercenary"
    power = 3
    toughness = 3
    oracleText = "This creature enters with two +1/+1 counters on it if it wasn't cast or no mana was spent to cast it.\n" +
        "Plot {3}{G} (You may pay {3}{G} and exile this card from your hand. Cast it as a sorcery on a later turn without paying its mana cost. Plot only as a sorcery.)"

    keywordAbility(KeywordAbility.plot("{3}{G}"))

    replacementEffect(EntersWithCounters(
        counterType = CounterTypeFilter.PlusOnePlusOne,
        count = 2,
        selfOnly = true,
        condition = Conditions.NoManaSpentToCast
    ))

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "162"
        artist = "Adrián Rodríguez Pérez"
        imageUri = "https://cards.scryfall.io/normal/front/9/2/92762169-095e-46e2-82f6-5b2ff2232240.jpg?1712355916"

        ruling("2024-04-12", "If Freestrider Commando is cast without paying its mana cost but you paid mana for additional costs or cost increases (such as from Aven Interrupter), Freestrider Commando won't enter with +1/+1 counters.")
        ruling("2024-04-12", "Plot abilities are written \"Plot [cost],\" which means \"Any time you have priority during your main phase while the stack is empty, you may pay [cost] and exile this card from your hand. It becomes plotted.\"")
        ruling("2024-04-12", "You can't cast a plotted card on the same turn it became plotted. On any future turn, you may cast that card from exile without paying its mana cost during your main phase while the stack is empty.")
        ruling("2024-04-12", "If you're casting a plotted card from exile without paying its mana cost, you can't choose to cast it for any other alternative costs. You can, however, pay additional costs, such as kicker costs. If the plotted card has any mandatory additional costs, those must still be paid to cast the spell.")
        ruling("2024-04-12", "If a plotted card has {X} in its mana cost, you must choose 0 as the value of X when casting it without paying its mana cost.")
    }
}
