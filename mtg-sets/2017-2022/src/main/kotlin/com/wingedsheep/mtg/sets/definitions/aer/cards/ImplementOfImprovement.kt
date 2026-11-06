package com.wingedsheep.mtg.sets.definitions.aer.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Implement of Improvement
 * {1}
 * Artifact
 * {W}, Sacrifice this artifact: You gain 2 life.
 * When this artifact is put into a graveyard from the battlefield, draw a card.
 *
 * The Implement cycle: sacrificing it pays the first ability's cost, so the
 * `Triggers.self.dies()` draw (battlefield-to-graveyard, not creature-only) resolves first.
 */
val ImplementOfImprovement = card("Implement of Improvement") {
    manaCost = "{1}"
    colorIdentity = "W"
    typeLine = "Artifact"
    oracleText = "{W}, Sacrifice this artifact: You gain 2 life.\n" +
        "When this artifact is put into a graveyard from the battlefield, draw a card."

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{W}"), Costs.SacrificeSelf)
        effect = Effects.GainLife(2)
    }

    triggeredAbility {
        trigger = Triggers.self.dies()
        effect = Effects.DrawCards(1)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "158"
        artist = "Kieran Yanner"
        flavorText = "A mystery in metal."
        imageUri = "https://cards.scryfall.io/normal/front/2/c/2cb8eb0c-d7fa-4229-ae65-1890c77b2c7c.jpg?1783936725"
        ruling("2017-02-09", "An Implement's last ability triggers no matter why it's put into a graveyard from the battlefield. You don't have to activate its first ability.")
        ruling("2017-02-09", "If you activate an Implement's first ability, you'll draw a card from its second ability before the first ability resolves. If the first ability has a target, you need a legal target to activate that ability, and you choose that target before seeing what card you'll draw.")
    }
}
