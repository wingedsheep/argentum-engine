package com.wingedsheep.mtg.sets.definitions.mkm.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Loxodon Eavesdropper — Murders at Karlov Manor #168
 * {3}{G} · Creature — Elephant Detective · 3/3
 *
 * When this creature enters, investigate.
 * Whenever you draw your second card each turn, this creature gets +1/+1 and gains vigilance
 * until end of turn.
 *
 * The two halves are deliberately linked: the Clue the enters trigger leaves behind is itself a
 * draw, so cracking it on a turn you've already drawn for the turn is what turns the second clause
 * on. `Triggers.NthCardDrawn(2)` counts *draws* (CR 121.2) — a card put into hand without the word
 * "draw" (CR 121.5) doesn't advance it, and a single multi-card draw that crosses two still fires
 * the trigger exactly once.
 *
 * The payoff names the source rather than a target, so both halves are `EffectTarget.Self` under
 * one composite: the +1/+1 and the vigilance are one ability's effect and share the end-of-turn
 * duration. Vigilance arriving *after* attackers are declared does nothing, which is why the
 * interesting line is drawing at instant speed during the opponent's turn or in your first main
 * phase — not after combat.
 */
val LoxodonEavesdropper = card("Loxodon Eavesdropper") {
    manaCost = "{3}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Elephant Detective"
    oracleText = "When this creature enters, investigate. (Create a Clue token. It's an artifact " +
        "with \"{2}, Sacrifice this token: Draw a card.\")\n" +
        "Whenever you draw your second card each turn, this creature gets +1/+1 and gains " +
        "vigilance until end of turn."
    power = 3
    toughness = 3

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = Effects.Investigate()
    }

    triggeredAbility {
        trigger = Triggers.NthCardDrawn(2)
        effect = Effects.Composite(
            Effects.ModifyStats(1, 1, EffectTarget.Self),
            Effects.GrantKeyword(Keyword.VIGILANCE, EffectTarget.Self)
        )
        description = "Whenever you draw your second card each turn, this creature gets +1/+1 " +
            "and gains vigilance until end of turn."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "168"
        artist = "Jesper Ejsing"
        flavorText = "He's all ears."
        imageUri = "https://cards.scryfall.io/normal/front/b/b/bbbf8c3a-6c74-42fd-bb8d-61e3f0a77848.jpg?1783912865"

        ruling(
            "2024-02-02",
            "Clue is an artifact type. Even though it appears on some cards with other permanent " +
                "types, it's never a creature type, a land type, or anything but an artifact type."
        )
        ruling(
            "2024-02-02",
            "If an effect refers to a Clue, it means any Clue artifact, not just a Clue artifact token."
        )
    }
}
