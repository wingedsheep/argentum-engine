package com.wingedsheep.mtg.sets.definitions.emn.cards

import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.MayCastWithoutPayingManaCost
import com.wingedsheep.sdk.scripting.effects.CreateDelayedTriggerEffect
import com.wingedsheep.sdk.scripting.effects.DelayedTriggerExpiry
import com.wingedsheep.sdk.scripting.effects.ForEachTargetEffect
import com.wingedsheep.sdk.scripting.effects.GrantKeywordEffect
import com.wingedsheep.sdk.scripting.events.DamageType
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetCreature
import com.wingedsheep.sdk.scripting.targets.TargetPermanent

/**
 * Tamiyo, Field Researcher — Eldritch Moon #190
 * {1}{G}{W}{U} · Legendary Planeswalker — Tamiyo · Starting loyalty 4
 *
 * +1: Choose up to two target creatures. Until your next turn, whenever either of those creatures
 *     deals combat damage, you draw a card.
 * −2: Tap up to two target nonland permanents. They don't untap during their controller's next
 *     untap step.
 * −7: Draw three cards. You get an emblem with "You may cast spells from your hand without paying
 *     their mana costs."
 *
 * Modeling notes:
 *
 *  - **"Up to two target …"** is one target requirement taking up to two objects
 *    (`count = 2, optional = true` → `minCount = 0`, the Smoldering Werewolf idiom), not two
 *    independent slots — so the picks must be different objects (CR 601.2c) and choosing zero is
 *    legal. Each payoff is then applied once per chosen object via [ForEachTargetEffect].
 *  - **The +1 sets up delayed triggers, not granted abilities.** Per the printed rulings the
 *    watcher "triggers even if Tamiyo has left the battlefield", and *you* draw the card, not the
 *    creature's controller — so this is Tamiyo's own delayed triggered ability, entity-scoped to
 *    each chosen creature via `watchedTarget`, and never an ability granted to those creatures.
 *    One trigger per creature, so two creatures dealing combat damage simultaneously draw two
 *    cards (also a printed ruling). `fireOnce = false` (the default) is what makes it *whenever*
 *    rather than *when … next*, and the new [DelayedTriggerExpiry.UntilControllersNextTurn] is the
 *    "Until your next turn" half — it survives the intervening opponents' turns and wears off on
 *    the post-untap hook of Tamiyo's controller's next turn, alongside
 *    [Duration.UntilYourNextTurn] effects.
 *  - **The −2** is the Crippling Chill pair (tap + [AbilityFlag.DOESNT_UNTAP] for
 *    [Duration.UntilAfterAffectedControllersNextUntap]) applied per target. The duration keys off
 *    each affected permanent's *own* controller, which is what "their controller's next untap
 *    step" means when the two targets are controlled by different players.
 *  - **The −7 emblem** carries wording that reads on its controller rather than on a group of
 *    permanents, so it goes in `ownedStaticAbilities` — the emblem *has* Omniscience's
 *    [MayCastWithoutPayingManaCost] exactly as a permanent printing it would, instead of the
 *    P/T-and-keyword group grant `CreatePermanentEmblem` otherwise models.
 */
val TamiyoFieldResearcher = card("Tamiyo, Field Researcher") {
    manaCost = "{1}{G}{W}{U}"
    colorIdentity = "GWU"
    typeLine = "Legendary Planeswalker — Tamiyo"
    startingLoyalty = 4
    oracleText = "+1: Choose up to two target creatures. Until your next turn, whenever either of " +
        "those creatures deals combat damage, you draw a card.\n" +
        "−2: Tap up to two target nonland permanents. They don't untap during their controller's " +
        "next untap step.\n" +
        "−7: Draw three cards. You get an emblem with \"You may cast spells from your hand " +
        "without paying their mana costs.\""

    // +1: Choose up to two target creatures. Until your next turn, whenever either of those
    //     creatures deals combat damage, you draw a card.
    loyaltyAbility(+1) {
        target("up to two target creatures", TargetCreature(count = 2, optional = true))
        effect = ForEachTargetEffect(
            listOf(
                CreateDelayedTriggerEffect(
                    effect = Effects.DrawCards(1),
                    trigger = Triggers.dealsDamage(damageType = DamageType.Combat),
                    watchedTarget = EffectTarget.ContextTarget(0),
                    expiry = DelayedTriggerExpiry.UntilControllersNextTurn
                )
            )
        )
        description = "Choose up to two target creatures. Until your next turn, whenever either " +
            "of those creatures deals combat damage, you draw a card."
    }

    // −2: Tap up to two target nonland permanents. They don't untap during their controller's
    //     next untap step.
    loyaltyAbility(-2) {
        target(
            "up to two target nonland permanents",
            TargetPermanent(
                count = 2,
                optional = true,
                filter = TargetFilter(GameObjectFilter.NonlandPermanent)
            )
        )
        effect = ForEachTargetEffect(
            listOf(
                Effects.Tap(EffectTarget.ContextTarget(0)),
                GrantKeywordEffect(
                    AbilityFlag.DOESNT_UNTAP.name,
                    EffectTarget.ContextTarget(0),
                    Duration.UntilAfterAffectedControllersNextUntap
                )
            )
        )
        description = "Tap up to two target nonland permanents. They don't untap during their " +
            "controller's next untap step."
    }

    // −7: Draw three cards. You get an emblem with "You may cast spells from your hand without
    //     paying their mana costs."
    loyaltyAbility(-7) {
        effect = Effects.DrawCards(3) then Effects.CreatePermanentEmblem(
            ownedStaticAbilities = listOf(MayCastWithoutPayingManaCost(controllerOnly = true)),
            emblemDescription = "You may cast spells from your hand without paying their mana costs."
        )
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "190"
        artist = "Tianhua X"
        imageUri = "https://cards.scryfall.io/normal/front/9/f/9f9aced7-9ae9-432f-b8c0-caac9cad098b.jpg?1783937424"

        ruling("2025-01-24", "Tamiyo's first ability can target creatures you don't control. You'll draw a card, not their controller, if they deal combat damage.")
        ruling("2025-01-24", "Tamiyo's first ability sets up a delayed triggered ability that triggers even if Tamiyo has left the battlefield before those creatures deal combat damage.")
        ruling("2025-01-24", "If Tamiyo's first ability targets two creatures, and both deal combat damage at the same time, the delayed triggered ability triggers twice.")
        ruling("2025-01-24", "If you cast a spell \"without paying its mana cost,\" you can't choose to cast it for any alternative costs. You can, however, pay additional costs. If the spell has any mandatory additional costs, those must be paid to cast the spell.")
        ruling("2025-01-24", "If a spell has {X} in its mana cost, you must choose 0 as the value of X when casting it without paying its mana cost.")
    }
}
