package com.wingedsheep.mtg.sets.definitions.aer.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Implement of Ferocity
 * {1}
 * Artifact
 * {G}, Sacrifice this artifact: Put a +1/+1 counter on target creature. Activate only as a sorcery.
 * When this artifact is put into a graveyard from the battlefield, draw a card.
 *
 * The Implement cycle: sacrificing it pays the first ability's cost, so the
 * `Triggers.self.dies()` draw (battlefield-to-graveyard, not creature-only) resolves first.
 */
val ImplementOfFerocity = card("Implement of Ferocity") {
    manaCost = "{1}"
    colorIdentity = "G"
    typeLine = "Artifact"
    oracleText = "{G}, Sacrifice this artifact: Put a +1/+1 counter on target creature. Activate only as a sorcery.\n" +
        "When this artifact is put into a graveyard from the battlefield, draw a card."

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{G}"), Costs.SacrificeSelf)
        timing = TimingRule.SorcerySpeed
        val creature = target(TargetFilter.Creature)
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, creature)
    }

    triggeredAbility {
        trigger = Triggers.self.dies()
        effect = Effects.DrawCards(1)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "157"
        artist = "Jason Kang"
        imageUri = "https://cards.scryfall.io/normal/front/b/c/bcdee084-0d1a-486a-8361-680d394a5e23.jpg?1783936726"
        ruling("2017-02-09", "An Implement's last ability triggers no matter why it's put into a graveyard from the battlefield. You don't have to activate its first ability.")
        ruling("2017-02-09", "If you activate an Implement's first ability, you'll draw a card from its second ability before the first ability resolves. If the first ability has a target, you need a legal target to activate that ability, and you choose that target before seeing what card you'll draw.")
    }
}
