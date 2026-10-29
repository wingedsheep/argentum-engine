package com.wingedsheep.mtg.sets.definitions.dsc.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Suspended Sentence
 * {3}{B}
 * Instant
 * Destroy target creature an opponent controls. That player loses 3 life. Exile Suspended
 * Sentence with three time counters on it.
 * Suspend 3—{1}{B}
 */
val SuspendedSentence = card("Suspended Sentence") {
    manaCost = "{3}{B}"
    colorIdentity = "B"
    typeLine = "Instant"
    oracleText = "Destroy target creature an opponent controls. That player loses 3 life. " +
        "Exile Suspended Sentence with three time counters on it.\n" +
        "Suspend 3—{1}{B} (Rather than cast this card from your hand, you may pay {1}{B} and exile it with " +
        "three time counters on it. At the beginning of your upkeep, remove a time counter. When the last is " +
        "removed, you may cast it without paying its mana cost.)"

    keywordAbility(KeywordAbility.suspend("{1}{B}", 3))

    spell {
        val creature = target(TargetFilter.CreatureOpponentControls)
        effect = Effects.Destroy(creature) then Effects.LoseLife(3, EffectTarget.TargetController)
        selfExile(timeCounters = 3)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "25"
        artist = "L.A. Draws"
        imageUri = "https://cards.scryfall.io/normal/front/3/8/38c98aa0-bfa1-42f4-b035-a48dac86f0ce.jpg?1783909657"
    }
}
