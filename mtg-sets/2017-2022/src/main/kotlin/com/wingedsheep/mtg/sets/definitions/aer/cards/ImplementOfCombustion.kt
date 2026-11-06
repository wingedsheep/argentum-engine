package com.wingedsheep.mtg.sets.definitions.aer.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Implement of Combustion
 * {1}
 * Artifact
 * {R}, Sacrifice this artifact: It deals 1 damage to target player or planeswalker.
 * When this artifact is put into a graveyard from the battlefield, draw a card.
 *
 * The Implement cycle: sacrificing it pays the first ability's cost, so the
 * `Triggers.self.dies()` draw (battlefield-to-graveyard, not creature-only) resolves first.
 */
val ImplementOfCombustion = card("Implement of Combustion") {
    manaCost = "{1}"
    colorIdentity = "R"
    typeLine = "Artifact"
    oracleText = "{R}, Sacrifice this artifact: It deals 1 damage to target player or planeswalker.\n" +
        "When this artifact is put into a graveyard from the battlefield, draw a card."

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{R}"), Costs.SacrificeSelf)
        val victim = target(Targets.PlayerOrPlaneswalker)
        effect = Effects.DealDamage(1, victim)
    }

    triggeredAbility {
        trigger = Triggers.self.dies()
        effect = Effects.DrawCards(1)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "155"
        artist = "Enora Mercier"
        flavorText = "A dream of fire lies coiled inside, waiting for its glorious awakening."
        imageUri = "https://cards.scryfall.io/normal/front/1/5/15b1c8da-604a-42f0-8658-82580700dd31.jpg?1783936726"
        ruling("2017-02-09", "An Implement's last ability triggers no matter why it's put into a graveyard from the battlefield. You don't have to activate its first ability.")
        ruling("2017-02-09", "If you activate an Implement's first ability, you'll draw a card from its second ability before the first ability resolves. If the first ability has a target, you need a legal target to activate that ability, and you choose that target before seeing what card you'll draw.")
    }
}
