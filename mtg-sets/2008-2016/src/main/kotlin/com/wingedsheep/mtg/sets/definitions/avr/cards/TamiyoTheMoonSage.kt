package com.wingedsheep.mtg.sets.definitions.avr.cards

import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TriggeredAbility
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.predicates.CardPredicate
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Tamiyo, the Moon Sage — Avacyn Restored #79
 * {3}{U}{U} · Legendary Planeswalker — Tamiyo · Starting loyalty 4
 *
 * +1: Tap target permanent. It doesn't untap during its controller's next untap step.
 * −2: Draw a card for each tapped creature target player controls.
 * −8: You get an emblem with "You have no maximum hand size" and "Whenever a card is put into
 *     your graveyard from anywhere, you may return it to your hand."
 *
 * Modeling notes:
 *
 *  - **The +1** is the Crippling Chill pair: tap, then [AbilityFlag.DOESNT_UNTAP] for
 *    [Duration.UntilAfterAffectedControllersNextUntap]. The duration keys off the permanent's own
 *    controller, which is what the ruling asks for — it tracks the permanent, not the player who
 *    controlled it when the ability resolved. Already-tapped permanents are legal targets.
 *  - **The −2** counts tapped creatures the targeted player controls at resolution (ruling) —
 *    a battlefield count scoped to `Player.ContextPlayer(0)`; the draw goes to Tamiyo's controller.
 *  - **The −8 emblem** has two abilities, so it is the two primitives each half needs, applied
 *    together on resolution: "You have no maximum hand size" is [Effects.RemoveMaximumHandSize], the
 *    player-scoped permanent property Wrenn and Seven's emblem uses; the trigger is a permanent
 *    [Effects.CreateGlobalTriggeredAbility] (Jace, Unraveler of Secrets' emblem shape). "From
 *    anywhere" is a bare `to = GRAVEYARD` zone change with no `from` (Vulturous Zombie), filtered to
 *    nontoken cards **owned** by you — a card always goes to its owner's graveyard. The return reads
 *    the triggering card and only moves it out of the graveyard, so a card that left the graveyard
 *    before the trigger resolved is a new object and stays where it is.
 *  - Current Oracle text has no "Exile Tamiyo" rider. If she is activated from exactly 8 loyalty she
 *    is already in the graveyard when the emblem is created, so she doesn't come back (ruling).
 */
val TamiyoTheMoonSage = card("Tamiyo, the Moon Sage") {
    manaCost = "{3}{U}{U}"
    colorIdentity = "U"
    typeLine = "Legendary Planeswalker — Tamiyo"
    startingLoyalty = 4
    oracleText = "+1: Tap target permanent. It doesn't untap during its controller's next untap step.\n" +
        "−2: Draw a card for each tapped creature target player controls.\n" +
        "−8: You get an emblem with \"You have no maximum hand size\" and \"Whenever a card is put " +
        "into your graveyard from anywhere, you may return it to your hand.\""

    // +1: Tap target permanent. It doesn't untap during its controller's next untap step.
    loyaltyAbility(+1) {
        val permanent = target(TargetFilter.Permanent)
        effect = Effects.Tap(permanent) then Effects.GrantKeyword(
            AbilityFlag.DOESNT_UNTAP,
            permanent,
            Duration.UntilAfterAffectedControllersNextUntap
        )
        description = "Tap target permanent. It doesn't untap during its controller's next untap step."
    }

    // −2: Draw a card for each tapped creature target player controls.
    loyaltyAbility(-2) {
        val player = target(Targets.Player)
        effect = Effects.DrawCards(
            DynamicAmounts.count(player.asPlayer, Zone.BATTLEFIELD, GameObjectFilter.Creature.tapped())
        )
        description = "Draw a card for each tapped creature target player controls."
    }

    // −8: You get an emblem with "You have no maximum hand size" and "Whenever a card is put into
    //     your graveyard from anywhere, you may return it to your hand."
    loyaltyAbility(-8) {
        effect = Effects.RemoveMaximumHandSize() then Effects.CreateGlobalTriggeredAbility(
            ability = TriggeredAbility.create(
                trigger = Triggers.a(
                    GameObjectFilter.Any
                        .withCardPredicate(CardPredicate.IsNontoken)
                        .ownedByYou()
                ).changesZone(to = Zone.GRAVEYARD),
                effect = Effects.May(Effects.ReturnToHandFromGraveyard(EffectTarget.TriggeringEntity))
            ),
            descriptionOverride = "Whenever a card is put into your graveyard from anywhere, you may " +
                "return it to your hand."
        )
        description = "You get an emblem with \"You have no maximum hand size\" and \"Whenever a card " +
            "is put into your graveyard from anywhere, you may return it to your hand.\""
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "79"
        artist = "Eric Deschamps"
        imageUri = "https://cards.scryfall.io/normal/front/b/9/b9398926-13b9-47b8-b66b-1ab9d06bb704.jpg?1783940708"

        ruling("2013-07-01", "The ability can target a tapped permanent. If the targeted permanent is already tapped when it resolves, that permanent just remains tapped and doesn't untap during its controller's next untap step.")
        ruling("2012-05-01", "Tamiyo's first ability tracks the permanent, but not its controller. If the permanent changes controllers before its first controller's next untap step has come around, then it won't untap during its new controller's next untap step.")
        ruling("2012-05-01", "The number of tapped creatures the player controls is determined when Tamiyo's second ability resolves.")
        ruling("2012-05-01", "If you activate Tamiyo's third ability when she has eight loyalty counters on her, she'll be put into your graveyard before the emblem is created. She won't return to your hand.")
        ruling("2012-05-01", "Cards that are put into your graveyard can be returned to your hand by the emblem's ability even if they were never in your hand.")
    }
}
