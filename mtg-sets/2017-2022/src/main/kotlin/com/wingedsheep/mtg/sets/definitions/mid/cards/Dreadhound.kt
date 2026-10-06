package com.wingedsheep.mtg.sets.definitions.mid.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Dreadhound
 * {4}{B}{B}
 * Creature — Demon Dog
 * 6/6
 *
 * When this creature enters, mill three cards.
 * Whenever a creature dies or a creature card is put into a graveyard from a library, each
 * opponent loses 1 life.
 *
 * The second ability is one printed trigger with two event halves. No single zone change can be
 * both (a death leaves the battlefield, the other leaves a library), so it is authored as two
 * triggered abilities with the same payoff — each event still yields exactly one trigger. Neither
 * half is scoped by controller or owner: any creature dying, and any player's creature card
 * going from their library to their graveyard, counts. Each creature card moved is its own
 * trigger, so Dreadhound's own mill of two creature cards drains each opponent for 2.
 */
val Dreadhound = card("Dreadhound") {
    manaCost = "{4}{B}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Demon Dog"
    power = 6
    toughness = 6
    oracleText = "When this creature enters, mill three cards. (Put the top three cards of your " +
        "library into your graveyard.)\n" +
        "Whenever a creature dies or a creature card is put into a graveyard from a library, " +
        "each opponent loses 1 life."

    // When this creature enters, mill three cards.
    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Patterns.Library.mill(3)
    }

    // Whenever a creature dies ... each opponent loses 1 life.
    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Creature).dies()
        effect = Effects.LoseLife(1, EffectTarget.PlayerRef(Player.EachOpponent))
    }

    // ... or a creature card is put into a graveyard from a library, each opponent loses 1 life.
    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Creature).changesZone(from = Zone.LIBRARY, to = Zone.GRAVEYARD)
        effect = Effects.LoseLife(1, EffectTarget.PlayerRef(Player.EachOpponent))
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "97"
        artist = "Joe Slucher"
        flavorText = "It herds souls toward eternal torment."
        imageUri = "https://cards.scryfall.io/normal/front/2/7/27fd571b-8316-4c60-a042-8bcf4816b126.jpg?1783925620"
    }
}
