package com.wingedsheep.mtg.sets.definitions.fdn.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.MayPlayCardsFromExile
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Tinybones, Bauble Burglar
 * {1}{B}
 * Legendary Creature — Skeleton Rogue
 * 1/3
 *
 * Whenever an opponent discards a card, exile it from their graveyard with a stash counter on it.
 * During your turn, you may play cards you don't own with stash counters on them from exile, and
 * mana of any type can be spent to cast those spells.
 * {3}{B}, {T}: Each opponent discards a card. Activate only as a sorcery.
 *
 * Implementation:
 * - The discard trigger ([Triggers.AnyOpponentDiscards]) fires once per discarded card and binds
 *   that card as the triggering entity, so `EffectTarget.TriggeringEntity` is the card the discard
 *   put into the graveyard (CR 400.7e — a trigger can find the new object a card became in a public
 *   zone). `fromZone = GRAVEYARD` makes the exile a no-op if the card left the graveyard before the
 *   ability resolved (it would be a different object by then), and `addCounterType` puts the stash
 *   counter on it as it lands.
 * - The play permission is a [MayPlayCardsFromExile] static, not a per-card grant stamped at exile
 *   time. Per the ruling it covers every stash-countered card you don't own "regardless of whether
 *   they were put there by the Tinybones you currently control or a Tinybones that was previously on
 *   the battlefield", so it has to be a live filter over exile rather than a set of remembered cards.
 *   `ownedByOpponent()` is the engine's "not owned by you" predicate, and cards in exile carry an
 *   owner but no controller, so it reads correctly there.
 * - Nothing about the permission waives costs or timing: per the second ruling you pay all costs and
 *   follow normal timing rules, so a stash-countered land is playable only during your main phase
 *   with an empty stack, and a stash-countered creature only at sorcery speed. `withAnyManaType`
 *   relaxes only the colored requirements of spells cast through this permission (CR 118.14).
 * - The activated ability is a plain "each opponent discards a card", sorcery-timed. Its discards
 *   feed the first ability, which exiles them — Tinybones stealing what it made you throw away.
 */
val TinybonesBaubleBurglar = card("Tinybones, Bauble Burglar") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Legendary Creature — Skeleton Rogue"
    power = 1
    toughness = 3
    oracleText = "Whenever an opponent discards a card, exile it from their graveyard with a stash " +
        "counter on it.\n" +
        "During your turn, you may play cards you don't own with stash counters on them from exile, " +
        "and mana of any type can be spent to cast those spells.\n" +
        "{3}{B}, {T}: Each opponent discards a card. Activate only as a sorcery."

    triggeredAbility {
        trigger = Triggers.AnyOpponentDiscards
        effect = Effects.Exile(
            target = EffectTarget.TriggeringEntity,
            fromZone = Zone.GRAVEYARD,
            addCounterType = CounterType.STASH,
        )
    }

    staticAbility {
        ability = MayPlayCardsFromExile(
            filter = GameObjectFilter.Any.ownedByOpponent().withCounter(Counters.STASH),
            condition = Conditions.IsYourTurn,
            withAnyManaType = true,
        )
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{3}{B}"), Costs.Tap)
        effect = Effects.EachOpponentDiscards(1)
        timing = TimingRule.SorcerySpeed
        description = "Each opponent discards a card."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "72"
        artist = "Leonardo Santanna"
        imageUri = "https://cards.scryfall.io/normal/front/f/f/ff3d85bc-ef2d-4251-baf4-a14bd0cee61e.jpg?1783909107"

        ruling("2024-11-08", "Tinybones's second ability allows you to play all exiled cards you " +
            "don't own with stash counters on them, regardless of whether they were put there by " +
            "the Tinybones you currently control or a Tinybones that was previously on the battlefield.")
        ruling("2024-11-08", "You pay all costs and follow all normal timing rules for cards played " +
            "with the permission granted by Tinybones's second ability. For example, if the exiled " +
            "card is a land card, you may play it only during your main phase while the stack is empty.")
    }
}
