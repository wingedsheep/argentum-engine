package com.wingedsheep.mtg.sets.definitions.ltr.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Witch-king of Angmar
 * {3}{B}{B}
 * Legendary Creature — Wraith Noble
 * 5/3
 *
 * Flying
 * Whenever one or more creatures deal combat damage to you, each opponent sacrifices a creature of
 * their choice that dealt combat damage to you this turn. The Ring tempts you.
 * Discard a card: Witch-king of Angmar gains indestructible until end of turn. Tap it.
 *
 * The triggered ability uses the defensive combat-damage batch trigger
 * `Triggers.OneOrMoreCreaturesDealCombatDamageToYou()` (fires once per combat regardless of how
 * many creatures connected). The edict restricts each opponent's sacrifice to creatures matching
 * the new source-relative filter `dealtCombatDamageToSourceControllerThisTurn()` — i.e. creatures
 * that dealt combat damage to the Witch-king's controller this turn.
 */
val WitchKingOfAngmar = card("Witch-king of Angmar") {
    manaCost = "{3}{B}{B}"
    colorIdentity = "B"
    typeLine = "Legendary Creature — Wraith Noble"
    power = 5
    toughness = 3
    oracleText = "Flying\n" +
        "Whenever one or more creatures deal combat damage to you, each opponent sacrifices a " +
        "creature of their choice that dealt combat damage to you this turn. The Ring tempts you.\n" +
        "Discard a card: Witch-king of Angmar gains indestructible until end of turn. Tap it."

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.OneOrMoreCreaturesDealCombatDamageToYou()
        effect = Effects.Sacrifice(
            filter = GameObjectFilter.Creature.dealtCombatDamageToSourceControllerThisTurn(),
            target = EffectTarget.PlayerRef(Player.EachOpponent)
        ).then(Effects.TheRingTemptsYou())
    }

    activatedAbility {
        cost = Costs.DiscardCard
        effect = Effects.GrantKeyword(Keyword.INDESTRUCTIBLE, EffectTarget.Self, Duration.EndOfTurn)
            .then(Effects.Tap(EffectTarget.Self))
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "114"
        artist = "Anato Finnstark"
        imageUri = "https://cards.scryfall.io/normal/front/a/5/a55e6508-0b59-4573-bc4e-67b27279cfed.jpg?1686968786"
    }
}
