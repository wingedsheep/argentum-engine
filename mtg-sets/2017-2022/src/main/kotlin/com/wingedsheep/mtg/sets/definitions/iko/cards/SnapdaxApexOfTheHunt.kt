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
 * Snapdax, Apex of the Hunt — Ikoria: Lair of Behemoths #209
 * {1}{R}{W}{B} · Legendary Creature — Dinosaur Cat Nightmare 3/5
 *
 * Mutate {2}{B/R}{W}{W}
 * Double strike
 * Whenever this creature mutates, it deals 4 damage to target creature or planeswalker an
 * opponent controls and you gain 4 life.
 */
val SnapdaxApexOfTheHunt = card("Snapdax, Apex of the Hunt") {
    manaCost = "{1}{R}{W}{B}"
    colorIdentity = "BRW"
    typeLine = "Legendary Creature — Dinosaur Cat Nightmare"
    power = 3
    toughness = 5
    oracleText = "Mutate {2}{B/R}{W}{W} (If you cast this spell for its mutate cost, put it over or under target non-Human creature you own. They mutate into the creature on top plus all abilities from under it.)\n" +
        "Double strike\n" +
        "Whenever this creature mutates, it deals 4 damage to target creature or planeswalker an opponent controls and you gain 4 life."

    keywordAbility(KeywordAbility.mutate("{2}{B/R}{W}{W}"))
    keywords(Keyword.DOUBLE_STRIKE)

    triggeredAbility {
        trigger = Triggers.self.mutates()
        val t = target(TargetFilter(GameObjectFilter.CreatureOrPlaneswalker).opponentControls())
        effect = Effects.DealDamage(4, t) then Effects.GainLife(4)
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "209"
        artist = "Viktor Titov"
        imageUri = "https://cards.scryfall.io/normal/front/5/1/51fdeeb7-9868-4fc2-9c53-806244cd5488.jpg?1783931016"
    }
}
