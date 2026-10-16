package com.wingedsheep.mtg.sets.definitions.snc.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Supertype
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.grantedTriggeredAbility
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.effects.CopyExceptions
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Ob Nixilis, the Adversary — Streets of New Capenna #206
 * {1}{B}{R} · Legendary Planeswalker — Nixilis · Loyalty 3
 *
 * Casualty X. The copy isn't legendary and has starting loyalty X. (As you cast this spell, you
 * may sacrifice a creature with power X. When you do, copy this spell. The copy becomes a token.)
 * +1: Each opponent loses 2 life unless they discard a card. If you control a Demon or Devil, you
 * gain 2 life.
 * −2: Create a 1/1 red Devil creature token with "When this token dies, it deals 1 damage to
 * any target."
 * −7: Target player draws seven cards and loses 7 life.
 */
val ObNixilisTheAdversary = card("Ob Nixilis, the Adversary") {
    manaCost = "{1}{B}{R}"
    colorIdentity = "BR"
    typeLine = "Legendary Planeswalker — Nixilis"
    startingLoyalty = 3
    oracleText = "Casualty X. The copy isn't legendary and has starting loyalty X. (As you cast this " +
        "spell, you may sacrifice a creature with power X. When you do, copy this spell. The copy " +
        "becomes a token.)\n" +
        "+1: Each opponent loses 2 life unless they discard a card. If you control a Demon or Devil, " +
        "you gain 2 life.\n" +
        "−2: Create a 1/1 red Devil creature token with \"When this token dies, it deals 1 damage " +
        "to any target.\"\n" +
        "−7: Target player draws seven cards and loses 7 life."

    keywordAbility(
        KeywordAbility.casualtyX(
            copyExceptions = CopyExceptions(removedSupertypes = setOf(Supertype.LEGENDARY)),
            copyStartingLoyalty = DynamicAmounts.xValue()
        )
    )

    loyaltyAbility(+1) {
        effect = Effects.ForEachPlayer(
            players = Player.EachOpponent,
            effect = Effects.PayOrSuffer(
                cost = Costs.pay.Discard(count = 1),
                suffer = Effects.LoseLife(2, target = EffectTarget.Controller),
                player = EffectTarget.Controller
            )
        ) then Effects.If(
            Conditions.YouControl(GameObjectFilter.Permanent.withAnySubtype("Demon", "Devil")),
            Effects.GainLife(2)
        )
        description = "Each opponent loses 2 life unless they discard a card. If you control a Demon " +
            "or Devil, you gain 2 life."
    }

    loyaltyAbility(-2) {
        effect = Effects.CreateToken(
            power = 1,
            toughness = 1,
            colors = setOf(Color.RED),
            creatureTypes = setOf("Devil"),
            triggeredAbilities = listOf(
                grantedTriggeredAbility {
                    trigger = Triggers.self.dies()
                    val any = target(Targets.Any)
                    effect = Effects.DealDamage(1, any)
                    description = "When this token dies, it deals 1 damage to any target."
                }
            ),
            imageUri = "https://cards.scryfall.io/normal/front/e/c/ec75fdab-818c-4f3a-94c1-fd3f577a87f5.jpg?1783922960",
        )
        description = "Create a 1/1 red Devil creature token with \"When this token dies, it deals " +
            "1 damage to any target.\""
    }

    loyaltyAbility(-7) {
        val player = target(Targets.Player)
        effect = Effects.DrawCards(7, player) then Effects.LoseLife(7, player)
        description = "Target player draws seven cards and loses 7 life."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "206"
        artist = "Yongjae Choi"
        imageUri = "https://cards.scryfall.io/normal/front/8/0/80226520-6b0a-48ea-a868-a29eb3fbd403.jpg?1783923077"
        ruling("2022-04-29", "As you cast a spell with casualty X, you choose whether to pay its casualty cost and what the value of X will be.")
        ruling("2022-04-29", "The copied spell from Ob Nixilis, the Adversary's triggered ability copies exactly what is printed on Ob Nixilis, except its starting loyalty is equal to the chosen value of X and it isn't legendary. The copy becomes a token as it resolves.")
        ruling("2022-04-29", "You can control exactly one legendary Ob Nixilis, the Adversary and any number of nonlegendary copies of Ob Nixilis, the Adversary.")
    }
}
