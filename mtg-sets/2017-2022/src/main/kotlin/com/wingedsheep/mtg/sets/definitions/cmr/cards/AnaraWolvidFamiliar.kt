package com.wingedsheep.mtg.sets.definitions.cmr.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Anara, Wolvid Familiar — Commander Legends #214 (canonical printing)
 * {3}{G} · Legendary Creature — Wolf Beast · 4/4
 *
 * During your turn, commanders you control have indestructible.
 * Partner (You can have two commanders if both have partner.)
 *
 * A [Conditions.IsYourTurn]-gated [GrantKeyword] over commander permanents you *control* (the
 * designation is read off the object, CR 903.3) — so a commander you own but an opponent controls
 * isn't covered, one you control but don't own is, and Anara covers itself when it is your
 * commander (all three per its rulings). Partner is deck construction only.
 */
val AnaraWolvidFamiliar = card("Anara, Wolvid Familiar") {
    manaCost = "{3}{G}"
    colorIdentity = "G"
    typeLine = "Legendary Creature — Wolf Beast"
    power = 4
    toughness = 4
    oracleText = "During your turn, commanders you control have indestructible. (Effects that say " +
        "\"destroy\" don't destroy them. A creature with indestructible can't be destroyed by damage.)\n" +
        "Partner (You can have two commanders if both have partner.)"

    staticAbility {
        condition = Conditions.IsYourTurn
        ability = GrantKeyword(
            Keyword.INDESTRUCTIBLE,
            GroupFilter(GameObjectFilter.Permanent.commander().youControl())
        )
    }

    keywordAbility(KeywordAbility.partner())

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "214"
        artist = "Jesper Ejsing"
        imageUri = "https://cards.scryfall.io/normal/front/9/a/9a67cfaa-990c-4dcb-97a9-9c5d7316cabe.jpg?1783928801"
        ruling("2020-11-10", "If Anara is your commander, its first ability affects itself during your turn.")
        ruling(
            "2020-11-10",
            "Anara's first ability affects commanders you control even if you don't own them. It " +
                "doesn't affect your commander if another player controls it."
        )
        ruling(
            "2020-11-10",
            "Because damage remains marked on a creature until the damage is removed as the turn " +
                "ends, nonlethal damage dealt to a commander you control may become lethal if Anara " +
                "leaves the battlefield during that turn."
        )
    }
}
