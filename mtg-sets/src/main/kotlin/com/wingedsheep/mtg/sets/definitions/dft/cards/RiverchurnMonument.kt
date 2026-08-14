package com.wingedsheep.mtg.sets.definitions.dft.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.ForEachTargetEffect
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetPlayer

/**
 * Riverchurn Monument
 * {1}{U}
 * Artifact
 * {1}, {T}: Any number of target players each mill two cards.
 * Exhaust — {2}{U}{U}, {T}: Any number of target players each mill cards equal to the number of
 * cards in their graveyard. (Activate each exhaust ability only once.)
 *
 * Both abilities use `TargetPlayer(unlimited = true)` for "any number of target players" and
 * iterate the chosen players with [ForEachTargetEffect], so each player mills from their own
 * library via `Player.ContextPlayer(0)`. The exhaust amount is that player's own graveyard size,
 * evaluated per iteration — not the controller's.
 */
val RiverchurnMonument = card("Riverchurn Monument") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Artifact"
    oracleText = "{1}, {T}: Any number of target players each mill two cards.\n" +
        "Exhaust — {2}{U}{U}, {T}: Any number of target players each mill cards equal to the " +
        "number of cards in their graveyard. (Activate each exhaust ability only once.)"

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{1}"), Costs.Tap)
        target("any number of target players", TargetPlayer(unlimited = true))
        effect = ForEachTargetEffect(
            listOf(
                Patterns.Library.mill(2, EffectTarget.PlayerRef(Player.ContextPlayer(0)))
            )
        )
        description = "{1}, {T}: Any number of target players each mill two cards."
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{2}{U}{U}"), Costs.Tap)
        isExhaust = true
        target("any number of target players", TargetPlayer(unlimited = true))
        effect = ForEachTargetEffect(
            listOf(
                Patterns.Library.mill(
                    DynamicAmounts.zone(Player.ContextPlayer(0), Zone.GRAVEYARD).count(),
                    EffectTarget.PlayerRef(Player.ContextPlayer(0))
                )
            )
        )
        description = "Exhaust — {2}{U}{U}, {T}: Any number of target players each mill cards " +
            "equal to the number of cards in their graveyard."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "57"
        artist = "Anthony Devine"
        imageUri = "https://cards.scryfall.io/normal/front/e/6/e66ff696-fd39-49ad-9ee5-c0868167df37.jpg?1783907906"
        ruling("2025-02-07", "Exhaust abilities can be activated any time you could normally activate an ability.")
        ruling("2025-02-07", "If an exhaust ability of a permanent is activated, and then that permanent leaves the battlefield and returns to the battlefield, it becomes a new object so its exhaust ability can be activated again.")
        ruling("2025-02-07", "If an ability triggers whenever you activate an exhaust ability, that ability resolves before the exhaust ability resolves.")
    }
}
