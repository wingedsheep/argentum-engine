package com.wingedsheep.mtg.sets.definitions.dgm.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.events.AttackPredicate
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Boros Mastiff
 * {1}{W}
 * Creature — Dog
 * 2/2
 * Battalion — Whenever this creature and at least two other creatures attack, this creature gains
 * lifelink until end of turn.
 *
 * Battalion is the self attack trigger with `AttackerCountAtLeast(3)` (Makeshift Battalion).
 */
val BorosMastiff = card("Boros Mastiff") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Dog"
    power = 2
    toughness = 2
    oracleText = "Battalion — Whenever this creature and at least two other creatures attack, this creature " +
        "gains lifelink until end of turn. (Damage dealt by a creature with lifelink also causes its " +
        "controller to gain that much life.)"

    triggeredAbility {
        trigger = Triggers.self.attacks(setOf(AttackPredicate.AttackerCountAtLeast(3)))
        effect = Effects.GrantKeyword(Keyword.LIFELINK, EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "1"
        artist = "Kev Walker"
        imageUri = "https://cards.scryfall.io/normal/front/2/7/27a3bfb6-3843-4bda-bbcb-905e4b351dea.jpg?1783940045"
    }
}
