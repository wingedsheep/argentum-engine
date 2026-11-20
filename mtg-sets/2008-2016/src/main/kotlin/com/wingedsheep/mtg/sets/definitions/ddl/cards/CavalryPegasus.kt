package com.wingedsheep.mtg.sets.definitions.ddl.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Cavalry Pegasus — Duel Decks: Heroes vs. Monsters #4 (canonical printing)
 * {1}{W}
 * Creature — Pegasus
 * 1/1
 * Flying
 * Whenever this creature attacks, each attacking Human gains flying until end of turn.
 *
 * Duel Decks: Heroes vs. Monsters (2013-09-06) predates Theros, so the canonical definition lives
 * here and THS carries a `Printing` row. The group is gathered as the trigger resolves (still in the
 * declare-attackers step, before blockers), and each Human keeps flying even if the Pegasus leaves.
 */
val CavalryPegasus = card("Cavalry Pegasus") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Pegasus"
    power = 1
    toughness = 1
    oracleText = "Flying\nWhenever this creature attacks, each attacking Human gains flying until end of turn."

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.self.attacks()
        effect = Patterns.Group.grantKeywordToAll(
            Keyword.FLYING,
            GroupFilter(GameObjectFilter.Creature.withSubtype("Human").attacking()),
        )
        description = "Whenever this creature attacks, each attacking Human gains flying until end of turn."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "4"
        artist = "Kev Walker"
        flavorText = "\"It is hope, hooved and winged.\"\n—Cymede, queen of Akros"
        imageUri = "https://cards.scryfall.io/normal/front/6/e/6ea1e03e-5f6d-4e52-9538-956963c2335c.jpg?1783939866"
        ruling("2013-09-15", "Once the triggered ability resolves, the attacking Humans will continue to have flying even if Cavalry Pegasus leaves the battlefield.")
        ruling("2013-09-15", "The attacking Humans gain flying before blockers are declared.")
    }
}
