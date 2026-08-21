package com.wingedsheep.mtg.sets.definitions.mh2.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Jade Avenger — Modern Horizons 2 #167
 * {1}{G} · Creature — Frog Samurai · 2 / 2
 *
 * Bushido 2 (Whenever this creature blocks or becomes blocked, it gets +2/+2 until end of turn.)
 *
 * **Bushido is lowered here, not handled by the engine.** [KeywordAbility.bushido] is display-only
 * vocabulary — nothing in the rules engine reads `Keyword.BUSHIDO` — so the ability it abbreviates
 * is wired explicitly, the same way modular is lowered on the Arcbound cycle.
 *
 * CR 702.45a defines bushido N as a single triggered ability, "Whenever this creature blocks or
 * becomes blocked, it gets +N/+N until end of turn." The SDK has no "blocks or becomes blocked"
 * event covering both directions from the source's point of view — [Triggers.BlocksOrBecomesBlockedBy]
 * is about a *partner* creature — so it is written as two triggers over the two distinct events,
 * mirroring the attacks-or-blocks pair on `lci/cards/BurningSunCavalry.kt`. They are mutually
 * exclusive for any one combat: the Avenger either declares a block or is blocked, never both, so
 * the pump never doubles.
 *
 * The pump targets [EffectTarget.Self] rather than `TriggeringEntity` because [Triggers.Blocks]
 * fires off a block event that does not bind the source as the triggering entity; every corpus
 * "whenever this creature blocks, it gets …" card uses `Self` (`ulg/cards/SustainerOfTheRealm.kt`).
 */
val JadeAvenger = card("Jade Avenger") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Frog Samurai"
    power = 2
    toughness = 2
    oracleText = "Bushido 2 (Whenever this creature blocks or becomes blocked, it gets +2/+2 until end of turn.)"

    keywordAbility(KeywordAbility.bushido(2))

    // Bushido 2, half one: "Whenever this creature blocks …"
    triggeredAbility {
        trigger = Triggers.Blocks
        effect = Effects.ModifyStats(2, 2, EffectTarget.Self)
        description = "Bushido 2"
    }

    // Bushido 2, half two: "… or becomes blocked, it gets +2/+2 until end of turn."
    triggeredAbility {
        trigger = Triggers.BecomesBlocked
        effect = Effects.ModifyStats(2, 2, EffectTarget.Self)
        description = "Bushido 2"
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "167"
        artist = "Chris Seaman"
        flavorText = "\"Froggy fighter at the gate.\nDraw your sword and meet your fate.\"\n—Traditional children's rhyme"
        imageUri = "https://cards.scryfall.io/normal/front/f/8/f81500be-c959-4f38-bcf2-d63519168f67.jpg?1783926829"
    }
}
