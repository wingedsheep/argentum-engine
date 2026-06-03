package com.wingedsheep.mtg.sets.definitions.eoe.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersWithDynamicCounters
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Bioengineered Future {1}{G}{G}
 * Enchantment
 *
 * When this enchantment enters, create a Lander token. (It's an artifact with "{2}, {T},
 * Sacrifice this token: Search your library for a basic land card, put it onto the
 * battlefield tapped, then shuffle.")
 * Each creature you control enters with an additional +1/+1 counter on it for each land
 * that entered the battlefield under your control this turn.
 *
 * The second ability is an `EntersWithDynamicCounters(otherOnly = true)` whose count reads
 * the per-player `LANDS_ENTERED_UNDER_CONTROL` turn tracker. Per the Scryfall ruling, lands
 * that entered the battlefield under the controller's control *before* Bioengineered Future
 * was on the battlefield still count — the tracker has been accumulating from the start of
 * the turn, regardless of when Bioengineered Future itself entered.
 */
val BioengineeredFuture = card("Bioengineered Future") {
    manaCost = "{1}{G}{G}"
    colorIdentity = "G"
    typeLine = "Enchantment"
    oracleText = "When this enchantment enters, create a Lander token. (It's an artifact with " +
        "\"{2}, {T}, Sacrifice this token: Search your library for a basic land card, put it " +
        "onto the battlefield tapped, then shuffle.\")\n" +
        "Each creature you control enters with an additional +1/+1 counter on it for each " +
        "land that entered the battlefield under your control this turn."

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = Effects.CreateLander()
    }

    replacementEffect(
        EntersWithDynamicCounters(
            count = DynamicAmounts.landsEnteredUnderControlThisTurn(),
            otherOnly = true,
            appliesTo = EventPattern.ZoneChangeEvent(
                filter = GameObjectFilter.Creature.youControl(),
                to = Zone.BATTLEFIELD,
            ),
        )
    )

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "172"
        artist = "Constantin Marin"
        imageUri = "https://cards.scryfall.io/normal/front/8/0/800ee479-c1dc-4dd0-9b98-436c78997958.jpg?1758204025"
        ruling(
            "2025-07-25",
            "Bioengineered Future's last ability will consider lands that entered the battlefield " +
                "under your control before it was on the battlefield. For example, if two lands " +
                "entered the battlefield under your control this turn before Bioengineered Future " +
                "entered, creatures you control will enter with two +1/+1 counters on them this turn."
        )
        ruling(
            "2025-07-25",
            "In the unlikely event that Bioengineered Future and one or more creatures you " +
                "control enter at the same time, Bioengineered Future's last ability won't cause " +
                "those creatures to enter with additional +1/+1 counters on them."
        )
    }
}
