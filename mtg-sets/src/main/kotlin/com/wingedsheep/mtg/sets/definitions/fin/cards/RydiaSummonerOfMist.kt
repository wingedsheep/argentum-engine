package com.wingedsheep.mtg.sets.definitions.fin.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.effects.AddCountersEffect
import com.wingedsheep.sdk.scripting.effects.IfYouDoEffect
import com.wingedsheep.sdk.scripting.effects.MayEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Rydia, Summoner of Mist — Final Fantasy #239
 * {R}{G} · Legendary Creature — Human Shaman · 1/2
 *
 * Landfall — Whenever a land you control enters, you may discard a card. If you do, draw a card.
 * Summon — {X}, {T}: Return target Saga card with mana value X from your graveyard to the
 * battlefield with a finality counter on it. It gains haste until end of turn. Activate only as
 * a sorcery.
 *
 * "Landfall" and "Summon" are ability words (CR 207.2c) — flavor only, no rules meaning. The
 * landfall trigger is the Giott rummage shape: [MayEffect] wrapping [IfYouDoEffect] (discard a
 * card → if you do, draw a card).
 *
 * The Summon ability is the Ent-Draught Basin "{X} in the activation cost" shape: the chosen X
 * threads into the target filter via `manaValueEqualsX()`, so only a Saga whose mana value is
 * exactly X is a legal target. It then mirrors Rakdos Joins Up's "return target ... with
 * counters" idiom — a captured target handle moved GRAVEYARD → BATTLEFIELD, then the
 * [Counters.FINALITY] counter and haste are applied to that same returned permanent. The
 * finality counter's exile-instead-of-die replacement is handled by the engine.
 */
val RydiaSummonerOfMist = card("Rydia, Summoner of Mist") {
    manaCost = "{R}{G}"
    colorIdentity = "RG"
    typeLine = "Legendary Creature — Human Shaman"
    power = 1
    toughness = 2
    oracleText = "Landfall — Whenever a land you control enters, you may discard a card. If you do, draw a card.\n" +
        "Summon — {X}, {T}: Return target Saga card with mana value X from your graveyard to the battlefield " +
        "with a finality counter on it. It gains haste until end of turn. Activate only as a sorcery."

    triggeredAbility {
        trigger = Triggers.LandYouControlEnters
        effect = MayEffect(
            effect = IfYouDoEffect(
                action = Patterns.Hand.discardCards(1),
                ifYouDo = Effects.DrawCards(1),
            ),
            descriptionOverride = "You may discard a card. If you do, draw a card.",
        )
        description = "Landfall — Whenever a land you control enters, you may discard a card. If you do, draw a card."
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{X}"), Costs.Tap)
        val saga = target(
            "target Saga card with mana value X in your graveyard",
            TargetObject(
                filter = TargetFilter(
                    GameObjectFilter.Any.withSubtype(Subtype.SAGA).ownedByYou().manaValueEqualsX(),
                    zone = Zone.GRAVEYARD,
                ),
            ),
        )
        effect = Effects.Move(saga, Zone.BATTLEFIELD, fromZone = Zone.GRAVEYARD)
            .then(AddCountersEffect(Counters.FINALITY, 1, saga))
            .then(Effects.GrantKeyword(Keyword.HASTE, saga, Duration.EndOfTurn))
        timing = TimingRule.SorcerySpeed
        description = "Summon — {X}, {T}: Return target Saga card with mana value X from your graveyard to " +
            "the battlefield with a finality counter on it. It gains haste until end of turn. Activate only " +
            "as a sorcery."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "239"
        artist = "Yumi Yaoshida"
        imageUri = "https://cards.scryfall.io/normal/front/9/9/99450143-6ab5-463d-9e04-e8e6703a8b92.jpg?1748706673"
    }
}
