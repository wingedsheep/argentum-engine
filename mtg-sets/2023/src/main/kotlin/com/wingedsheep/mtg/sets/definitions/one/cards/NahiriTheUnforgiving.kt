package com.wingedsheep.mtg.sets.definitions.one.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.minus
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.MustAttack
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.predicates.CardPredicate
import com.wingedsheep.sdk.scripting.predicates.ControllerPredicate

/**
 * Nahiri, the Unforgiving — Phyrexia: All Will Be One #211
 * {1}{R}{R/W/P}{W} · Legendary Planeswalker — Nahiri · Starting loyalty 5
 *
 * Compleated
 * +1: Until your next turn, up to one target creature attacks a player each combat if able.
 * +1: Discard a card, then draw a card.
 * 0: Exile target creature or Equipment card with mana value less than Nahiri's loyalty from your
 *    graveyard. Create a token that's a copy of it. That token gains haste. Exile it at the
 *    beginning of the next end step.
 *
 * The first +1 grants `MustAttack(playersOnly = true)` until Nahiri's controller's next turn: the
 * creature must attack each combat, and attacking a planeswalker or battle doesn't satisfy it while
 * a player could be attacked. The 0's mana-value cap is live — the target is re-checked against
 * Nahiri's current loyalty on resolution, per the ruling.
 */
val NahiriTheUnforgiving = card("Nahiri, the Unforgiving") {
    manaCost = "{1}{R}{R/W/P}{W}"
    colorIdentity = "RW"
    typeLine = "Legendary Planeswalker — Nahiri"
    startingLoyalty = 5
    oracleText = "Compleated ({R/W/P} can be paid with {R}, {W}, or 2 life. If life was paid, this planeswalker enters with two fewer loyalty counters.)\n" +
        "+1: Until your next turn, up to one target creature attacks a player each combat if able.\n" +
        "+1: Discard a card, then draw a card.\n" +
        "0: Exile target creature or Equipment card with mana value less than Nahiri's loyalty from your graveyard. Create a token that's a copy of it. That token gains haste. Exile it at the beginning of the next end step."

    keywords(Keyword.COMPLEATED)

    loyaltyAbility(+1) {
        val creature = target(TargetFilter.Creature, optional = true)
        effect = Effects.GrantStaticAbility(MustAttack(playersOnly = true), creature, Duration.UntilYourNextTurn)
    }

    loyaltyAbility(+1) {
        effect = Effects.Discard(1) then Effects.DrawCards(1)
    }

    loyaltyAbility(0) {
        val card = target(
            TargetFilter(
                GameObjectFilter(
                    cardPredicates = listOf(
                        CardPredicate.Or(listOf(CardPredicate.IsCreature, CardPredicate.HasSubtype(Subtype.EQUIPMENT))),
                        CardPredicate.ManaValueAtMostDynamic(DynamicAmounts.countersOnSelf(CounterType.LOYALTY) - 1)
                    ),
                    controllerPredicate = ControllerPredicate.OwnedByYou
                ),
                zone = Zone.GRAVEYARD
            )
        )
        effect = Effects.Exile(card) then
            Effects.CreateTokenCopyOfTarget(
                target = card,
                addedKeywords = setOf(Keyword.HASTE),
                exileAtStep = Step.END
            )
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "211"
        artist = "Chase Stone"
        imageUri = "https://cards.scryfall.io/normal/front/5/7/578e35b9-7252-4767-a1f3-aecd71256515.jpg?1783917998"
        ruling("2023-02-04", "If the creature targeted by the first loyalty ability can't attack for any reason (such as being tapped), then it doesn't attack. If there's a cost associated with having it attack, its controller isn't forced to pay that cost. If they don't, it doesn't have to attack in that case either.")
        ruling("2023-02-04", "The last loyalty ability will check Nahiri's loyalty as the ability tries to resolve to see if the target is still legal. If it's not, the ability won't resolve and none of its effects will happen. The card won't be exiled and no token will be created.")
        ruling("2023-02-04", "A hybrid Phyrexian mana symbol contributes 1 toward the mana value of a card, even if life is paid for it. Specifically, Nahiri's mana value is always 4.")
        ruling("2023-02-04", "The compleated ability looks only at whether a player chose to pay 2 life for a Phyrexian mana symbol as they were casting the spell. If a player paid life for some other reason while casting the spell, that will not reduce the number of loyalty counters the planeswalker enters the battlefield with.")
        ruling("2023-02-04", "Other replacement effects that would change the number of loyalty counters Nahiri enters with will apply as normal.")
    }
}
