package com.wingedsheep.mtg.sets.definitions.ths.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.TriggeredAbility
import com.wingedsheep.sdk.scripting.events.Recipient
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Warriors' Lesson — Theros #184
 * {G} · Instant
 *
 * Until end of turn, up to two target creatures you control each gain "Whenever this creature
 * deals combat damage to a player, draw a card."
 *
 * "Up to two" is one optional multi-target slot (`count = 2, optional = true`); [ForEachTarget]
 * grants the draw trigger to each chosen creature on its own (Open into Wonder shape), so the
 * trigger is bound to that creature and an illegal target simply misses out.
 */
val WarriorsLesson = card("Warriors' Lesson") {
    manaCost = "{G}"
    colorIdentity = "G"
    typeLine = "Instant"
    oracleText = "Until end of turn, up to two target creatures you control each gain " +
        "\"Whenever this creature deals combat damage to a player, draw a card.\""

    spell {
        targets(TargetFilter.CreatureYouControl, count = 2, optional = true)
        effect = Effects.ForEachTarget(
            Effects.GrantTriggeredAbility(
                ability = TriggeredAbility.create(
                    trigger = Triggers.self.dealsCombatDamage(Recipient.AnyPlayer),
                    effect = Effects.DrawCards(1),
                    descriptionOverride = "Whenever this creature deals combat damage to a player, draw a card.",
                ),
                target = EffectTarget.ContextTarget(0),
                duration = Duration.EndOfTurn,
            ),
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "184"
        artist = "Steve Prescott"
        flavorText = "\"Let each challenge make you a better warrior.\"\n—Anthousa of Setessa"
        imageUri = "https://cards.scryfall.io/normal/front/6/3/63f7e81a-6f9e-4cad-a911-fba7a431cd55.jpg?1783939734"
    }
}
