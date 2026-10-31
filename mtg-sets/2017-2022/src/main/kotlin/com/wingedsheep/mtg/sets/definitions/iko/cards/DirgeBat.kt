package com.wingedsheep.mtg.sets.definitions.iko.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Dirge Bat
 * {2}{B}{B}
 * Creature — Bat
 * 3/3
 * Mutate {4}{B}{B}
 * Flash
 * Flying
 * Whenever this creature mutates, destroy target creature or planeswalker an opponent controls.
 */
val DirgeBat = card("Dirge Bat") {
    manaCost = "{2}{B}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Bat"
    power = 3
    toughness = 3
    oracleText = "Mutate {4}{B}{B} (If you cast this spell for its mutate cost, put it over or under target non-Human creature you own. They mutate into the creature on top plus all abilities from under it.)\n" +
        "Flash\n" +
        "Flying\n" +
        "Whenever this creature mutates, destroy target creature or planeswalker an opponent controls."

    keywordAbility(KeywordAbility.mutate("{4}{B}{B}"))
    keywords(Keyword.FLASH, Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.self.mutates()
        val victim = target(TargetFilter(GameObjectFilter.CreatureOrPlaneswalker).opponentControls())
        effect = Effects.Destroy(victim)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "84"
        artist = "Paul Scott Canavan"
        imageUri = "https://cards.scryfall.io/normal/front/5/9/59677c76-8148-4b6f-95b0-0e3ccf137a3f.jpg?1783931062"
    }
}
