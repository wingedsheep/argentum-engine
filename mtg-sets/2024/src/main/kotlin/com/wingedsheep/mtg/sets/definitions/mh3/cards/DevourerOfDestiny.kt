package com.wingedsheep.mtg.sets.definitions.mh3.cards

import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.revealFromOpeningHand
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardOrder
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.predicates.CardPredicate
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Devourer of Destiny
 * {5}{C}{C}
 * Creature — Eldrazi
 * 6/6
 * You may reveal this card from your opening hand. If you do, at the beginning of your first
 * upkeep, look at the top four cards of your library. You may put one of those cards back on top
 * of your library. Exile the rest.
 * When you cast this spell, exile target permanent that's one or more colors.
 *
 * The reveal is an opening-hand action (CR 103.6b) whose payoff is a delayed trigger created
 * before turn 1, so the controller's next upkeep is their first.
 */
val DevourerOfDestiny = card("Devourer of Destiny") {
    manaCost = "{5}{C}{C}"
    typeLine = "Creature — Eldrazi"
    power = 6
    toughness = 6
    oracleText = "You may reveal this card from your opening hand. If you do, at the beginning of your " +
        "first upkeep, look at the top four cards of your library. You may put one of those cards back " +
        "on top of your library. Exile the rest.\n" +
        "When you cast this spell, exile target permanent that's one or more colors."

    revealFromOpeningHand(
        Effects.CreateDelayedTrigger(
            step = Step.UPKEEP,
            fireOnPlayer = EffectTarget.PlayerRef(Player.You),
            effect = Effects.Pipeline {
                val looked = gather(CardSource.TopOfLibrary(4, Player.You))
                val (kept, rest) = chooseUpToSplit(
                    1, looked,
                    prompt = "You may put one of those cards back on top of your library",
                    selectedLabel = "Put on top",
                    remainderLabel = "Exile"
                )
                toLibraryTop(kept, order = CardOrder.Preserve) // at most one card: nothing to order
                exile(rest)
            }
        )
    )

    triggeredAbility {
        val target = target(
            TargetFilter(GameObjectFilter(cardPredicates = listOf(CardPredicate.IsPermanent, CardPredicate.IsColored)))
        )
        trigger = Triggers.self.isCast()
        effect = Effects.Exile(target)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "2"
        artist = "Raph Lomotan"
        imageUri = "https://cards.scryfall.io/normal/front/5/6/560debcd-feb4-4534-991e-a7aa1cca2409.jpg"
        ruling(
            "2024-06-07",
            "If you reveal more than one Devourer of Destiny from your opening hand, you'll put that many " +
                "triggered abilities on the stack at the beginning of your first upkeep. As a result, you'll " +
                "look at the top four cards of your library, put up to one back on top, and exile the rest " +
                "once for each Devour of Destiny you revealed."
        )
        ruling(
            "2024-06-07",
            "Devourer of Destiny's last triggered ability will resolve before Devourer of Destiny does. If " +
                "Devourer of Destiny is countered or otherwise leaves the stack in response to that triggered " +
                "ability, the triggered ability will still resolve as normal."
        )
    }
}
