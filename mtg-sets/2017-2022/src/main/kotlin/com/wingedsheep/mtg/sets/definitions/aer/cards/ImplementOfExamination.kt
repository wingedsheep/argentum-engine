package com.wingedsheep.mtg.sets.definitions.aer.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Implement of Examination
 * {3}
 * Artifact
 * {U}, Sacrifice this artifact: Draw a card.
 * When this artifact is put into a graveyard from the battlefield, draw a card.
 *
 * The Implement cycle: sacrificing it pays the first ability's cost, so the
 * `Triggers.self.dies()` draw (battlefield-to-graveyard, not creature-only) resolves first.
 */
val ImplementOfExamination = card("Implement of Examination") {
    manaCost = "{3}"
    colorIdentity = "U"
    typeLine = "Artifact"
    oracleText = "{U}, Sacrifice this artifact: Draw a card.\n" +
        "When this artifact is put into a graveyard from the battlefield, draw a card."

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{U}"), Costs.SacrificeSelf)
        effect = Effects.DrawCards(1)
    }

    triggeredAbility {
        trigger = Triggers.self.dies()
        effect = Effects.DrawCards(1)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "156"
        artist = "Darek Zabrocki"
        flavorText = "A tool to see the unseen."
        imageUri = "https://cards.scryfall.io/normal/front/a/7/a791aa71-00db-422d-a78a-53b121c24db5.jpg?1783936725"
        ruling("2017-02-09", "An Implement's last ability triggers no matter why it's put into a graveyard from the battlefield. You don't have to activate its first ability.")
        ruling("2017-02-09", "If you activate an Implement's first ability, you'll draw a card from its second ability before the first ability resolves. If the first ability has a target, you need a legal target to activate that ability, and you choose that target before seeing what card you'll draw.")
    }
}
