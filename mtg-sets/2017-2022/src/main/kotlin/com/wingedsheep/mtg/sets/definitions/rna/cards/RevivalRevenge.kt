package com.wingedsheep.mtg.sets.definitions.rna.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.times
import com.wingedsheep.sdk.model.CardLayout
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Revival // Revenge (RNA 228) — split-layout spell (CR 709).
 *
 * Revival {W/B}{W/B} — Sorcery
 *   Return target creature card with mana value 3 or less from your graveyard to the battlefield.
 *
 * Revenge {4}{W}{B} — Sorcery
 *   Double your life total. Target opponent loses half their life, rounded up.
 *
 * Doubling is "your life total becomes twice what it was" (the Beacon of Immortality shape): the
 * set-life executor turns the difference into life gain (or loss, below 0), so life-gain
 * replacements and triggers see it. The opponent's half is read off their life as Revenge resolves.
 */
val RevivalRevenge = card("Revival // Revenge") {
    layout = CardLayout.SPLIT
    colorIdentity = "WB"

    face("Revival") {
        manaCost = "{W/B}{W/B}"
        typeLine = "Sorcery"
        oracleText = "Return target creature card with mana value 3 or less from your graveyard to the battlefield."

        spell {
            val creature = target(TargetFilter.CreatureInYourGraveyard.manaValueAtMost(3))
            effect = Effects.PutOntoBattlefield(creature)
        }
    }

    face("Revenge") {
        manaCost = "{4}{W}{B}"
        typeLine = "Sorcery"
        oracleText = "Double your life total. Target opponent loses half their life, rounded up."

        spell {
            val opponent = target(Targets.Opponent)
            effect = Effects.SetLifeTotal(DynamicAmounts.lifeTotal(Player.You) * 2) then
                Effects.LoseHalfLife(roundUp = true, target = opponent, lifePlayer = opponent.asPlayer)
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "228"
        artist = "Paul Scott Canavan"
        imageUri = "https://cards.scryfall.io/normal/front/5/0/50ae0831-f3ba-4535-bfb6-feefbbc15275.jpg?1783933626"
        ruling(
            "2019-01-25",
            "To double a player's life total, that player gains as much life as needed so that their life total is twice the number it was before. " +
                "If their life total was negative, that player loses as much life as needed so that their life total is twice as far below 0 as it was before. " +
                "Other effects interact with this life gain or loss accordingly."
        )
        ruling("2019-01-25", "If a card in a graveyard has {X} in its mana cost, X is considered to be 0.")
    }
}
