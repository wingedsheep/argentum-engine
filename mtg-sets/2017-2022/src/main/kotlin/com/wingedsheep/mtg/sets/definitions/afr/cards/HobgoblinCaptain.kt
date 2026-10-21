package com.wingedsheep.mtg.sets.definitions.afr.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.events.AttackPredicate
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Hobgoblin Captain
 * {1}{R}
 * Creature — Goblin Barbarian
 * 3/1
 * Pack tactics — Whenever this creature attacks, if you attacked with creatures with total power 6
 * or greater this combat, this creature gains first strike until end of turn.
 */
val HobgoblinCaptain = card("Hobgoblin Captain") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Goblin Barbarian"
    oracleText = "Pack tactics — Whenever this creature attacks, if you attacked with creatures with " +
        "total power 6 or greater this combat, this creature gains first strike until end of turn."
    power = 3
    toughness = 1

    triggeredAbility {
        trigger = Triggers.self.attacks(setOf(AttackPredicate.AttackersTotalPowerAtLeast(6)))
        effect = Effects.GrantKeyword(Keyword.FIRST_STRIKE, EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "148"
        artist = "Karl Kopinski"
        flavorText = "Hobgoblins do not fear death, for those who fall in battle have gained glory eternal."
        imageUri = "https://cards.scryfall.io/normal/front/f/7/f7c0aa86-fd3b-4e0f-9d37-ce1ea8922243.jpg?1783926478"
    }
}
