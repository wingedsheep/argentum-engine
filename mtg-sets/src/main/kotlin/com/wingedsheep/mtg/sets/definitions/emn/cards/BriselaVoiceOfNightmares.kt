package com.wingedsheep.mtg.sets.definitions.emn.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.PlayersCantCastSpells
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Brisela, Voice of Nightmares
 * Legendary Creature — Eldrazi Angel
 * 9/10
 * Flying, first strike, vigilance, lifelink
 * Your opponents can't cast spells with mana value 3 or less.
 *
 * This is a meld result (Bruna, the Fading Light + Gisela, the Broken Blade). Meld itself is a
 * blocked mechanic, so per the task scope Brisela is authored as a normal colorless legendary
 * creature with its printed abilities; the meld linkage is ignored. `meldResult = true` keeps it
 * out of booster, draft and deckbuilding pools — it's only ever created by melding the pair.
 *
 * The cast restriction is the reused [PlayersCantCastSpells] primitive scoped to each opponent
 * (Void Winnower / Grand Abolisher family), filtered to spells with mana value 3 or less.
 */
val BriselaVoiceOfNightmares = card("Brisela, Voice of Nightmares") {
    manaCost = ""
    meldResult = true
    colorIdentity = ""
    typeLine = "Legendary Creature — Eldrazi Angel"
    power = 9
    toughness = 10
    oracleText = "Flying, first strike, vigilance, lifelink\n" +
        "Your opponents can't cast spells with mana value 3 or less."

    keywords(Keyword.FLYING, Keyword.FIRST_STRIKE, Keyword.VIGILANCE, Keyword.LIFELINK)

    staticAbility {
        ability = PlayersCantCastSpells(
            affected = Player.EachOpponent,
            spellFilter = Filters.ManaValueAtMost(3)
        )
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "15b"
        artist = "Clint Cearley"
        imageUri = "https://cards.scryfall.io/normal/front/5/a/5a7a212e-e0b6-4f12-a95c-173cae023f93.jpg?1782711940"
    }
}
