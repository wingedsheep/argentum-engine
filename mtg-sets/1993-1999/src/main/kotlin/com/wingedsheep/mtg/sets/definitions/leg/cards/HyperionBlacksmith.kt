package com.wingedsheep.mtg.sets.definitions.leg.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.Mode
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Hyperion Blacksmith
 * {1}{R}{R}
 * Creature — Human Artificer
 * 2/2
 *
 * {T}: You may tap or untap target artifact an opponent controls.
 */
val HyperionBlacksmith = card("Hyperion Blacksmith") {
    manaCost = "{1}{R}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Human Artificer"
    oracleText = "{T}: You may tap or untap target artifact an opponent controls."
    power = 2
    toughness = 2

    activatedAbility {
        cost = Costs.Tap
        val artifact = target(TargetFilter.Artifact.opponentControls())
        effect = Effects.May(
            Effects.Modal(
                modes = listOf(
                    Mode.noTarget(Effects.Tap(artifact), "Tap that artifact"),
                    Mode.noTarget(Effects.Untap(artifact), "Untap that artifact")
                ),
                chooseCount = 1,
                countsAsModalSpell = false
            )
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "152"
        artist = "Dan Frazier"
        flavorText = "\"The smith a mighty man is he/ With large and sinewy hands./ And the muscles of his brawny arms/ Are strong as iron bands.\"\n—Henry Wadsworth Longfellow, The Village Blacksmith"
        imageUri = "https://cards.scryfall.io/normal/front/4/4/44d499a9-fe7c-4a1a-9eb3-a7fd9f85ae08.jpg?1783948056"
    }
}
