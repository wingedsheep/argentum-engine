package com.wingedsheep.mtg.sets.definitions.iko.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.EntityNumericProperty

/**
 * Insatiable Hemophage
 * {3}{B}
 * Creature — Nightmare
 * 3/3
 * Mutate {2}{B}
 * Deathtouch
 * Whenever this creature mutates, each opponent loses X life and you gain X life, where X is the
 * number of times this creature has mutated.
 *
 * "You gain X life" is a fixed X, not "life lost this way" ([Effects.DrainLife]): you gain X once,
 * however many opponents there are and whether or not they could lose life.
 */
val InsatiableHemophage = card("Insatiable Hemophage") {
    manaCost = "{3}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Nightmare"
    power = 3
    toughness = 3
    oracleText = "Mutate {2}{B} (If you cast this spell for its mutate cost, put it over or under target non-Human creature you own. They mutate into the creature on top plus all abilities from under it.)\n" +
        "Deathtouch\n" +
        "Whenever this creature mutates, each opponent loses X life and you gain X life, where X is the number of times this creature has mutated."

    keywordAbility(KeywordAbility.mutate("{2}{B}"))
    keywords(Keyword.DEATHTOUCH)

    triggeredAbility {
        trigger = Triggers.self.mutates()
        val timesMutated = DynamicAmounts.propertyOf(EffectTarget.Self, EntityNumericProperty.TimesMutated)
        effect = Effects.LoseLife(timesMutated, EffectTarget.PlayerRef(Player.EachOpponent)) then
            Effects.GainLife(timesMutated)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "93"
        artist = "Antonio José Manzanedo"
        imageUri = "https://cards.scryfall.io/normal/front/7/0/70419590-2e0d-4232-b596-a5359b284647.jpg"
    }
}
