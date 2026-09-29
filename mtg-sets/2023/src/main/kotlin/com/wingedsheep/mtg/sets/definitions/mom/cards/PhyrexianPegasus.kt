package com.wingedsheep.mtg.sets.definitions.mom.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Phyrexian Pegasus — March of the Machine #324 (Jumpstart-only card, its sole printing)
 * {2}{W} · Creature — Phyrexian Pegasus · 2/2
 *
 * Flying
 * Whenever this creature attacks, another target attacking creature without flying gains flying
 * until end of turn.
 */
val PhyrexianPegasus = card("Phyrexian Pegasus") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Phyrexian Pegasus"
    power = 2
    toughness = 2
    oracleText = "Flying\n" +
        "Whenever this creature attacks, another target attacking creature without flying gains " +
        "flying until end of turn."

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.self.attacks()
        val attacker = target(TargetFilter.Creature.attacking().withoutKeyword(Keyword.FLYING).other())
        effect = Effects.GrantKeyword(Keyword.FLYING, attacker)
        description = "Whenever this creature attacks, another target attacking creature without " +
            "flying gains flying until end of turn."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "324"
        artist = "Carlos Palma Cruchaga"
        flavorText = "\"It is glory, exalted and *compleat*.\"\n—Cymede, regent of the Alabaster Host"
        imageUri = "https://cards.scryfall.io/normal/front/a/a/aa1200a1-8671-464f-ab3b-bfc1fb0d6ed8.jpg?1783916907"
    }
}
