package com.wingedsheep.mtg.sets.definitions.mh3.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Kozilek's Command
 * {X}{C}{C}
 * Kindred Instant — Eldrazi
 * Choose two —
 * • Target player creates X 0/1 colorless Eldrazi Spawn creature tokens with "Sacrifice this token: Add {C}."
 * • Target player scries X, then draws a card.
 * • Exile target creature with mana value X or less.
 * • Exile up to X target cards from graveyards.
 *
 * Every mode reads the one X paid at cast time ([DynamicAmounts.xValue]), as Profane Command does.
 * Each mode declares its own target, so a target is only chosen for the modes picked (CR 700.2).
 *  - Spawn: the dynamic-count `CreateEldraziSpawn` with the targeted player as the tokens' controller.
 *  - Scry: the dynamic, player-scoped `Effects.Scry(X, player)` — the target player looks at and
 *    decides on their own library (X = 0 is no scry at all), then that same player draws.
 *  - Exile creature: an MV ≤ X target filter (`manaValueAtMostX`).
 *  - Graveyards: "up to X" target cards from any graveyards (no single-graveyard restriction).
 */
val KozileksCommand = card("Kozilek's Command") {
    manaCost = "{X}{C}{C}"
    typeLine = "Kindred Instant — Eldrazi"
    oracleText = "Choose two —\n" +
        "• Target player creates X 0/1 colorless Eldrazi Spawn creature tokens with \"Sacrifice this token: Add {C}.\"\n" +
        "• Target player scries X, then draws a card.\n" +
        "• Exile target creature with mana value X or less.\n" +
        "• Exile up to X target cards from graveyards."

    spell {
        modal(chooseCount = 2) {
            mode("Target player creates X 0/1 colorless Eldrazi Spawn creature tokens") {
                val player = target(Targets.Player)
                effect = Effects.CreateEldraziSpawn(DynamicAmounts.xValue(), controller = player)
            }
            mode("Target player scries X, then draws a card") {
                val player = target(Targets.Player)
                effect = Effects.Scry(DynamicAmounts.xValue(), player) then Effects.DrawCards(1, player)
            }
            mode("Exile target creature with mana value X or less") {
                val creature = target(TargetFilter.Creature.manaValueAtMostX())
                effect = Effects.Exile(creature)
            }
            mode("Exile up to X target cards from graveyards") {
                targets(TargetFilter.CardInGraveyard, optional = true, dynamicMaxCount = DynamicAmounts.xValue())
                effect = Effects.ForEachTarget(Effects.Exile(EffectTarget.ContextTarget(0)))
            }
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "11"
        artist = "Yeong-Hao Han"
        imageUri = "https://cards.scryfall.io/normal/front/9/2/92585587-cfdc-406a-9114-4f6dd8802c37.jpg?1784634156"
        ruling(
            "2024-06-07",
            "If all of Kozilek's Command's targets are illegal as it tries to resolve, it will do nothing. " +
                "If at least one target is still legal, it will resolve and do as much as it can."
        )
        ruling("2024-06-07", "If a creature has {X} in its mana cost, X is 0 when determining its mana value.")
    }
}
