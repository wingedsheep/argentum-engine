package com.wingedsheep.mtg.sets.definitions.otj.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.ConditionalEffect
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetCreature

/**
 * Oko, the Ringleader
 * {2}{G}{U}
 * Legendary Planeswalker — Oko
 * Starting Loyalty: 3
 *
 * At the beginning of combat on your turn, Oko becomes a copy of up to one target creature you
 * control until end of turn, except he has hexproof.
 * +1: Draw two cards. If you've committed a crime this turn, discard a card. Otherwise, discard
 * two cards.
 * -1: Create a 3/3 green Elk creature token.
 * -5: For each other nonland permanent you control, create a token that's a copy of that permanent.
 *
 * Implementation:
 * - Combat-begin trigger ([Triggers.BeginCombat], already scoped to your turn): Oko becomes a copy
 *   of up to one target creature you control via [Effects.EachPermanentBecomesCopyOfTarget]
 *   (`affected = Self`), plus [Effects.GrantHexproof] on Self for the "except he has hexproof"
 *   clause — the same compose Fleeting Reflection uses. The target is optional ("up to one"), so
 *   omitting it makes the copy a no-op and Oko simply gains hexproof.
 * - +1: draw two, then a [ConditionalEffect] on [Conditions.YouCommittedCrimeThisTurn] discards one
 *   (crime) or two (no crime).
 * - -1: create a 3/3 green Elk token.
 * - -5: [Effects.ForEachInGroup] over other nonland permanents you control, creating a token copy
 *   of each (`EffectTarget.Self` = the iterated permanent inside the group body).
 */
private const val ELK_TOKEN_IMAGE =
    "https://cards.scryfall.io/normal/front/1/6/1632f3fa-4615-46ee-9768-22bbd9d142d6.jpg?1712316649"

val OkoTheRingleader = card("Oko, the Ringleader") {
    manaCost = "{2}{G}{U}"
    colorIdentity = "GU"
    typeLine = "Legendary Planeswalker — Oko"
    startingLoyalty = 3
    oracleText = "At the beginning of combat on your turn, Oko becomes a copy of up to one target " +
        "creature you control until end of turn, except he has hexproof.\n" +
        "+1: Draw two cards. If you've committed a crime this turn, discard a card. Otherwise, " +
        "discard two cards.\n" +
        "−1: Create a 3/3 green Elk creature token.\n" +
        "−5: For each other nonland permanent you control, create a token that's a copy of " +
        "that permanent."

    triggeredAbility {
        trigger = Triggers.BeginCombat
        target(
            "creature you control",
            TargetCreature(filter = TargetFilter.CreatureYouControl, optional = true),
        )
        effect = Effects.Composite(
            listOf(
                Effects.EachPermanentBecomesCopyOfTarget(
                    target = EffectTarget.ContextTarget(0),
                    duration = Duration.EndOfTurn,
                    affected = EffectTarget.Self,
                ),
                Effects.GrantHexproof(EffectTarget.Self, Duration.EndOfTurn),
            )
        )
        description = "At the beginning of combat on your turn, Oko becomes a copy of up to one " +
            "target creature you control until end of turn, except he has hexproof."
    }

    // +1: Draw two cards. If you've committed a crime this turn, discard a card. Otherwise, discard two.
    loyaltyAbility(+1) {
        effect = Effects.DrawCards(2).then(
            ConditionalEffect(
                condition = Conditions.YouCommittedCrimeThisTurn,
                effect = Patterns.Hand.discardCards(1),
                elseEffect = Patterns.Hand.discardCards(2),
            )
        )
    }

    // -1: Create a 3/3 green Elk creature token.
    loyaltyAbility(-1) {
        effect = Effects.CreateToken(
            power = 3,
            toughness = 3,
            colors = setOf(Color.GREEN),
            creatureTypes = setOf("Elk"),
            imageUri = ELK_TOKEN_IMAGE,
        )
    }

    // -5: For each other nonland permanent you control, create a token that's a copy of that permanent.
    loyaltyAbility(-5) {
        effect = Effects.ForEachInGroup(
            filter = GroupFilter(
                GameObjectFilter.NonlandPermanent.youControl(),
                excludeSelf = true,
            ),
            effect = Effects.CreateTokenCopyOfTarget(target = EffectTarget.Self),
        )
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "223"
        artist = "Magali Villeneuve"
        imageUri = "https://cards.scryfall.io/normal/front/3/9/396df8d6-e85d-4486-8116-68841b7e1e2e.jpg?1712356176"

        ruling("2024-04-12", "Oko's static ability triggers at the beginning of combat on your turn. If you control no creatures, you may still let the ability resolve; Oko just doesn't become a copy of anything.")
        ruling("2024-04-12", "While Oko is a copy of a creature, he's both a planeswalker and a creature. He can attack and be attacked, and his loyalty abilities can still be activated.")
        ruling("2024-04-12", "For -5, the tokens enter as copies of the permanents as they currently exist, copying only the copiable values (not counters, Auras, or other effects).")
    }
}
