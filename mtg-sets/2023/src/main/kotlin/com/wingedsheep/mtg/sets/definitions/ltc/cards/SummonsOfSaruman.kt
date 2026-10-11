package com.wingedsheep.mtg.sets.definitions.ltc.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.dsl.DynamicAmounts

/**
 * Summons of Saruman — Tales of Middle-earth Commander #70
 * {X}{U}{R} · Sorcery · Rare
 *
 * Amass Orcs X. Mill X cards. You may cast an instant or sorcery spell with mana value X or less
 * from among them without paying its mana cost.
 * Flashback—{3}{U}{R}, Exile X cards from your graveyard.
 *
 * The flashback cost carries no `{X}`: the exile cost defines X (CR 107.3a), so the number of
 * cards exiled is the X the spell resolves with. The spell itself is on the stack while its costs
 * are paid (CR 601.2a), so it can't be one of the exiled cards.
 */
val SummonsOfSaruman = card("Summons of Saruman") {
    manaCost = "{X}{U}{R}"
    colorIdentity = "UR"
    typeLine = "Sorcery"
    oracleText = "Amass Orcs X. Mill X cards. You may cast an instant or sorcery spell with mana value X " +
        "or less from among them without paying its mana cost. (To amass Orcs X, put X +1/+1 counters " +
        "on an Army you control. It's also an Orc. If you don't control an Army, create a 0/0 black " +
        "Orc Army creature token first.)\n" +
        "Flashback—{3}{U}{R}, Exile X cards from your graveyard."

    spell {
        effect = Effects.Pipeline {
            run(Effects.Amass(DynamicAmounts.xValue(), "Orc"))
            val milled = mill(DynamicAmounts.xValue())
            val castable = filter(milled, GameObjectFilter.InstantOrSorcery.manaValueAtMostX())
            val chosen = chooseUpTo(
                1,
                from = castable,
                selectedLabel = "Cast without paying its mana cost",
            )
            run(Effects.CastFromCollectionWithoutPayingCost(chosen))
        }
    }

    keywordAbility(KeywordAbility.flashback("{3}{U}{R}", Costs.additional.ExileVariableCards(minCount = 0)))

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "70"
        artist = "Svetlin Velinov"
        imageUri = "https://cards.scryfall.io/normal/front/c/b/cbb1bc1f-b1d6-4639-b892-9b1ff6ed65fb.jpg?1783916015"
        ruling(
            "2023-06-23",
            "If you want to cast one of the milled cards, you cast it as part of the resolution of " +
                "Summons of Saruman. You can't wait to cast the spell later in the turn. Timing " +
                "permissions based on the card's type are ignored."
        )
        ruling(
            "2023-06-23",
            "If the spell has {X} in its mana cost, you must choose 0 as the value of X."
        )
    }
}
