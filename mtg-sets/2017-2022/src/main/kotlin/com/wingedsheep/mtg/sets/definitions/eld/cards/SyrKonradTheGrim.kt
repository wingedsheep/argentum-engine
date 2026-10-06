package com.wingedsheep.mtg.sets.definitions.eld.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Syr Konrad, the Grim
 * {3}{B}{B}
 * Legendary Creature — Human Knight
 * 5/4
 * Whenever another creature dies, or a creature card is put into a graveyard from anywhere other
 * than the battlefield, or a creature card leaves your graveyard, Syr Konrad deals 1 damage to
 * each opponent.
 * {1}{B}: Each player mills a card.
 *
 * Modeling notes:
 *  - The three trigger conditions share one effect but not one binding: "another creature dies"
 *    excludes Syr Konrad itself (OTHER), while the two card-movement clauses watch every card
 *    (ANY). So the printed ability is split into two triggered abilities with the same effect.
 *    The clauses are disjoint (a death is from the battlefield; the second clause excludes the
 *    battlefield; the third starts in a graveyard), so no single event fires both.
 *  - "From anywhere other than the battlefield" has no exclude-from axis, so it is spelled as the
 *    union of every other zone a card can reach a graveyard from: hand, library, stack, exile,
 *    and the command zone.
 *  - "Leaves your graveyard" is `changesZone(from = GRAVEYARD)` with any destination, keyed on
 *    `.ownedByYou()`: a card is only ever in its owner's graveyard (CR 400.3), whereas control
 *    would follow a reanimated card to whoever returned it.
 */
val SyrKonradTheGrim = card("Syr Konrad, the Grim") {
    manaCost = "{3}{B}{B}"
    colorIdentity = "B"
    typeLine = "Legendary Creature — Human Knight"
    oracleText = "Whenever another creature dies, or a creature card is put into a graveyard from " +
        "anywhere other than the battlefield, or a creature card leaves your graveyard, Syr Konrad " +
        "deals 1 damage to each opponent.\n" +
        "{1}{B}: Each player mills a card. (They each put the top card of their library into their graveyard.)"
    power = 5
    toughness = 4

    triggeredAbility {
        trigger = Triggers.another(GameObjectFilter.Creature).dies()
        effect = Effects.DealDamage(1, EffectTarget.PlayerRef(Player.EachOpponent))
        description = "Syr Konrad deals 1 damage to each opponent."
    }

    triggeredAbility {
        val creatureCard = Triggers.a(GameObjectFilter.Creature)
        trigger = Triggers.or(
            creatureCard.changesZone(from = Zone.HAND, to = Zone.GRAVEYARD),
            creatureCard.changesZone(from = Zone.LIBRARY, to = Zone.GRAVEYARD),
            creatureCard.changesZone(from = Zone.STACK, to = Zone.GRAVEYARD),
            creatureCard.changesZone(from = Zone.EXILE, to = Zone.GRAVEYARD),
            creatureCard.changesZone(from = Zone.COMMAND, to = Zone.GRAVEYARD),
            Triggers.a(GameObjectFilter.Creature.ownedByYou()).changesZone(from = Zone.GRAVEYARD),
        )
        effect = Effects.DealDamage(1, EffectTarget.PlayerRef(Player.EachOpponent))
        description = "Syr Konrad deals 1 damage to each opponent."
    }

    activatedAbility {
        cost = Costs.Mana("{1}{B}")
        effect = Patterns.Library.mill(1, EffectTarget.PlayerRef(Player.Each))
        description = "Each player mills a card."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "107"
        artist = "Anna Steinbauer"
        imageUri = "https://cards.scryfall.io/normal/front/a/8/a808868f-aea8-4651-9357-85a4d7b4f290.jpg?1783932632"
        ruling("2019-10-04", "If one or more creatures die at the same time as Syr Konrad, its first ability triggers for each of those creatures.")
        ruling("2019-10-04", "In a Two-Headed Giant game, Syr Konrad's first ability causes it to deal 1 damage twice.")
    }
}
