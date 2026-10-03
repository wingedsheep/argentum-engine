package com.wingedsheep.mtg.sets.definitions.mh3.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.ProtectionScope
import com.wingedsheep.sdk.scripting.predicates.CardPredicate
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Breaker of Creation — Modern Horizons 3 #1 (uncommon)
 * {6}{C}{C} · Creature — Eldrazi · 8/4
 *
 * When you cast this spell, you gain 1 life for each colorless permanent you control.
 * Hexproof from each color
 * Annihilator 2
 *
 * Modeling notes:
 *  - The cast trigger resolves while the spell is still on the stack, so the spell never counts
 *    itself; lands are colorless permanents and count (CR 105.2c).
 *  - "Hexproof from each color" is shorthand for hexproof from all five colors — one
 *    `ProtectionScope.Colors` carrying every [Color].
 *  - Annihilator is display-only [KeywordAbility.Numeric] vocabulary, lowered as its attack-trigger
 *    edict on the defending player (same shape as Eldrazi Ravager).
 */
val BreakerOfCreation = card("Breaker of Creation") {
    manaCost = "{6}{C}{C}"
    colorIdentity = ""
    typeLine = "Creature — Eldrazi"
    power = 8
    toughness = 4
    oracleText = "When you cast this spell, you gain 1 life for each colorless permanent you control.\n" +
        "Hexproof from each color\n" +
        "Annihilator 2 (Whenever this creature attacks, defending player sacrifices two permanents " +
        "of their choice.)"

    triggeredAbility {
        trigger = Triggers.self.isCast()
        effect = Effects.GainLife(
            DynamicAmounts.battlefield(
                Player.You,
                GameObjectFilter.Permanent.withCardPredicate(CardPredicate.IsColorless)
            ).count()
        )
    }

    keywordAbility(KeywordAbility.Hexproof(ProtectionScope.Colors(Color.entries.toSet())))
    keywordAbility(KeywordAbility.annihilator(2))

    // Annihilator 2 — the lowering of the display-only keyword ability above.
    triggeredAbility {
        trigger = Triggers.self.attacks()
        effect = Effects.Sacrifice(
            GameObjectFilter.Permanent,
            2,
            EffectTarget.PlayerRef(Player.DefendingPlayer)
        )
        description = "Annihilator 2"
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "1"
        artist = "Yohann Schepacz"
        flavorText = "The shackles of reality were stifling. It would unmake all that had bound it."
        imageUri = "https://cards.scryfall.io/normal/front/7/2/72449552-aa2c-4ae3-846f-df523c5e6078.jpg?1783911311"

        ruling(
            "2024-06-07",
            "Breaker of Creation's triggered ability will resolve before Breaker of Creation does. " +
                "If Breaker of Creation is countered or otherwise leaves the stack in response to " +
                "that triggered ability, the triggered ability will still resolve as normal."
        )
        ruling(
            "2024-06-07",
            "Annihilator abilities trigger and resolve during the declare attackers step. The " +
                "defending player sacrifices the required number of permanents of their choice " +
                "before they declare blockers. Any creatures sacrificed this way won't be able " +
                "to block."
        )
        ruling(
            "2024-06-07",
            "If a creature with annihilator is attacking a planeswalker, and the defending " +
                "player chooses to sacrifice that planeswalker, the attacking creature continues " +
                "to attack. It may be blocked. If it isn't blocked, it simply won't deal combat " +
                "damage to anything."
        )
    }
}
