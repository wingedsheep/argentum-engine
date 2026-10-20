package com.wingedsheep.mtg.sets.definitions.leg.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Voodoo Doll
 * {6}
 * Artifact
 * At the beginning of your upkeep, put a pin counter on this artifact.
 * At the beginning of your end step, if this artifact is untapped, destroy this artifact and it
 * deals damage to you equal to the number of pin counters on it.
 * {X}{X}, {T}: This artifact deals damage equal to the number of pin counters on it to any target.
 * X is the number of pin counters on this artifact.
 *
 * The end-step check is an intervening-if; if the Doll has left the battlefield by resolution, it
 * is answered from how the Doll last existed there (its last-known tap state), and the damage
 * reads its last-known pin counters. The cost's X is defined, not chosen (`xDefinedAs`), fixed as
 * the ability is activated; the damage counts pin counters again as it resolves.
 */
val VoodooDoll = card("Voodoo Doll") {
    manaCost = "{6}"
    typeLine = "Artifact"
    oracleText = "At the beginning of your upkeep, put a pin counter on this artifact.\n" +
        "At the beginning of your end step, if this artifact is untapped, destroy this artifact and it deals " +
        "damage to you equal to the number of pin counters on it.\n" +
        "{X}{X}, {T}: This artifact deals damage equal to the number of pin counters on it to any target. " +
        "X is the number of pin counters on this artifact."

    val pins = DynamicAmounts.countersOnSelf(CounterType.PIN)

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.UPKEEP)
        effect = Effects.AddCounters(CounterType.PIN, 1, EffectTarget.Self)
    }

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.END)
        interveningIf = Conditions.SourceIsUntapped
        effect = Effects.Destroy(EffectTarget.Self) then
            Effects.DealDamage(pins, EffectTarget.PlayerRef(Player.You))
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{X}{X}"), Costs.Tap)
        xDefinedAs = pins
        val t = target(Targets.Any)
        effect = Effects.DealDamage(pins, t)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "298"
        artist = "Sandra Everingham"
        imageUri = "https://cards.scryfall.io/normal/front/0/4/0418672d-056e-416d-91b4-8ee6e47201dc.jpg?1783948024"
        ruling("2011-01-01", "Voodoo Doll's second ability causes it to be destroyed regardless of whether it actually deals damage to you or not. Voodoo Doll is destroyed even if all the damage is prevented or redirected.")
        ruling("2009-10-01", "Voodoo Doll's second ability has an \"intervening 'if' clause.\" That means (1) the ability won't trigger at all unless Voodoo Doll is untapped, and (2) the ability will do nothing if Voodoo Doll is tapped by the time it resolves. If Voodoo Doll is no longer on the battlefield as the ability resolves, its last existence on the battlefield is checked to see if it was tapped or untapped.")
        ruling("2009-10-01", "Each X in the cost of Voodoo Doll's third ability is the number of pin counters on Voodoo Doll at the time you activate the ability. The X in the ability's effect is the number of pin counters on Voodoo Doll at the time it resolves, which may not be the same number (if a spell or ability put counters on it or removed counters from it in the meantime). If Voodoo Doll is no longer on the battlefield as the ability resolves, its last existence on the battlefield is checked to see how many pin counters were on it.")
    }
}
