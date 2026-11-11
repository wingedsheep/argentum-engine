package com.wingedsheep.mtg.sets.definitions.bfz.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Sludge Crawler
 * {B}
 * Creature — Eldrazi Drone
 * 1/1
 * Devoid (This card has no color.)
 * Ingest (Whenever this creature deals combat damage to a player, that player exiles the top card of their library.)
 * {2}: This creature gets +1/+1 until end of turn.
 *
 * Ingest is engine-live: declaring the keyword is the whole ability (see `Ingest`). The pump is an
 * ordinary generic-cost self-[Effects.ModifyStats] (until end of turn by default).
 */
val SludgeCrawler = card("Sludge Crawler") {
    manaCost = "{B}"
    colorIdentity = "B"
    typeLine = "Creature — Eldrazi Drone"
    power = 1
    toughness = 1
    oracleText = "Devoid (This card has no color.)\n" +
        "Ingest (Whenever this creature deals combat damage to a player, that player exiles the top card of their library.)\n" +
        "{2}: This creature gets +1/+1 until end of turn."

    keywords(Keyword.DEVOID, Keyword.INGEST)

    activatedAbility {
        cost = Costs.Mana("{2}")
        effect = Effects.ModifyStats(power = 1, toughness = 1, target = EffectTarget.Self)
        description = "{2}: This creature gets +1/+1 until end of turn."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "98"
        artist = "Johann Bodin"
        imageUri = "https://cards.scryfall.io/normal/front/9/7/9782b463-0677-40f4-ab2c-4c3ff19f1168.jpg?1783938204"
        ruling("2015-08-25", "The card exiled by the ingest ability is exiled face up.")
        ruling("2015-08-25", "If the player has no cards in their library when the ingest ability resolves, nothing happens. That player won't lose the game (until they have to draw a card from an empty library).")
    }
}
