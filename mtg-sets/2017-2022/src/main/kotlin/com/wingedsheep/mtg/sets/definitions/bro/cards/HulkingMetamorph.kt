package com.wingedsheep.mtg.sets.definitions.bro.cards

import com.wingedsheep.sdk.core.CardType
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersAsCopy
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.effects.CopyExceptions

/**
 * Hulking Metamorph
 * {9}
 * Artifact Creature — Shapeshifter
 * 7/7
 * Prototype {2}{U}{U} — 3/3
 * You may have this creature enter as a copy of an artifact or creature you control, except it's an
 * artifact creature in addition to its other types, and its power and toughness are equal to this
 * creature's power and toughness.
 *
 * `retainPowerToughness` keeps the entrant's own base P/T — 3/3 when cast prototyped, 7/7 otherwise.
 */
val HulkingMetamorph = card("Hulking Metamorph") {
    manaCost = "{9}"
    colorIdentity = "U"
    typeLine = "Artifact Creature — Shapeshifter"
    power = 7
    toughness = 7
    oracleText = "Prototype {2}{U}{U} — 3/3 (You may cast this spell with different mana cost, color, and size. It keeps its abilities and types.)\n" +
        "You may have this creature enter as a copy of an artifact or creature you control, except it's an artifact " +
        "creature in addition to its other types, and its power and toughness are equal to this creature's power and toughness."

    keywordAbility(KeywordAbility.prototype("{2}{U}{U}", 3, 3))

    replacementEffect(
        EntersAsCopy(
            optional = true,
            copyFilter = GameObjectFilter.CreatureOrArtifact.youControl(),
            exceptions = CopyExceptions(
                addedCardTypes = setOf(CardType.ARTIFACT, CardType.CREATURE),
                retainPowerToughness = true,
            ),
        )
    )

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "79"
        artist = "Diego Gisbert"
        imageUri = "https://cards.scryfall.io/normal/front/4/2/420f0b81-8b87-4854-9dc4-6da84cc38623.jpg?1783920097"
        ruling("2022-10-14", "Use the appropriate printed power and toughness (either 7/7 or 3/3) to determine the result of Hulking Metamorph's copy effect.")
        ruling("2022-10-14", "You can choose not to copy anything. In that case, Hulking Metamorph simply enters the battlefield as a 7/7 (or 3/3) artifact creature.")
    }
}
