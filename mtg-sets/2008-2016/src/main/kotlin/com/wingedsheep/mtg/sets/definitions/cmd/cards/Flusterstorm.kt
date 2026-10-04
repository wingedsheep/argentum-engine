package com.wingedsheep.mtg.sets.definitions.cmd.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Flusterstorm
 * {U}
 * Instant
 * Counter target instant or sorcery spell unless its controller pays {1}.
 * Storm (When you cast this spell, copy it for each spell cast before it this turn. You may
 * choose new targets for the copies.)
 *
 * Disrupt's tax counter over [TargetFilter.InstantOrSorcerySpellOnStack], plus storm. Storm
 * (CR 702.40) copies the spell off `script.spellEffect`, so this stays a plain `spell { }`.
 * Each copy is its own "unless pays {1}" demand, so the controller pays per copy.
 */
val Flusterstorm = card("Flusterstorm") {
    manaCost = "{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Counter target instant or sorcery spell unless its controller pays {1}.\n" +
        "Storm (When you cast this spell, copy it for each spell cast before it this turn. You may choose new targets for the copies.)"

    spell {
        val instantOrSorcerySpell = target(TargetFilter.InstantOrSorcerySpellOnStack)
        effect = Effects.CounterUnlessPays("{1}")
    }

    keywords(Keyword.STORM)

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "46"
        artist = "Erica Yang"
        imageUri = "https://cards.scryfall.io/normal/front/1/e/1e2e09bf-e7c8-4f13-bcee-f9c8cbc57993.jpg?1783941241"

        ruling("2022-12-08", "Spells cast from zones other than a player's hand and spells that were countered are counted by the storm ability.")
        ruling("2022-12-08", "The copies are put directly onto the stack. They aren't cast and won't be counted by other spells with storm cast later in the turn.")
        ruling("2022-12-08", "A copy of a spell can be countered like any other spell, but it must be countered individually. Countering a spell with storm won't affect the copies.")
    }
}
