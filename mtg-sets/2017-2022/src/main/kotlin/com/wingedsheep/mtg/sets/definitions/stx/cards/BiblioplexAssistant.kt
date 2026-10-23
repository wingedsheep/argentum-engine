package com.wingedsheep.mtg.sets.definitions.stx.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Biblioplex Assistant — Strixhaven: School of Mages #251 (canonical printing)
 * {4} · Artifact Creature — Gargoyle · 2/1
 *
 * Flying
 * When this creature enters, put up to one target instant or sorcery card from your graveyard on
 * top of your library.
 *
 * Meldweb Curator's enters trigger on a flyer: an optional ("up to one") graveyard target moved
 * with [Effects.PutOnTopOfLibrary].
 */
val BiblioplexAssistant = card("Biblioplex Assistant") {
    manaCost = "{4}"
    typeLine = "Artifact Creature — Gargoyle"
    oracleText = "Flying\n" +
        "When this creature enters, put up to one target instant or sorcery card from your graveyard on top of your library."
    power = 2
    toughness = 1

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.self.enters()
        val card = target(TargetFilter.InstantOrSorceryInYourGraveyard, optional = true)
        effect = Effects.PutOnTopOfLibrary(card)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "251"
        artist = "Raoul Vitale"
        flavorText = "\"I appreciate the help, and honestly, it's very entertaining.\"\n—Isabough of the Biblioplex"
        imageUri = "https://cards.scryfall.io/normal/front/d/7/d74e1117-0196-4268-be97-a1e81b5dc90e.jpg?1783927282"
    }
}
