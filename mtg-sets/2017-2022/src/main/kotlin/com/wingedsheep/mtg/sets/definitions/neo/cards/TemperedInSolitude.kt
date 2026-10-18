package com.wingedsheep.mtg.sets.definitions.neo.cards

import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.events.AttackPredicate

/**
 * Tempered in Solitude — Kamigawa: Neon Dynasty #165 (canonical printing)
 * {1}{R} · Enchantment
 *
 * Whenever a creature you control attacks alone, exile the top card of your library.
 * You may play that card this turn.
 */
val TemperedInSolitude = card("Tempered in Solitude") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Enchantment"
    oracleText = "Whenever a creature you control attacks alone, exile the top card of your library. " +
        "You may play that card this turn."

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Creature.youControl()).attacks(setOf(AttackPredicate.Alone))
        effect = Patterns.Exile.impulse(1)
        description = "Whenever a creature you control attacks alone, exile the top card of your library. " +
            "You may play that card this turn."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "165"
        artist = "Chris Cold"
        flavorText = "Many master samurai make pilgrimages deep into the Sokenzan Mountains, where they " +
            "train and meditate in silence, hoping to commune with a lava kami."
        imageUri = "https://cards.scryfall.io/normal/front/2/c/2cfd0693-6dba-4c0d-8c42-859d7bf38c40.jpg?1783923858"
    }
}
