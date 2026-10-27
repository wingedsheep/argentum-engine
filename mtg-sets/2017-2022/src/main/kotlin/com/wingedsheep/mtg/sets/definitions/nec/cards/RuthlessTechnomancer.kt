package com.wingedsheep.mtg.sets.definitions.nec.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.values.CardNumericProperty

/**
 * Ruthless Technomancer
 * {3}{B}
 * Creature — Human Wizard
 * 2/4
 *
 * When this creature enters, you may sacrifice another creature you control. If you do, create a
 * number of Treasure tokens equal to that creature's power.
 * {2}{B}, Sacrifice X artifacts: Return target creature card with power X or less from your
 * graveyard to the battlefield. X can't be 0.
 *
 * The enter trigger is the Disciple of Freyalise shape: a resolution-time "up to one other creature"
 * choice, sacrificed, with "if you do" gating the Treasures on a non-empty choice and the count read
 * from the sacrificed creature's last-known power ([DynamicAmounts.sacrificedPower]).
 *
 * The activated ability is the Fabrication Foundry shape: a variable-count sacrifice cost whose
 * count is X (`minCount = 1` — "X can't be 0"), with the graveyard target bounded by that X.
 */
val RuthlessTechnomancer = card("Ruthless Technomancer") {
    manaCost = "{3}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Human Wizard"
    power = 2
    toughness = 4
    oracleText = "When this creature enters, you may sacrifice another creature you control. If you do, " +
        "create a number of Treasure tokens equal to that creature's power.\n" +
        "{2}{B}, Sacrifice X artifacts: Return target creature card with power X or less from your " +
        "graveyard to the battlefield. X can't be 0."

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.Pipeline {
            val others = gather(
                CardSource.BattlefieldMatching(
                    filter = GameObjectFilter.Creature,
                    player = Player.You,
                    excludeSelf = true,
                )
            )
            val chosen = chooseUpTo(
                1,
                from = others,
                useTargetingUI = true,
                prompt = "You may sacrifice another creature you control",
                selectedLabel = "Sacrifice",
            )
            ifNotEmpty(chosen) {
                sacrifice(chosen)
                run(Effects.CreateTreasure(DynamicAmounts.sacrificedPower()))
            }
        }
        description = "When this creature enters, you may sacrifice another creature you control. If you " +
            "do, create a number of Treasure tokens equal to that creature's power."
    }

    activatedAbility {
        cost = Costs.Composite(
            Costs.Mana("{2}{B}"),
            Costs.SacrificePermanents(GameObjectFilter.Artifact, minCount = 1),
        )
        val creature = target(
            TargetFilter(
                baseFilter = GameObjectFilter.Creature.ownedByYou().compareNumericProperty(
                    CardNumericProperty.POWER,
                    ComparisonOperator.LTE,
                    DynamicAmounts.xValue(),
                ),
                zone = Zone.GRAVEYARD,
            ),
        )
        effect = Effects.PutOntoBattlefieldFromGraveyard(creature)
        description = "{2}{B}, Sacrifice X artifacts: Return target creature card with power X or less " +
            "from your graveyard to the battlefield. X can't be 0."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "35"
        artist = "PINDURSKI"
        imageUri = "https://cards.scryfall.io/normal/front/d/d/dddeb5cc-b535-4484-8a0e-ce32c4c09000.jpg?1783923986"
    }
}
