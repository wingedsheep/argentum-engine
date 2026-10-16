package com.wingedsheep.mtg.sets.definitions.snc.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.CollectionSlot
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.grantedTriggeredAbility
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Jaxis, the Troublemaker — Streets of New Capenna #112
 * {3}{R} · Legendary Creature — Human Warrior · 2/3
 *
 * {R}, {T}, Discard a card: Create a token that's a copy of another target creature you control.
 * It gains haste and "When this token dies, draw a card." Sacrifice it at the beginning of the
 * next end step. Activate only as a sorcery.
 * Blitz {1}{R}
 *
 * "It *gains*" is not an "except" clause: neither haste nor the dies-draw trigger is a copiable
 * value (ruling — a copy of the token has neither). So haste is a permanent granted keyword over
 * the created tokens rather than `addedKeywords`, and the draw trigger rides in
 * `triggeredAbilities`, which the copy executor installs as an object grant. Every token a
 * replacement effect adds is covered by both and by the end-step sacrifice.
 */
val JaxisTheTroublemaker = card("Jaxis, the Troublemaker") {
    manaCost = "{3}{R}"
    colorIdentity = "R"
    typeLine = "Legendary Creature — Human Warrior"
    power = 2
    toughness = 3
    oracleText = "{R}, {T}, Discard a card: Create a token that's a copy of another target creature you control. " +
        "It gains haste and \"When this token dies, draw a card.\" Sacrifice it at the beginning of the next " +
        "end step. Activate only as a sorcery.\n" +
        "Blitz {1}{R} (If you cast this spell for its blitz cost, it gains haste and \"When this creature dies, " +
        "draw a card.\" Sacrifice it at the beginning of the next end step.)"

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{R}"), Costs.Tap, Costs.Discard())
        timing = TimingRule.SorcerySpeed
        val creature = target(TargetFilter(GameObjectFilter.Creature.youControl()).other())
        effect = Effects.CreateTokenCopyOfTarget(
            target = creature,
            triggeredAbilities = listOf(
                grantedTriggeredAbility {
                    trigger = Triggers.self.dies()
                    effect = Effects.DrawCards(1)
                    description = "When this token dies, draw a card."
                }
            ),
            sacrificeAtStep = Step.END,
        ) then Effects.ForEachInCollection(
            CollectionSlot.CreatedTokens,
            Effects.GrantKeyword(Keyword.HASTE, EffectTarget.IterationEntity, Duration.Permanent)
        )
        description = "{R}, {T}, Discard a card: Create a token that's a copy of another target creature you " +
            "control. It gains haste and \"When this token dies, draw a card.\" Sacrifice it at the beginning " +
            "of the next end step. Activate only as a sorcery."
    }

    keywordAbility(KeywordAbility.blitz("{1}{R}"))

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "112"
        artist = "Zoltan Boros"
        imageUri = "https://cards.scryfall.io/normal/front/7/8/78127c0c-672f-4e4b-9c23-6a5f237228fd.jpg?1783923117"
        ruling("2022-04-29", "The token copies exactly what was printed on the original creature and nothing else (unless it's copying a creature that's a token or that's copying something else). It doesn't copy whether the creature is tapped or untapped, whether it has any counters on it or Auras and/or Equipment attached to it, or any non-copy effects that changed its power, toughness, types, color, and so on. Most notably, if the target creature isn't normally a creature, the copy won't be a creature.")
        ruling("2022-04-29", "If another creature becomes a copy of, or enters the battlefield as a copy of, the token, that creature will copy the creature card the token is copying. However, it won't have either haste or \"When this creature dies, draw a card,\" and you won't sacrifice the new copy at the beginning of the next end step.")
        ruling("2022-04-29", "If Jaxis, the Troublemaker's ability creates multiple tokens due to a replacement effect (such as the one Doubling Season creates), you'll sacrifice each of them at the beginning of the next end step.")
    }
}
