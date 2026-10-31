package com.wingedsheep.mtg.sets.definitions.eld.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantAttackUnless
import com.wingedsheep.sdk.scripting.CantBlockUnless
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Vantress Gargoyle
 * {1}{U}
 * Artifact Creature — Gargoyle
 * 5/4
 * Flying
 * This creature can't attack unless defending player has seven or more cards in their graveyard.
 * This creature can't block unless you have four or more cards in hand.
 * {T}: Each player mills a card.
 *
 * `CantAttackUnless` binds the player each attacker is declared against as `Player.DefendingPlayer`
 * (the planeswalker's controller when attacking a planeswalker), and both restrictions are checked
 * only at declaration, matching the 2019-10-04 rulings.
 */
val VantressGargoyle = card("Vantress Gargoyle") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Artifact Creature — Gargoyle"
    power = 5
    toughness = 4
    oracleText = "Flying\nThis creature can't attack unless defending player has seven or more cards in their graveyard.\nThis creature can't block unless you have four or more cards in hand.\n{T}: Each player mills a card."

    keywords(Keyword.FLYING)

    staticAbility {
        ability = CantAttackUnless(
            Conditions.CompareAmounts(
                DynamicAmounts.zone(Player.DefendingPlayer, Zone.GRAVEYARD).count(),
                ComparisonOperator.GTE,
                7
            )
        )
    }

    staticAbility {
        ability = CantBlockUnless(Conditions.CardsInHandAtLeast(4))
    }

    activatedAbility {
        cost = Costs.Tap
        effect = Patterns.Library.mill(1, EffectTarget.PlayerRef(Player.Each))
        description = "{T}: Each player mills a card."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "71"
        artist = "Cristi Balanescu"
        imageUri = "https://cards.scryfall.io/normal/front/d/a/daff1c8d-0f25-4bec-bd50-208ae2ac0aac.jpg?1783932649"
        ruling("2019-10-04", "If Vantress Gargoyle attacks a planeswalker, the controller of that planeswalker is the defending player.")
        ruling("2019-10-04", "Vantress Gargoyle's attacking and blocking restrictions are checked only at the moment that you declare it as an attacking or blocking creature. If the defending player loses cards from their graveyard after Vantress Gargoyle attacks, or if you lose cards from your hand after it blocks, it remains in combat.")
    }
}
