package com.wingedsheep.mtg.sets.definitions.dka.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersWithCounters

/**
 * Gravetiller Wurm
 * {5}{G}
 * Creature — Wurm
 * 4/4
 * Trample
 * Morbid — This creature enters with four +1/+1 counters on it if a creature died this turn.
 */
val GravetillerWurm = card("Gravetiller Wurm") {
    manaCost = "{5}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Wurm"
    oracleText = "Trample\n" +
        "Morbid — This creature enters with four +1/+1 counters on it if a creature died this turn."
    power = 4
    toughness = 4
    keywords(Keyword.TRAMPLE)
    replacementEffect(EntersWithCounters(count = 4, selfOnly = true, condition = Conditions.CreatureDiedThisTurn))

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "116"
        artist = "Slawomir Maniak"
        flavorText = "As the creatures of the night closed in on humanity, Innistrad's primeval forces stirred, as though waking to watch the slaughter."
        imageUri = "https://cards.scryfall.io/normal/front/6/6/66d9fe36-2eac-49e3-8f89-810009ba8a4b.jpg"
    }
}
