package com.wingedsheep.mtg.sets.definitions.c15.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantKeyword

/**
 * Blade of Selves — Commander 2015 #51
 * {2} · Artifact — Equipment
 *
 * Equipped creature has myriad. (Whenever it attacks, for each opponent other than defending
 * player, you may create a token copy that's tapped and attacking that player or a planeswalker
 * they control. Exile the tokens at end of combat.)
 * Equip {4}
 *
 * A static grant of the engine-live bare keyword: the engine supplies the myriad trigger to the
 * equipped creature, one instance (see [com.wingedsheep.sdk.scripting.Myriad]). The copies don't
 * carry the Equipment, so they don't have myriad themselves.
 */
val BladeOfSelves = card("Blade of Selves") {
    manaCost = "{2}"
    typeLine = "Artifact — Equipment"
    oracleText = "Equipped creature has myriad. (Whenever it attacks, for each opponent other than " +
        "defending player, you may create a token copy that's tapped and attacking that player or a " +
        "planeswalker they control. Exile the tokens at end of combat.)\n" +
        "Equip {4}"

    staticAbility {
        ability = GrantKeyword(Keyword.MYRIAD, Filters.EquippedCreature)
    }

    equipAbility("{4}")

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "51"
        artist = "Alan Pollack"
        imageUri = "https://cards.scryfall.io/normal/front/6/e/6e8735b5-64ef-4782-8d8d-4477b409e52d.jpg?1783938105"
        ruling("2015-11-04", "If the defending player is your only opponent, no tokens are put onto the battlefield.")
        ruling("2015-11-04", "You choose whether each token is attacking the player or a planeswalker they control as the token is created.")
        ruling("2015-11-04", "If the tokens aren't creatures (perhaps because the equipped creature was an animated land), they'll enter the battlefield but they won't be attacking. You'll still exile those tokens at end of combat.")
        ruling("2015-11-04", "Each token copies exactly what was printed on the original creature and nothing else. It doesn't copy whether that creature is tapped or untapped, whether it has any counters on it or Auras and Equipment attached to it, or any non-copy effects that have changed its power, toughness, types, color, and so on.")
    }
}
