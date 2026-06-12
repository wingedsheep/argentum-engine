package com.wingedsheep.mtg.sets.definitions.tdm.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetCreature

/**
 * Mardu Siegebreaker — Tarkir: Dragonstorm #206
 * {1}{R}{W}{B} · Creature — Human Warrior · Rare
 * 4/4
 *
 * Deathtouch, haste
 * When this creature enters, exile up to one other target creature you control until this
 * creature leaves the battlefield.
 * Whenever this creature attacks, for each opponent, create a tapped token that's a copy of the
 * exiled card attacking that opponent. At the beginning of your next end step, sacrifice those tokens.
 *
 * Modeling:
 * - The ETB exile reuses the linked-exile machinery ([Effects.ExileUntilLeaves] + a
 *   LeavesBattlefield trigger returning the linked card). "Up to one other ... you control" is an
 *   optional target over [TargetFilter.OtherCreatureYouControl].
 * - The attack trigger gathers the linked-exiled card and creates a tapped + attacking token copy
 *   of it. The engine is a two-player approximation, so "for each opponent" yields the single
 *   opponent: one token attacking that opponent (defender resolved via the `attacking` flag).
 *   If the exiled pile is empty (the ETB target was declined, or a token was exiled and ceased to
 *   exist), the gather yields nothing and no copy is created — matching the printed rulings.
 * - "At the beginning of your next end step, sacrifice those tokens" is the token copy's
 *   `sacrificeAtStep = Step.END` with `sacrificeOnlyOnControllersTurn = true` (your end step).
 */
val MarduSiegebreaker = card("Mardu Siegebreaker") {
    manaCost = "{1}{R}{W}{B}"
    colorIdentity = "RWB"
    typeLine = "Creature — Human Warrior"
    power = 4
    toughness = 4
    oracleText = "Deathtouch, haste\n" +
        "When this creature enters, exile up to one other target creature you control until this " +
        "creature leaves the battlefield.\n" +
        "Whenever this creature attacks, for each opponent, create a tapped token that's a copy of " +
        "the exiled card attacking that opponent. At the beginning of your next end step, sacrifice those tokens."

    keywords(Keyword.DEATHTOUCH, Keyword.HASTE)

    // ETB: exile up to one other target creature you control until this leaves the battlefield.
    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        val creature = target(
            "up to one other target creature you control",
            TargetCreature(count = 1, optional = true, filter = TargetFilter.OtherCreatureYouControl)
        )
        effect = Effects.ExileUntilLeaves(creature)
    }

    // LTB: return the exiled card to its owner's control.
    triggeredAbility {
        trigger = Triggers.LeavesBattlefield
        effect = Effects.ReturnLinkedExileUnderOwnersControl()
    }

    // Attacks: create a tapped, attacking token copy of the exiled card; sacrifice it at your next end step.
    triggeredAbility {
        trigger = Triggers.Attacks
        effect = Effects.Pipeline {
            gather(CardSource.FromLinkedExile(), name = "exiledCard")
            run(
                Effects.CreateTokenCopyOfTarget(
                    target = EffectTarget.PipelineTarget("exiledCard"),
                    tapped = true,
                    attacking = true,
                    sacrificeAtStep = Step.END,
                    sacrificeOnlyOnControllersTurn = true
                )
            )
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "206"
        artist = "Chris Seaman"
        imageUri = "https://cards.scryfall.io/normal/front/3/0/3044b232-edf4-4000-9273-cc4653ad653a.jpg?1743204809"

        ruling("2025-04-04", "If the exiled card isn't a creature card, the token copies will still be created and they'll still enter tapped. They just won't be attacking.")
        ruling("2025-04-04", "Although the tokens enter attacking, they were never declared as attacking creatures. Abilities that trigger whenever a creature attacks won't trigger when those tokens enter attacking.")
        ruling("2025-04-04", "If Mardu Siegebreaker leaves the battlefield before its \"enters\" ability resolves, the target creature won't be exiled.")
        ruling("2025-04-04", "If a token is exiled with Mardu Siegebreaker's third ability, it will cease to exist and won't return to the battlefield. Mardu Siegebreaker's last ability won't create copies of that token.")
        ruling("2025-04-04", "The token copies what's printed on the exiled card and nothing else. It doesn't copy any effects that changed its characteristics before it was exiled.")
        ruling("2025-04-04", "Any \"enters\" abilities of the copied card will trigger when the tokens enter.")
    }
}
