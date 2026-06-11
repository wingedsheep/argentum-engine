package com.wingedsheep.mtg.sets.definitions.tmt.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Oroku Saki, Shredder Rising
 * {2}{B}
 * Legendary Creature — Human Ninja
 * 3/1
 *
 * Sneak {1}{B} (You may cast this spell for {1}{B} if you also return an
 * unblocked attacker you control to hand during the declare blockers step. He
 * enters tapped and attacking.)
 * Whenever Oroku Saki deals combat damage to a player, you draw a card and lose 1 life.
 */
val OrokuSakiShredderRising = card("Oroku Saki, Shredder Rising") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Legendary Creature — Human Ninja"
    oracleText = "Sneak {1}{B} (You may cast this spell for {1}{B} if you also return an unblocked attacker you control to hand during the declare blockers step. He enters tapped and attacking.)\nWhenever Oroku Saki deals combat damage to a player, you draw a card and lose 1 life."
    power = 3
    toughness = 1

    sneak("{1}{B}")

    triggeredAbility {
        trigger = Triggers.DealsCombatDamageToPlayer
        effect = Effects.DrawCards(1).then(Effects.LoseLife(1, EffectTarget.Controller))
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "68"
        artist = "Randy Gallegos"
        imageUri = "https://cards.scryfall.io/normal/front/e/6/e65ff1f4-a061-4c31-a78e-92af6e7bc56f.jpg?1771586878"
    }
}
