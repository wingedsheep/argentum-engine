package com.wingedsheep.mtg.sets.definitions.ogw.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.SuccessCriterion
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Harvester Troll — Oath of the Gatewatch #133 (canonical printing)
 * {3}{G} · Creature — Troll · 2/3
 *
 * When this creature enters, you may sacrifice a creature or land. If you do, put two +1/+1
 * counters on this creature.
 *
 * The sacrifice is chosen on resolution (see ruling) and isn't "another", so the Troll may
 * sacrifice itself — then the counters have no object to land on, since `Self` is lost once
 * the source leaves the battlefield. Chitterspitter's May + IfYouDo(PermanentsSacrificed) shape.
 */
val HarvesterTroll = card("Harvester Troll") {
    manaCost = "{3}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Troll"
    power = 2
    toughness = 3
    oracleText = "When this creature enters, you may sacrifice a creature or land. If you do, put two " +
        "+1/+1 counters on this creature."

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.May(
            Effects.IfYouDo(
                action = Effects.SacrificeOwn(GameObjectFilter.CreatureOrLand),
                then = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 2, EffectTarget.Self),
                successCriterion = SuccessCriterion.PermanentsSacrificed
            )
        )
        description = "When this creature enters, you may sacrifice a creature or land. If you do, put " +
            "two +1/+1 counters on this creature."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "133"
        artist = "Greg Staples"
        flavorText = "Having no interest in the greater conflict, the trolls continue to prey upon " +
            "whatever wanders into their domain."
        imageUri = "https://cards.scryfall.io/normal/front/0/b/0bf92744-0acc-4c7e-819c-51b0696facad.jpg?1783937901"
        ruling(
            "2016-01-22",
            "Players can respond to the enters-the-battlefield ability (notably, while Harvester Troll is " +
                "still 2/3), but once that ability starts resolving, it's too late for any player to " +
                "respond. You don't choose which creature or land you're sacrificing, if any, until the " +
                "ability resolves."
        )
    }
}
