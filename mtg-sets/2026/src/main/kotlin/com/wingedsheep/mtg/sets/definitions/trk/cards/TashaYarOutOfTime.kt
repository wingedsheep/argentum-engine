package com.wingedsheep.mtg.sets.definitions.trk.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.effects.MayPlayExpiry
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Tasha Yar, Out of Time
 * {1}{R}
 * Legendary Creature — Human Officer
 * 2/1
 *
 * Haste
 * When Tasha Yar dies, exile the top card of your library. Until the end of your next turn, you
 * may play that card.
 * {2}{R}, Exile this card from your graveyard: Create a token that's a copy of this card.
 * Sacrifice it at the beginning of the next end step. Activate only as a sorcery.
 *
 * The dies trigger is the impulse-draw recipe with the turn-keyed `UntilEndOfNextTurn` window
 * (Crimson Operative). The graveyard ability is encore's shape without the per-opponent loop: the
 * cost exiles the card, then a token copy of it (resolved through [EffectTarget.Self], now in
 * exile) is created with a sacrifice at the next end step. The copy has haste and the dies
 * trigger as copiable values, so it can attack at once and impulses again when sacrificed.
 */
val TashaYarOutOfTime = card("Tasha Yar, Out of Time") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Legendary Creature — Human Officer"
    power = 2
    toughness = 1
    oracleText = "Haste\n" +
        "When Tasha Yar dies, exile the top card of your library. Until the end of your next turn, " +
        "you may play that card.\n" +
        "{2}{R}, Exile this card from your graveyard: Create a token that's a copy of this card. " +
        "Sacrifice it at the beginning of the next end step. Activate only as a sorcery."

    keywords(Keyword.HASTE)

    triggeredAbility {
        trigger = Triggers.self.dies()
        effect = Patterns.Exile.impulse(
            count = 1,
            expiry = MayPlayExpiry.UntilEndOfNextTurn
        )
        description = "When Tasha Yar dies, exile the top card of your library. Until the end of " +
            "your next turn, you may play that card."
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{2}{R}"), Costs.ExileSelf)
        effect = Effects.CreateTokenCopyOfTarget(
            target = EffectTarget.Self,
            sacrificeAtStep = Step.END,
        )
        timing = TimingRule.SorcerySpeed
        activateFromZone = Zone.GRAVEYARD
        description = "{2}{R}, Exile this card from your graveyard: Create a token that's a copy " +
            "of this card. Sacrifice it at the beginning of the next end step. Activate only as a sorcery."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "176"
        artist = "Patricia Pria"
        imageUri = "https://cards.scryfall.io/normal/front/5/f/5f6cbe6c-5cd8-4eec-82b7-25d5bf5641c0.jpg?1793100738"
    }
}
