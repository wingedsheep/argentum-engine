package com.wingedsheep.mtg.sets.definitions.dominaria.cards

import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.effects.DealDamageEffect
import com.wingedsheep.sdk.scripting.effects.DividedDamageEffect
import com.wingedsheep.sdk.scripting.targets.AnyTarget
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetCreature

/**
 * Fight with Fire
 * {2}{R}
 * Sorcery
 * Kicker {5}{R}
 * Fight with Fire deals 5 damage to target creature. If this spell was kicked,
 * it deals 10 damage divided as you choose among any number of targets instead.
 *
 * Note: The kicked version completely changes the targeting mode (any target, up to 10)
 * and the effect (divided damage instead of single target damage).
 */
val FightWithFire = card("Fight with Fire") {
    manaCost = "{2}{R}"
    typeLine = "Sorcery"
    oracleText = "Kicker {5}{R} (You may pay an additional {5}{R} as you cast this spell.)\nFight with Fire deals 5 damage to target creature. If this spell was kicked, it deals 10 damage divided as you choose among any number of targets instead."

    keywordAbility(KeywordAbility.Kicker(ManaCost.parse("{5}{R}")))

    spell {
        // Unkicked: 5 damage to target creature
        target = TargetCreature()
        effect = DealDamageEffect(5, EffectTarget.ContextTarget(0))

        // Kicked: 10 damage divided among any number of targets (up to 10)
        kickerTarget = AnyTarget(count = 10, minCount = 1)
        kickerEffect = DividedDamageEffect(
            totalDamage = 10,
            minTargets = 1,
            maxTargets = 10
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "119"
        artist = "Yongjae Choi"
        imageUri = "https://cards.scryfall.io/normal/front/7/2/72c94d7a-cad7-438f-bc96-e6a7158ab0e6.jpg?1562737735"
    }
}
