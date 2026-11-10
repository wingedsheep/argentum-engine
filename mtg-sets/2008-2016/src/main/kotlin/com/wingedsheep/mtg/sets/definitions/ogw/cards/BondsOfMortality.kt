package com.wingedsheep.mtg.sets.definitions.ogw.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Bonds of Mortality — Oath of the Gatewatch #128 (canonical printing)
 * {1}{G} · Enchantment
 *
 * When this enchantment enters, draw a card.
 * {G}: Creatures your opponents control lose hexproof and indestructible until end of turn.
 *
 * Shadowspear's hexproof/indestructible strip narrowed to creatures: [Patterns.Group.removeKeywordFromAll]
 * snapshots the opponents' creatures as the ability resolves, so a creature that enters or gains either
 * keyword afterwards keeps it (ruling).
 */
val BondsOfMortality = card("Bonds of Mortality") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Enchantment"
    oracleText = "When this enchantment enters, draw a card.\n" +
        "{G}: Creatures your opponents control lose hexproof and indestructible until end of turn."

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.DrawCards(1)
        description = "When this enchantment enters, draw a card."
    }

    activatedAbility {
        cost = Costs.Mana("{G}")
        effect = Patterns.Group.removeKeywordFromAll(
            Keyword.HEXPROOF,
            Filters.Group.creaturesOpponentsControl,
        ) then
            Patterns.Group.removeKeywordFromAll(
                Keyword.INDESTRUCTIBLE,
                Filters.Group.creaturesOpponentsControl,
            )
        description = "{G}: Creatures your opponents control lose hexproof and indestructible until end of turn."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "128"
        artist = "Chris Rallis"
        flavorText = "Jace discovered the leyline pattern needed to anchor Ulamog and Kozilek to reality, " +
            "but it was Nissa who could trace it on her world."
        imageUri = "https://cards.scryfall.io/normal/front/f/f/ff50de00-115f-41ed-892b-aac9bd13b9b9.jpg?1783937902"
        ruling(
            "2016-01-22",
            "The last ability causes creatures your opponents control to lose hexproof and indestructible as it " +
                "resolves. Effects that cause those creatures to gain those abilities after that point will work normally.",
        )
        ruling(
            "2016-01-22",
            "Damage dealt to a creature with indestructible is still marked on that creature. If that creature loses " +
                "indestructible, and the marked damage is lethal, it will be destroyed. However, if a creature with " +
                "indestructible is dealt damage by a source with deathtouch and then later loses indestructible, that " +
                "creature won't be destroyed.",
        )
    }
}
