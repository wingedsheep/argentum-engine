package com.wingedsheep.mtg.sets.definitions.bro.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Arms Race
 * {3}{R}
 * Enchantment
 * {3}{R}: You may put an artifact card from your hand onto the battlefield. That artifact gains
 * haste. Sacrifice it at the beginning of the next end step.
 *
 * The Through the Breach shape over artifact cards: the `chooseUpTo(1)` selection carries the
 * "you may", and the rider only runs when something was put onto the battlefield. The haste grant
 * is [Duration.Permanent] — the oracle text says only "gains haste", with no "until end of turn".
 */
val ArmsRace = card("Arms Race") {
    manaCost = "{3}{R}"
    colorIdentity = "R"
    typeLine = "Enchantment"
    oracleText = "{3}{R}: You may put an artifact card from your hand onto the battlefield. " +
        "That artifact gains haste. Sacrifice it at the beginning of the next end step."

    activatedAbility {
        cost = Costs.Mana("{3}{R}")
        effect = Effects.Pipeline {
            val candidates = gather(CardSource.FromZone(Zone.HAND, Player.You, GameObjectFilter.Artifact))
            val putting = chooseUpTo(
                1,
                from = candidates,
                prompt = "Put an artifact card onto the battlefield"
            )
            move(putting, CardDestination.ToZone(Zone.BATTLEFIELD, Player.You))
            ifNotEmpty(putting) {
                run(Effects.GrantKeyword(
                    keyword = Keyword.HASTE,
                    target = putting.asTarget,
                    duration = Duration.Permanent
                ))
                run(Effects.CreateDelayedTrigger(
                    step = Step.END,
                    effect = Effects.SacrificeTarget(putting.asTarget)
                ))
            }
        }
        description = "Put an artifact card from your hand onto the battlefield. It gains haste. " +
            "Sacrifice it at the beginning of the next end step."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "126"
        artist = "Leesha Hannigan"
        flavorText = "\"You want it good, fast, or cheap? You can't have all three.\""
        imageUri = "https://cards.scryfall.io/normal/front/c/d/cd83b2c3-bb06-42db-83bf-8903a126512c.jpg?1783920074"
    }
}
