package com.wingedsheep.mtg.sets.definitions.neo.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Befriending the Moths // Imperial Moth
 * {3}{W} — Enchantment — Saga // Enchantment Creature — Insect 2/4
 *
 * Front — Befriending the Moths:
 *   I, II — Target creature you control gets +1/+1 and gains flying until end of turn.
 *   III — Exile this Saga, then return it to the battlefield transformed under your control.
 *
 * Back — Imperial Moth:
 *   Flying
 */
private val ImperialMoth = card("Imperial Moth") {
    manaCost = ""
    colorIdentity = "W"
    typeLine = "Enchantment Creature — Insect"
    oracleText = "Flying"
    power = 2
    toughness = 4

    keywords(Keyword.FLYING)

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "4"
        artist = "Matt Stewart"
        flavorText = "\"The moths of Eiganjo were once no different from any other great beast of " +
            "Kamigawa's wilds. It took years of patience and dedication to convince them to bear " +
            "riders.\"\n—*History of the Empire*"
        imageUri = "https://cards.scryfall.io/normal/back/8/a/8ad44884-ae0d-40ae-87a9-bad043d4e9ad.jpg?1783923933"
    }
}

private val BefriendingTheMothsFront = card("Befriending the Moths") {
    manaCost = "{3}{W}"
    colorIdentity = "W"
    typeLine = "Enchantment — Saga"
    oracleText = "(As this Saga enters and after your draw step, add a lore counter.)\n" +
        "I, II — Target creature you control gets +1/+1 and gains flying until end of turn.\n" +
        "III — Exile this Saga, then return it to the battlefield transformed under your control."

    for (chapter in 1..2) {
        sagaChapter(chapter) {
            val creature = target(TargetFilter.CreatureYouControl)
            effect = Effects.ModifyStats(1, 1, creature) then Effects.GrantKeyword(Keyword.FLYING, creature)
        }
    }

    sagaChapter(3) {
        effect = Effects.ExileAndReturnTransformed()
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "4"
        artist = "Matt Stewart"
        imageUri = "https://cards.scryfall.io/normal/front/8/a/8ad44884-ae0d-40ae-87a9-bad043d4e9ad.jpg?1783923933"
    }
}

val BefriendingTheMoths: CardDefinition = CardDefinition.doubleFacedPermanent(
    frontFace = BefriendingTheMothsFront,
    backFace = ImperialMoth,
)
