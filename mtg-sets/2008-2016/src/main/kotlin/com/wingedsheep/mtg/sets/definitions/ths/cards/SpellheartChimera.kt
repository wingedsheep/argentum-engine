package com.wingedsheep.mtg.sets.definitions.ths.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Spellheart Chimera — Theros #204
 * {1}{U}{R} · Creature — Chimera · * /3
 *
 * Power is a characteristic-defining ability (printed *), so it is `dynamicPower` — it functions in
 * every zone, per the ruling — over the count of instant and sorcery cards in its controller's
 * graveyard. Toughness is the printed fixed 3.
 */
val SpellheartChimera = card("Spellheart Chimera") {
    manaCost = "{1}{U}{R}"
    colorIdentity = "UR"
    typeLine = "Creature — Chimera"
    dynamicPower(
        DynamicAmounts.count(Player.You, Zone.GRAVEYARD, GameObjectFilter.InstantOrSorcery),
    )
    toughness = 3
    oracleText = "Flying, trample\n" +
        "Spellheart Chimera's power is equal to the number of instant and sorcery cards in your graveyard."

    keywords(Keyword.FLYING, Keyword.TRAMPLE)

    metadata {
        ruling("2013-09-15", "The ability that defines Spellheart Chimera's power functions in all zones, not just the battlefield.")
        rarity = Rarity.UNCOMMON
        collectorNumber = "204"
        artist = "Svetlin Velinov"
        flavorText = "Thaumaturges remain silent around chimeras, lest their words conjure even stranger beasts."
        imageUri = "https://cards.scryfall.io/normal/front/0/a/0a19670c-5afa-4e22-b3ff-258faca19556.jpg?1783939724"
    }
}
