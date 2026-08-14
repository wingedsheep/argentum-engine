package com.wingedsheep.mtg.sets.definitions.woe.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.effects.GrantMayPlayFromExileEffect
import com.wingedsheep.sdk.scripting.effects.MayEffect
import com.wingedsheep.sdk.scripting.effects.MoveCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SacrificeSelfEffect
import com.wingedsheep.sdk.scripting.events.CounterTypeFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Rotisserie Elemental
 * {R}
 * Creature — Elemental
 * 1/1
 *
 * Menace
 * Whenever this creature deals combat damage to a player, put a skewer counter on this creature.
 * Then you may sacrifice it. If you do, exile the top X cards of your library, where X is the
 * number of skewer counters on this creature. You may play those cards this turn.
 *
 * Skewer counters are a plain tally ([Counters.SKEWER]) — no keyword, no rule of their own — so the
 * only interesting part is the ordering inside the trigger.
 *
 * X is "the number of skewer counters on this creature", read *after* the new counter goes on but
 * for a permanent that the same resolution is about to sacrifice. Counters cease to exist on a zone
 * change (CR 122.2), and a mid-resolution `SacrificeSelfEffect` leaves no last-known-counters
 * snapshot for a `Source` read to fall back on, so the top-of-library gather runs one step *before*
 * the sacrifice and the exile one step after. Nothing can observe the difference: no player gets
 * priority inside a resolving ability (the printed ruling says exactly this), triggers from the
 * sacrifice wait until the ability finishes, and sacrificing a creature cannot reorder a library —
 * so the cards gathered are the same cards the printed sequence would exile.
 *
 * `sourceRequiredZone = BATTLEFIELD` carries the "If you do" clause for the case the ordering can't:
 * if the Elemental is already gone when the trigger resolves (bounced or killed in response), there
 * is nothing to sacrifice, the player is never prompted, and nothing is exiled.
 *
 * The exiled cards are playable, not free — [GrantMayPlayFromExileEffect] defaults to an
 * end-of-turn permission and normal timing rules, so a land among them is still a land drop during
 * your main phase.
 */
val RotisserieElemental = card("Rotisserie Elemental") {
    manaCost = "{R}"
    colorIdentity = "R"
    typeLine = "Creature — Elemental"
    power = 1
    toughness = 1
    oracleText = "Menace\n" +
        "Whenever this creature deals combat damage to a player, put a skewer counter on this " +
        "creature. Then you may sacrifice it. If you do, exile the top X cards of your library, " +
        "where X is the number of skewer counters on this creature. You may play those cards this turn."

    keywords(Keyword.MENACE)

    triggeredAbility {
        trigger = Triggers.DealsCombatDamageToPlayer
        effect = Effects.AddCounters(Counters.SKEWER, 1, EffectTarget.Self)
            .then(
                MayEffect(
                    GatherCardsEffect(
                        source = CardSource.TopOfLibrary(
                            DynamicAmounts.countersOnSelf(CounterTypeFilter.Named(Counters.SKEWER))
                        ),
                        storeAs = "skeweredCards"
                    )
                        .then(SacrificeSelfEffect)
                        .then(
                            MoveCollectionEffect(
                                from = "skeweredCards",
                                destination = CardDestination.ToZone(Zone.EXILE)
                            )
                        )
                        .then(GrantMayPlayFromExileEffect("skeweredCards")),
                    descriptionOverride = "You may sacrifice Rotisserie Elemental to exile that " +
                        "many cards from the top of your library and play them this turn.",
                    sourceRequiredZone = Zone.BATTLEFIELD
                )
            )
        description = "Whenever this creature deals combat damage to a player, put a skewer " +
            "counter on this creature. Then you may sacrifice it. If you do, exile the top X " +
            "cards of your library, where X is the number of skewer counters on this creature. " +
            "You may play those cards this turn."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "148"
        artist = "Leonardo Santanna"
        imageUri = "https://cards.scryfall.io/normal/front/8/d/8d787045-3918-4ed6-85ea-843d1f2356f2.jpg?1783915089"

        ruling(
            "2023-09-01",
            "You choose whether to sacrifice Rotisserie Elemental as its triggered ability " +
                "resolves. No player may respond between the time you sacrifice it and the time " +
                "you exile cards from the top of your library."
        )
        ruling(
            "2023-09-01",
            "You pay all costs and follow all normal timing rules for cards played from exile this " +
                "way. For example, if the exiled card is a land card, you may play it only during " +
                "your main phase while the stack is empty."
        )
    }
}
