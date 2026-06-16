package com.wingedsheep.mtg.sets.definitions.otj.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.effects.MakePlottedEffect
import com.wingedsheep.sdk.scripting.effects.MoveCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectFromCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectionMode
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Jace Reawakened
 * {U}{U}
 * Legendary Planeswalker — Jace
 * Starting Loyalty: 3
 *
 * You can't cast Jace Reawakened during your first, second, or third turns of the game.
 * +1: Draw a card, then discard a card.
 * +1: You may exile a nonland card with mana value 3 or less from your hand. If you do, it becomes plotted.
 * −6: Until end of turn, whenever you cast a spell, copy it. You may choose new targets for the copy.
 *
 * The cast restriction is expressed as `castOnlyIf(NOT controller-turns-taken ≤ 3)` — i.e. it can
 * only be cast once the controller has taken at least four turns. The +1 plot mode reuses the OTJ
 * gather → choose-up-to-one (nonland, MV ≤ 3) → exile → [MakePlottedEffect] pipeline (CR 718); the
 * `ChooseUpTo(1)` over the filtered hand is the "you may" fork, and `MakePlottedEffect` no-ops on an
 * empty selection so declining is safe. The −6 ultimate installs a copy-each-spell-cast effect over
 * *any* spell (not just instants/sorceries) for the turn; the engine's copy creation already offers
 * "you may choose new targets for the copy."
 */
val JaceReawakened = card("Jace Reawakened") {
    manaCost = "{U}{U}"
    colorIdentity = "U"
    typeLine = "Legendary Planeswalker — Jace"
    startingLoyalty = 3
    oracleText = "You can't cast Jace Reawakened during your first, second, or third turns of the game.\n" +
        "+1: Draw a card, then discard a card.\n" +
        "+1: You may exile a nonland card with mana value 3 or less from your hand. If you do, it becomes plotted.\n" +
        "−6: Until end of turn, whenever you cast a spell, copy it. You may choose new targets for the copy."

    // "You can't cast Jace Reawakened during your first, second, or third turns of the game."
    spell {
        castOnlyIf(Conditions.Not(Conditions.ControllerTurnsTakenAtMost(3)))
    }

    // +1: Draw a card, then discard a card.
    loyaltyAbility(+1) {
        effect = Effects.DrawCards(1).then(Patterns.Hand.discardCards(1))
    }

    // +1: You may exile a nonland card with mana value 3 or less from your hand. If you do, it becomes plotted.
    loyaltyAbility(+1) {
        effect = Effects.Composite(
            GatherCardsEffect(
                CardSource.FromZone(Zone.HAND, filter = GameObjectFilter.Nonland.manaValueAtMost(3)),
                storeAs = "handCards",
            ),
            SelectFromCollectionEffect(
                from = "handCards",
                selection = SelectionMode.ChooseUpTo(DynamicAmount.Fixed(1)),
                filter = GameObjectFilter.Nonland.manaValueAtMost(3),
                storeSelected = "toPlot",
                selectedLabel = "Exile and plot",
            ),
            MoveCollectionEffect(from = "toPlot", destination = CardDestination.ToZone(Zone.EXILE)),
            MakePlottedEffect(from = "toPlot"),
        )
    }

    // −6: Until end of turn, whenever you cast a spell, copy it. You may choose new targets for the copy.
    loyaltyAbility(-6) {
        effect = Effects.CopyEachSpellCast(copies = 1, spellFilter = GameObjectFilter.Any)
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "271"
        artist = "Cristi Balanescu"
        imageUri = "https://cards.scryfall.io/normal/front/f/d/fd17e8d4-499e-4005-ae3c-bc9c44dc5a67.jpg?1712356388"

        ruling("2024-04-12", "Jace Reawakened's last ability copies any spell you cast, not just instant and sorcery spells. The copy is created on the stack, so it's not \"cast.\"")
        ruling("2024-04-12", "A plotted card is exiled face up. You may cast it as a sorcery on a later turn without paying its mana cost, but not on the turn it became plotted.")
    }
}
