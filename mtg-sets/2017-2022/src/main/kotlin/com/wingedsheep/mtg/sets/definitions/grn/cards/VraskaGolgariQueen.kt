package com.wingedsheep.mtg.sets.definitions.grn.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TriggeredAbility
import com.wingedsheep.sdk.scripting.events.Recipient
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Vraska, Golgari Queen
 * {2}{B}{G}
 * Legendary Planeswalker — Vraska
 * Loyalty 4
 *
 * +2: You may sacrifice another permanent. If you do, you gain 1 life and draw a card.
 * −3: Destroy target nonland permanent with mana value 3 or less.
 * −9: You get an emblem with "Whenever a creature you control deals combat damage to a player,
 *     that player loses the game."
 *
 * Modeling notes:
 *  - +2 is the Eddie Brock shape: [Effects.MayPay] whose "cost" is a non-targeting
 *    [Effects.SacrificeOwn] of any permanent with `excludeSource` ("another"), so "If you do" gates
 *    the life gain and draw on a permanent actually being sacrificed. The choice is made while the
 *    ability resolves, per the ruling.
 *  - The −9 emblem is a triggered-ability emblem, so it uses [Effects.CreateGlobalTriggeredAbility]
 *    (the Kaito/Sarkhan shape); the trigger is Kaito's "creature you control deals combat damage to a
 *    player" and "that player" is the damaged player, [Player.TriggeringPlayer] (the Summon: Primal
 *    Odin shape).
 */
val VraskaGolgariQueen = card("Vraska, Golgari Queen") {
    manaCost = "{2}{B}{G}"
    colorIdentity = "BG"
    typeLine = "Legendary Planeswalker — Vraska"
    startingLoyalty = 4
    oracleText = "+2: You may sacrifice another permanent. If you do, you gain 1 life and draw a card.\n" +
        "−3: Destroy target nonland permanent with mana value 3 or less.\n" +
        "−9: You get an emblem with \"Whenever a creature you control deals combat damage to a " +
        "player, that player loses the game.\""

    // +2: You may sacrifice another permanent. If you do, you gain 1 life and draw a card.
    loyaltyAbility(+2) {
        effect = Effects.MayPay(
            Effects.SacrificeOwn(GameObjectFilter.Permanent, excludeSource = true),
            then = Effects.GainLife(1) then Effects.DrawCards(1),
            descriptionOverride = "You may sacrifice another permanent. If you do, you gain 1 life " +
                "and draw a card.",
        )
    }

    // −3: Destroy target nonland permanent with mana value 3 or less.
    loyaltyAbility(-3) {
        val permanent = target(TargetFilter.NonlandPermanent.manaValueAtMost(3))
        effect = Effects.Destroy(permanent)
    }

    // −9: You get an emblem with "Whenever a creature you control deals combat damage to a player,
    //     that player loses the game."
    loyaltyAbility(-9) {
        val emblemText = "Whenever a creature you control deals combat damage to a player, that " +
            "player loses the game."
        effect = Effects.CreateGlobalTriggeredAbility(
            ability = TriggeredAbility.create(
                trigger = Triggers.a(GameObjectFilter.Creature.youControl())
                    .dealsCombatDamage(Recipient.AnyPlayer),
                effect = Effects.LoseGame(
                    target = EffectTarget.PlayerRef(Player.TriggeringPlayer),
                    message = "Vraska, Golgari Queen's emblem: dealt combat damage",
                ),
                descriptionOverride = emblemText,
            ),
            descriptionOverride = emblemText,
        )
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "213"
        artist = "Magali Villeneuve"
        imageUri = "https://cards.scryfall.io/normal/front/a/3/a36d3ea7-0f18-4865-b47b-755673db065e.jpg?1783934118"

        ruling("2018-10-05", "You choose whether to sacrifice a permanent (and which one to sacrifice) while Vraska's first ability is resolving. No player may take actions between the time you choose which permanent to sacrifice and the time you do so.")
        ruling("2018-10-05", "Tokens that aren't a copy of something else don't have a mana cost. Anything without a mana cost normally has a mana value of 0.")
        ruling("2018-10-05", "If a permanent has {X} in its mana cost, X is considered to be 0.")
    }
}
