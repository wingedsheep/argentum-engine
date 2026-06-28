package com.wingedsheep.mtg.sets.definitions.fin.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TriggerBinding
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.effects.MayPlayExpiry
import com.wingedsheep.sdk.scripting.events.AttackPredicate
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Seifer Almasy — Final Fantasy #156
 * {3}{R} · Legendary Creature — Human Knight · 3/4
 *
 * Whenever a creature you control attacks alone, it gains double strike until end of turn.
 * Fire Cross — Whenever Seifer Almasy deals combat damage to a player, you may cast target
 * instant or sorcery card with mana value 3 or less from your graveyard without paying its
 * mana cost. If that spell would be put into your graveyard, exile it instead.
 *
 * First ability uses the Thoughtweft Imbuer "attacks alone" shape: an ANY-bound
 * [Triggers.attacks] with [AttackPredicate.Alone] over "creature you control", granting the
 * keyword to [EffectTarget.TriggeringEntity] (the lone attacker).
 *
 * "Fire Cross" is an ability word (CR 207.2c) — flavor only, no rules meaning, so it adds no
 * keyword. The second ability mirrors Quistis Trepe's targeted graveyard cast: target an
 * instant/sorcery in *your* graveyard with mana value ≤ 3, move it to exile, gather it into a
 * collection, then grant a may-play-from-exile permission with `exileAfterResolve = true`
 * (the "if it would be put into a graveyard, exile it instead" rider) paired with
 * [Effects.GrantPlayWithoutPayingCost] (the "without paying its mana cost" clause, where
 * Quistis instead lets any mana type pay). The "you may" is honored by the granted permission
 * never being forced.
 */
val SeiferAlmasy = card("Seifer Almasy") {
    manaCost = "{3}{R}"
    colorIdentity = "R"
    typeLine = "Legendary Creature — Human Knight"
    power = 3
    toughness = 4
    oracleText = "Whenever a creature you control attacks alone, it gains double strike until end of turn.\n" +
        "Fire Cross — Whenever Seifer Almasy deals combat damage to a player, you may cast target instant " +
        "or sorcery card with mana value 3 or less from your graveyard without paying its mana cost. If " +
        "that spell would be put into your graveyard, exile it instead."

    triggeredAbility {
        trigger = Triggers.attacks(
            filter = GameObjectFilter.Creature.youControl(),
            requires = setOf(AttackPredicate.Alone),
            binding = TriggerBinding.ANY,
        )
        effect = Effects.GrantKeyword(
            Keyword.DOUBLE_STRIKE,
            EffectTarget.TriggeringEntity,
            Duration.EndOfTurn,
        )
        description = "Whenever a creature you control attacks alone, it gains double strike until end of turn."
    }

    triggeredAbility {
        trigger = Triggers.DealsCombatDamageToPlayer
        target = TargetObject(
            filter = TargetFilter.InstantOrSorceryInYourGraveyard.manaValueAtMost(3),
        )
        effect = Effects.Composite(
            // Exile the targeted card from your graveyard so the free-cast grant keys off it.
            Effects.Move(EffectTarget.ContextTarget(0), Zone.EXILE),
            GatherCardsEffect(source = CardSource.ChosenTargets, storeAs = "fireCross"),
            // "You may cast ..." + "if it would be put into your graveyard, exile it instead."
            Effects.GrantMayPlayFromExile(
                from = "fireCross",
                expiry = MayPlayExpiry.EndOfTurn,
                exileAfterResolve = true,
            ),
            // "... without paying its mana cost."
            Effects.GrantPlayWithoutPayingCost("fireCross"),
        )
        description = "Fire Cross — Whenever Seifer Almasy deals combat damage to a player, you may cast " +
            "target instant or sorcery card with mana value 3 or less from your graveyard without paying " +
            "its mana cost. If that spell would be put into your graveyard, exile it instead."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "156"
        artist = "Kotetsu Kinoshita"
        imageUri = "https://cards.scryfall.io/normal/front/9/c/9c776984-99ea-4181-ac95-78c41ba9d54f.jpg?1748706341"
    }
}
