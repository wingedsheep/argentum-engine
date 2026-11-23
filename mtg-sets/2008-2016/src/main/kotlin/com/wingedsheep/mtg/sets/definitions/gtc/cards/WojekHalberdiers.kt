package com.wingedsheep.mtg.sets.definitions.gtc.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.events.AttackPredicate
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Wojek Halberdiers
 * {R}{W}
 * Creature — Human Soldier
 * 3/2
 * Battalion — Whenever this creature and at least two other creatures attack, this creature gains
 * first strike until end of turn.
 */
val WojekHalberdiers = card("Wojek Halberdiers") {
    manaCost = "{R}{W}"
    colorIdentity = "RW"
    typeLine = "Creature — Human Soldier"
    power = 3
    toughness = 2
    oracleText = "Battalion — Whenever this creature and at least two other creatures attack, this creature " +
        "gains first strike until end of turn."

    triggeredAbility {
        trigger = Triggers.self.attacks(setOf(AttackPredicate.AttackerCountAtLeast(3)))
        effect = Effects.GrantKeyword(Keyword.FIRST_STRIKE, EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "208"
        artist = "Nic Klein"
        flavorText = "Tired of Boros interference, the Rakdos unleashed hellraiser goblins, hoping their corpses would weigh down the halberds."
        imageUri = "https://cards.scryfall.io/normal/front/4/2/423f0870-dc1c-4cd8-b92c-6d5f92abbaec.jpg?1783940097"
    }
}
