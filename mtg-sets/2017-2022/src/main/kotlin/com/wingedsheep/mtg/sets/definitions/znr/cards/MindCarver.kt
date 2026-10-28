package com.wingedsheep.mtg.sets.definitions.znr.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ConditionalStaticAbility
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Mind Carver — Zendikar Rising #113
 * {B} · Artifact — Equipment
 *
 * When this Equipment enters, attach it to target creature you control.
 * Equipped creature gets +1/+0. It gets +3/+1 instead as long as an opponent has eight or more
 * cards in their graveyard.
 * Equip {2}{B}
 *
 * "Instead" is written as two mutually exclusive conditional bonuses, so exactly one applies at a
 * time. "An opponent has eight or more" is Expedition Lookout's per-opponent test: the greatest
 * graveyard among the Equipment controller's opponents, not a sum across them.
 */
private val OpponentHasEightInGraveyard = Conditions.CompareAmounts(
    DynamicAmounts.greatestAmongPlayers(
        DynamicAmounts.count(Player.You, Zone.GRAVEYARD),
        Player.EachOpponent
    ),
    ComparisonOperator.GTE,
    8
)

val MindCarver = card("Mind Carver") {
    manaCost = "{B}"
    colorIdentity = "B"
    typeLine = "Artifact — Equipment"
    oracleText = "When this Equipment enters, attach it to target creature you control.\n" +
        "Equipped creature gets +1/+0. It gets +3/+1 instead as long as an opponent has eight or more " +
        "cards in their graveyard.\n" +
        "Equip {2}{B} ({2}{B}: Attach to target creature you control. Equip only as a sorcery.)"

    triggeredAbility {
        trigger = Triggers.self.enters()
        val t = target(TargetFilter.Creature.youControl())
        effect = Effects.AttachEquipment(t)
    }

    staticAbility {
        ability = ConditionalStaticAbility(
            ability = ModifyStats(1, 0),
            condition = Conditions.Not(OpponentHasEightInGraveyard)
        )
    }

    staticAbility {
        ability = ConditionalStaticAbility(
            ability = ModifyStats(3, 1),
            condition = OpponentHasEightInGraveyard
        )
    }

    equipAbility("{2}{B}")

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "113"
        artist = "Scott Murphy"
        imageUri = "https://cards.scryfall.io/normal/front/1/0/105b6e18-bcd8-43b7-870d-6c7e5c06f99b.jpg?1783929373"
        ruling(
            "2020-09-25",
            "An ability that offers a bonus if an opponent has eight or more cards in their graveyard " +
                "won't provide additional benefits if more than one opponent has eight or more cards in " +
                "their graveyard. There are also no additional benefits no matter how many cards are in " +
                "an opponent's graveyard, as long as there are at least eight."
        )
        ruling(
            "2020-09-25",
            "Attaching an Equipment with its enters-the-battlefield triggered ability isn't the same as " +
                "using its equip ability. You don't pay mana for the attachment, and if the Equipment " +
                "enters at a time you couldn't cast a sorcery, you can still attach it to a creature you control."
        )
        ruling(
            "2020-09-25",
            "If the target creature becomes an illegal target, the Equipment remains on the battlefield unattached."
        )
    }
}
