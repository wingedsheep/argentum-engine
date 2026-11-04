package com.wingedsheep.mtg.sets.definitions.rix.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.CardNumericProperty

/**
 * Azor's Gateway // Sanctum of the Sun (Rivals of Ixalan — the card's earliest printing)
 * {2}
 * Legendary Artifact // Legendary Land
 *
 * Front — Azor's Gateway (Legendary Artifact, {2})
 *   {1}, {T}: Draw a card, then exile a card from your hand. If cards with five or more different
 *   mana values are exiled with Azor's Gateway, you gain 5 life, untap Azor's Gateway, and
 *   transform it.
 *
 * Back — Sanctum of the Sun (Legendary Land)
 *   {T}: Add X mana of any one color, where X is your life total.
 *
 * Implementation:
 *  - The draw comes first, so the hand always has a card when the mandatory `chooseExactly(1)`
 *    exile runs. The card is exiled face up (first ruling) and linked to the Gateway
 *    (`linkToSource`, CR 607), so only cards this object exiled count; transforming keeps the
 *    same object, so the pile is untouched by the flip.
 *  - "Five or more different mana values" is a [DynamicAmounts.zone] `distinctValues(MANA_VALUE)`
 *    over the linked pile, read on resolution after this activation's exile. The cards are read
 *    in their owners' exile zones ([Player.OwnersOfLinkedExile]). Mana value off the battlefield
 *    treats {X} as 0 and a split card as its combined cost (rulings); a land's 0 counts as a value.
 *  - Life, untap and transform all hang off the one condition, in printed order.
 *  - Sanctum's ability is a mana ability (last ruling) adding [DynamicAmounts.yourLifeTotal] of
 *    one chosen color, read as it resolves.
 */

private val cardsExiledWithIt = GameObjectFilter.Any.exiledWithSource()

private val AzorsGatewayFront = card("Azor's Gateway") {
    manaCost = "{2}"
    typeLine = "Legendary Artifact"
    oracleText = "{1}, {T}: Draw a card, then exile a card from your hand. If cards with five or " +
        "more different mana values are exiled with Azor's Gateway, you gain 5 life, untap " +
        "Azor's Gateway, and transform it."

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{1}"), Costs.Tap)
        effect = Effects.Pipeline {
            run(Effects.DrawCards(1))
            val gatewayHand = gather(CardSource.FromZone(Zone.HAND, Player.You))
            val gatewayExiled = chooseExactly(1, from = gatewayHand, prompt = "Choose a card to exile")
            move(gatewayExiled, CardDestination.ToZone(Zone.EXILE), linkToSource = true)
            run(
                Effects.If(
                    condition = Conditions.CompareAmounts(
                        left = DynamicAmounts.zone(Player.OwnersOfLinkedExile, Zone.EXILE, cardsExiledWithIt)
                            .distinctValues(CardNumericProperty.MANA_VALUE),
                        operator = ComparisonOperator.GTE,
                        right = 5,
                    ),
                    then = Effects.GainLife(5) then
                        Effects.Untap(EffectTarget.Self) then
                        Effects.Transform(EffectTarget.Self),
                )
            )
        }
        description = "{1}, {T}: Draw a card, then exile a card from your hand. If cards with five " +
            "or more different mana values are exiled with Azor's Gateway, you gain 5 life, untap " +
            "Azor's Gateway, and transform it."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "176"
        artist = "Yeong-Hao Han"
        imageUri = "https://cards.scryfall.io/normal/front/3/0/303d51ab-b9c4-4647-950f-291daabe7b81.jpg?1783935273"
        ruling("2018-01-19", "The cards you exile from your hand are exiled face up.")
        ruling("2018-01-19", "The mana value of a split card, such as a card with aftermath from the Amonkhet block, is based on the combined mana cost of its two halves. A split card doesn't have two mana values.")
        ruling("2018-01-19", "If a card in exile has {X} in its mana cost, X is considered to be 0.")
        ruling("2018-01-19", "If an object has no mana cost, its mana value is 0. A mana value of 0 can still help unlock Azor's Gateway.")
    }
}

private val SanctumOfTheSun = card("Sanctum of the Sun") {
    manaCost = ""
    typeLine = "Legendary Land"
    oracleText = "(Transforms from Azor's Gateway.)\n" +
        "{T}: Add X mana of any one color, where X is your life total."

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddAnyColorMana(DynamicAmounts.yourLifeTotal())
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "176"
        artist = "Yeong-Hao Han"
        flavorText = "As soon as Jace laid eyes on the great sphinx's chamber, the shards of memory began sliding into place."
        imageUri = "https://cards.scryfall.io/normal/back/3/0/303d51ab-b9c4-4647-950f-291daabe7b81.jpg?1783935273"
        ruling("2018-01-19", "The ability of Sanctum of the Sun is a mana ability. It doesn't use the stack and can't be responded to.")
    }
}

val AzorsGateway: CardDefinition = CardDefinition.doubleFacedPermanent(
    frontFace = AzorsGatewayFront,
    backFace = SanctumOfTheSun,
)
