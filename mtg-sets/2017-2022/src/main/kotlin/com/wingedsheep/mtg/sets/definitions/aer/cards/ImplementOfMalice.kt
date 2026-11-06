package com.wingedsheep.mtg.sets.definitions.aer.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.TimingRule

/**
 * Implement of Malice
 * {2}
 * Artifact
 * {B}, Sacrifice this artifact: Target player discards a card. Activate only as a sorcery.
 * When this artifact is put into a graveyard from the battlefield, draw a card.
 *
 * The Implement cycle: sacrificing it pays the first ability's cost, so the
 * `Triggers.self.dies()` draw (battlefield-to-graveyard, not creature-only) resolves first.
 */
val ImplementOfMalice = card("Implement of Malice") {
    manaCost = "{2}"
    colorIdentity = "B"
    typeLine = "Artifact"
    oracleText = "{B}, Sacrifice this artifact: Target player discards a card. Activate only as a sorcery.\n" +
        "When this artifact is put into a graveyard from the battlefield, draw a card."

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{B}"), Costs.SacrificeSelf)
        timing = TimingRule.SorcerySpeed
        val player = target(Targets.Player)
        effect = Effects.Discard(1, player)
    }

    triggeredAbility {
        trigger = Triggers.self.dies()
        effect = Effects.DrawCards(1)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "159"
        artist = "Titus Lunter"
        imageUri = "https://cards.scryfall.io/normal/front/2/e/2e45ca82-e387-4df0-890d-d53a7d0dadf7.jpg?1783936726"
        ruling("2017-02-09", "An Implement's last ability triggers no matter why it's put into a graveyard from the battlefield. You don't have to activate its first ability.")
        ruling("2017-02-09", "If you activate an Implement's first ability, you'll draw a card from its second ability before the first ability resolves. If the first ability has a target, you need a legal target to activate that ability, and you choose that target before seeing what card you'll draw.")
    }
}
