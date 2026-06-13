package com.wingedsheep.mtg.sets.definitions.sos.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.KeywordAbility

/**
 * Duel Tactics
 * {R}
 * Sorcery
 * Duel Tactics deals 1 damage to target creature. It can't block this turn.
 * Flashback {1}{R}
 *
 * The damaged creature is also prevented from blocking for the rest of the turn
 * ([Effects.CantBlock] with [Duration.EndOfTurn]). Flashback lets the spell be recast from the
 * graveyard for {1}{R}, then exiles it.
 */
val DuelTactics = card("Duel Tactics") {
    manaCost = "{R}"
    colorIdentity = "R"
    typeLine = "Sorcery"
    oracleText = "Duel Tactics deals 1 damage to target creature. It can't block this turn.\n" +
        "Flashback {1}{R} (You may cast this card from your graveyard for its flashback cost. Then exile it.)"

    keywordAbility(KeywordAbility.flashback("{1}{R}"))

    spell {
        val creature = target("target creature", Targets.Creature)
        effect = Effects.DealDamage(1, creature) then
            Effects.CantBlock(creature, Duration.EndOfTurn)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "112"
        artist = "Craig J Spearing"
        flavorText = "\"My trick? Hit their casting hand and they lose their advantage. So far, I'm undefeated.\"" +
            "\n—Alessia, Lorehold duelist"
        imageUri = "https://cards.scryfall.io/normal/front/8/f/8f3a1675-0cc7-4dfd-a12e-4740a2cf81e8.jpg?1775937718"
    }
}
