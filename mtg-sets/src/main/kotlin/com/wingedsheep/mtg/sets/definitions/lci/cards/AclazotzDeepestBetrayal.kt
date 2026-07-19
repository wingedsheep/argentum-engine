package com.wingedsheep.mtg.sets.definitions.lci.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ActivationRestriction
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.conditions.Compare
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.effects.TransformEffect
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Aclazotz, Deepest Betrayal // Temple of the Dead (The Lost Caverns of Ixalan)
 * {3}{B}{B}
 * Legendary Creature — Bat God // Land
 *
 * Front — Aclazotz, Deepest Betrayal (4/4, Flying, lifelink)
 *   Whenever Aclazotz attacks, each opponent discards a card. For each opponent who can't, you
 *   draw a card.
 *   Whenever an opponent discards a land card, create a 1/1 black Bat creature token with flying.
 *   When Aclazotz dies, return it to the battlefield tapped and transformed under its owner's
 *   control.
 *
 * Back — Temple of the Dead (Land)
 *   {T}: Add {B}.
 *   {2}{B}, {T}: Transform this land. Activate only if a player has one or fewer cards in hand
 *   and only as a sorcery.
 *
 * Implementation:
 *  - The attack trigger draws for each opponent who can't discard (an empty-handed opponent)
 *    via [DynamicAmount.CountPlayersWith]`(EachOpponent, hand <= 0)` — the Bandit's Talent idiom —
 *    then makes each opponent discard via [Effects.EachOpponentDiscards]. The count is snapshotted
 *    before the discards (an opponent's own draw can't change another player's hand), so
 *    draw-before-discard is outcome-equivalent to the printed discard-then-draw order.
 *  - The Bat trigger fires per land an opponent discards ([Triggers.discards]`(EachOpponent, Land)`).
 *  - Dies-return uses the shared [Effects.ReturnSelfFromGraveyardTransformed]`(tapped = true)`.
 *  - Back land: `{T}: Add {B}` + a `{2}{B}, {T}` sorcery-speed [TransformEffect] gated on at least
 *    one player (any) having one or fewer cards in hand.
 */

private val AclazotzDeepestBetrayalFront = card("Aclazotz, Deepest Betrayal") {
    manaCost = "{3}{B}{B}"
    colorIdentity = "B"
    typeLine = "Legendary Creature — Bat God"
    power = 4
    toughness = 4
    oracleText = "Flying, lifelink\n" +
        "Whenever Aclazotz attacks, each opponent discards a card. For each opponent who can't, " +
        "you draw a card.\n" +
        "Whenever an opponent discards a land card, create a 1/1 black Bat creature token with " +
        "flying.\n" +
        "When Aclazotz dies, return it to the battlefield tapped and transformed under its " +
        "owner's control."

    keywords(Keyword.FLYING, Keyword.LIFELINK)

    triggeredAbility {
        trigger = Triggers.Attacks
        effect = Effects.Composite(
            // Draw for each opponent who can't discard (empty hand), snapshotted before the discards.
            Effects.DrawCards(
                DynamicAmount.CountPlayersWith(
                    scope = Player.EachOpponent,
                    condition = Compare(
                        left = DynamicAmount.Count(Player.You, Zone.HAND),
                        operator = ComparisonOperator.LTE,
                        right = DynamicAmount.Fixed(0),
                    ),
                )
            ),
            Effects.EachOpponentDiscards(1),
        )
        description = "Whenever Aclazotz attacks, each opponent discards a card. For each " +
            "opponent who can't, you draw a card."
    }

    triggeredAbility {
        trigger = Triggers.discards(player = Player.EachOpponent, cardFilter = GameObjectFilter.Land)
        effect = Effects.CreateToken(
            power = 1,
            toughness = 1,
            colors = setOf(Color.BLACK),
            creatureTypes = setOf("Bat"),
            keywords = setOf(Keyword.FLYING),
            imageUri = "https://cards.scryfall.io/normal/front/1/0/100c0127-49dd-4a78-9c88-1881e7923674.jpg?1721425184",
        )
        description = "Whenever an opponent discards a land card, create a 1/1 black Bat creature " +
            "token with flying."
    }

    triggeredAbility {
        trigger = Triggers.Dies
        effect = Effects.ReturnSelfFromGraveyardTransformed(tapped = true)
        description = "When Aclazotz dies, return it to the battlefield tapped and transformed " +
            "under its owner's control."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "88"
        artist = "Steve Prescott"
        imageUri = "https://cards.scryfall.io/normal/front/6/2/627c392c-4d18-4eb2-a4e8-c668f61f5487.jpg?1782694541"
    }
}

private val TempleOfTheDead = card("Temple of the Dead") {
    manaCost = ""
    colorIdentity = "B"
    typeLine = "Land"
    oracleText = "(Transforms from Aclazotz, Deepest Betrayal.)\n" +
        "{T}: Add {B}.\n" +
        "{2}{B}, {T}: Transform this land. Activate only if a player has one or fewer cards in " +
        "hand and only as a sorcery."

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddMana(Color.BLACK, 1)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{2}{B}"), Costs.Tap)
        effect = TransformEffect(EffectTarget.Self)
        timing = TimingRule.SorcerySpeed
        restrictions = listOf(
            ActivationRestriction.OnlyIfCondition(
                // At least one player (any) has one or fewer cards in hand.
                Conditions.CompareAmounts(
                    DynamicAmount.CountPlayersWith(
                        scope = Player.Each,
                        condition = Compare(
                            left = DynamicAmount.Count(Player.You, Zone.HAND),
                            operator = ComparisonOperator.LTE,
                            right = DynamicAmount.Fixed(1),
                        ),
                    ),
                    ComparisonOperator.GTE,
                    DynamicAmount.Fixed(1),
                )
            )
        )
        description = "Transform this land. Activate only if a player has one or fewer cards in " +
            "hand and only as a sorcery."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "88"
        artist = "Steve Prescott"
        flavorText = "Chimil gave the Oltec peace in death. Aclazotz ripped it away."
        imageUri = "https://cards.scryfall.io/normal/back/6/2/627c392c-4d18-4eb2-a4e8-c668f61f5487.jpg?1782694541"
    }
}

val AclazotzDeepestBetrayal: CardDefinition = CardDefinition.doubleFacedPermanent(
    frontFace = AclazotzDeepestBetrayalFront,
    backFace = TempleOfTheDead,
)
