package com.wingedsheep.mtg.sets.definitions.sos.cards

import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CreateDelayedTriggerEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetCreature

/**
 * Conciliator's Duelist
 * {W}{W}{B}{B}
 * Creature — Kor Warlock
 * 4/3
 * When this creature enters, draw a card. Each player loses 1 life.
 * Repartee — Whenever you cast an instant or sorcery spell that targets a creature, exile up to
 * one target creature. Return that card to the battlefield under its owner's control at the
 * beginning of the next end step.
 *
 * "Repartee" is an ability word (flavor only). The Repartee trigger is the standard
 * `youCastSpell(InstantOrSorcery)` narrowed by `targetsMatching(Creature)` shared with the other
 * SOS Repartee cards. The exile/return-at-next-end-step body is the Salvation Swan shape: exile
 * up to one target creature, then a `CreateDelayedTriggerEffect(Step.END, ...)` returns that card
 * to the battlefield (under its owner's control, the default for a `Move` to battlefield).
 */
val ConciliatorsDuelist = card("Conciliator's Duelist") {
    manaCost = "{W}{W}{B}{B}"
    colorIdentity = "WB"
    typeLine = "Creature — Kor Warlock"
    power = 4
    toughness = 3
    oracleText = "When this creature enters, draw a card. Each player loses 1 life.\n" +
        "Repartee — Whenever you cast an instant or sorcery spell that targets a creature, exile " +
        "up to one target creature. Return that card to the battlefield under its owner's control " +
        "at the beginning of the next end step."

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = Effects.Composite(
            listOf(
                Effects.DrawCards(1, EffectTarget.Controller),
                Effects.LoseLife(1, EffectTarget.PlayerRef(Player.Each)),
            )
        )
    }

    triggeredAbility {
        trigger = Triggers.youCastSpell(
            spellFilter = GameObjectFilter.InstantOrSorcery.targetsMatching(GameObjectFilter.Creature)
        )

        val creature = target(
            "target creature",
            TargetCreature(optional = true, filter = TargetFilter.Creature),
        )

        effect = Effects.Composite(
            listOf(
                Effects.Move(creature, Zone.EXILE),
                CreateDelayedTriggerEffect(
                    step = Step.END,
                    effect = Effects.Move(creature, Zone.BATTLEFIELD),
                ),
            )
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "182"
        artist = "Andrew Mar"
        flavorText = "\"By all means, let's debate.\""
        imageUri = "https://cards.scryfall.io/normal/front/e/2/e225929b-6197-4550-969e-3c4a97206a68.jpg?1775938257"
    }
}
