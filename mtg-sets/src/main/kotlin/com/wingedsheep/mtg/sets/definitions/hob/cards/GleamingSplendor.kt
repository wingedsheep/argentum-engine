package com.wingedsheep.mtg.sets.definitions.hob.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetPlayer

/**
 * Gleaming Splendor — The Hobbit #15
 * {1}{W} · Enchantment · Mythic
 *
 * Whenever an opponent draws their second card each turn, you create a Treasure token.
 * {2}{W}: Two target players each draw a card.
 *
 * The trigger is [Triggers.NthCardDrawn]`(2, Player.EachOpponent)` (CR 121.2 — each card drawn is
 * an individual draw, so a multi-card draw fires it at most once, when the second card lands
 * inside that batch). `EachOpponent` scopes the per-turn draw counter to every player who isn't
 * the controller, so in multiplayer each opponent's own second draw fires it separately.
 *
 * The activated ability's "two target players" is a single count-2 [TargetPlayer] requirement —
 * the engine enforces that the two chosen players are distinct (CR 115.1b) — referenced
 * positionally by [EffectTarget.ContextTarget], the same shape as Parker Luck. Each targeted
 * player draws their own card, so a player who becomes an illegal target on resolution simply
 * doesn't draw.
 */
val GleamingSplendor = card("Gleaming Splendor") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Enchantment"
    oracleText = "Whenever an opponent draws their second card each turn, you create a Treasure token.\n" +
        "{2}{W}: Two target players each draw a card."

    triggeredAbility {
        trigger = Triggers.NthCardDrawn(2, Player.EachOpponent)
        effect = Effects.CreateTreasure()
        description = "Whenever an opponent draws their second card each turn, you create a Treasure token."
    }

    activatedAbility {
        cost = Costs.Mana("{2}{W}")
        target("two target players", TargetPlayer(count = 2))
        effect = Effects.Composite(
            listOf(
                Effects.DrawCards(1, EffectTarget.ContextTarget(0)),
                Effects.DrawCards(1, EffectTarget.ContextTarget(1)),
            )
        )
        description = "Two target players each draw a card."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "15"
        artist = "Kekai Kotaki"
        flavorText = "Bilbo had heard tell and sing of Dragon-hoards before, but the splendor, the " +
            "lust, the glory of such treasure had never yet come home to him."
        imageUri = "https://cards.scryfall.io/normal/front/3/b/3b087bd4-bbb7-4963-bdb6-0a700ff19a04.jpg?1785496942"
    }
}
