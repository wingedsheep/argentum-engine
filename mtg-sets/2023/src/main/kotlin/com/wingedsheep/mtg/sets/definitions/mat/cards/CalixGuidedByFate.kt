package com.wingedsheep.mtg.sets.definitions.mat.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.events.Recipient
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.predicates.StatePredicate
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Calix, Guided by Fate — March of the Machine: The Aftermath #26
 * {1}{G}{W} · Legendary Enchantment Creature — Human Druid · 2/2
 *
 * Constellation — Whenever Calix or another enchantment you control enters, put a +1/+1 counter
 * on target creature.
 * Whenever Calix or an enchanted creature you control deals combat damage to a player, you may
 * create a token that's a copy of a nonlegendary enchantment you control. Do this only once each
 * turn.
 *
 * Calix is itself an enchantment, so the constellation line is one unbound watcher over
 * enchantments you control (`Triggers.a`), firing for Calix's own entry too (Eidolon of Blossoms).
 *
 * The combat-damage line is one ability over "Calix or an enchanted creature you control": a
 * per-creature watcher over creatures you control, narrowed at trigger time (CR 603.2
 * `triggerRestriction`) to a damage source that is Calix itself or is enchanted. One ability, not
 * a self trigger plus an observer, so an enchanted Calix triggers it once and both halves share
 * the single "once each turn" budget. "Do this only once each turn" is `effectOncePerTurn`
 * (CR 603.2h): it is spent only when the token is actually created, so declining one instance
 * keeps a later one live, and once used later instances resolve with no effect (Scryfall ruling).
 * The enchantment is chosen on resolution, untargeted (`CreateTokenCopyOfChosenPermanent`, scoped
 * to permanents you control by its executor).
 */
val CalixGuidedByFate = card("Calix, Guided by Fate") {
    manaCost = "{1}{G}{W}"
    colorIdentity = "GW"
    typeLine = "Legendary Enchantment Creature — Human Druid"
    power = 2
    toughness = 2
    oracleText = "Constellation — Whenever Calix or another enchantment you control enters, put a " +
        "+1/+1 counter on target creature.\n" +
        "Whenever Calix or an enchanted creature you control deals combat damage to a player, you " +
        "may create a token that's a copy of a nonlegendary enchantment you control. Do this only " +
        "once each turn."

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Enchantment.youControl()).enters()
        val creature = target(TargetFilter.Creature)
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, creature)
        description = "Constellation — Whenever Calix or another enchantment you control enters, " +
            "put a +1/+1 counter on target creature."
    }

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Creature.youControl()).dealsCombatDamage(Recipient.AnyPlayer)
        triggerRestriction = Conditions.EntityMatches(
            EffectTarget.TriggeringEntity,
            GameObjectFilter.Creature.withStatePredicate(
                StatePredicate.Or(listOf(StatePredicate.IsSource, StatePredicate.IsEnchanted))
            )
        )
        effectOncePerTurn = true
        effect = Effects.May(
            Effects.CreateTokenCopyOfChosenPermanent(GameObjectFilter.Enchantment.nonlegendary())
        )
        description = "Whenever Calix or an enchanted creature you control deals combat damage to a " +
            "player, you may create a token that's a copy of a nonlegendary enchantment you control. " +
            "Do this only once each turn."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "26"
        artist = "PINDURSKI"
        imageUri = "https://cards.scryfall.io/normal/front/0/a/0a707ab3-b9b5-422a-8b0f-e963d3ad6606.jpg?1783916516"
        ruling(
            "2023-05-12",
            "Calix may be the target of its own constellation ability. This includes when Calix itself " +
                "enters the battlefield."
        )
        ruling(
            "2023-05-12",
            "Calix's last ability doesn't target any permanent. You choose which nonlegendary enchantment " +
                "you control you're creating a token copy of, if any, as the ability resolves."
        )
        ruling(
            "2023-05-12",
            "Once you choose to create a token using the last ability, that ability won't trigger again " +
                "that turn. Additional instances of the ability that are already on the stack will resolve " +
                "but will have no effect."
        )
    }
}
