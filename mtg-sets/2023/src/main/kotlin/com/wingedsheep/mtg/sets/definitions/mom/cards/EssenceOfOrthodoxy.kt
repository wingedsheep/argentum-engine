package com.wingedsheep.mtg.sets.definitions.mom.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Essence of Orthodoxy — March of the Machine #323
 * {3}{W}{W} · Creature — Phyrexian 3/3
 *
 * Flying
 * Whenever this creature or another Phyrexian you control enters, incubate 2.
 *
 * This creature is itself a Phyrexian, so "this creature or another Phyrexian you control" is one
 * ANY-bound enters trigger over Phyrexians you control — its own arrival fires it too.
 */
val EssenceOfOrthodoxy = card("Essence of Orthodoxy") {
    manaCost = "{3}{W}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Phyrexian"
    power = 3
    toughness = 3
    oracleText = "Flying\n" +
        "Whenever this creature or another Phyrexian you control enters, incubate 2. (Create an Incubator " +
        "token with two +1/+1 counters on it and \"{2}: Transform this token.\" It transforms into a 0/0 " +
        "Phyrexian artifact creature.)"

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Permanent.withSubtype("Phyrexian").youControl()).enters()
        effect = Effects.Incubate(2)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "323"
        artist = "Oriana Menendez"
        imageUri = "https://cards.scryfall.io/normal/front/6/0/604233dc-0520-47d0-8d3d-816f45c9f087.jpg?1783916907"
    }
}
