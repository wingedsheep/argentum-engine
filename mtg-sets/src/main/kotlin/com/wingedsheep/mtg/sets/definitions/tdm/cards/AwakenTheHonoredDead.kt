package com.wingedsheep.mtg.sets.definitions.tdm.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.ReflexiveTriggerEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject
import com.wingedsheep.sdk.dsl.LibraryPatterns

/**
 * Awaken the Honored Dead — Tarkir: Dragonstorm #170
 * {B}{G}{U} · Enchantment — Saga
 *
 * (As this Saga enters and after your draw step, add a lore counter. Sacrifice after III.)
 * I — Destroy target nonland permanent.
 * II — Mill three cards.
 * III — You may discard a card. When you do, return target creature or land card from your
 *       graveyard to your hand.
 *
 * Chapter III is a reflexive trigger: no target is chosen when the chapter ability triggers.
 * Discarding a card triggers a second ability whose target is chosen as it goes on the stack,
 * so the just-discarded card is itself a legal target.
 */
val AwakenTheHonoredDead = card("Awaken the Honored Dead") {
    manaCost = "{B}{G}{U}"
    colorIdentity = "BGU"
    typeLine = "Enchantment — Saga"
    oracleText = "(As this Saga enters and after your draw step, add a lore counter. Sacrifice after III.)\n" +
        "I — Destroy target nonland permanent.\n" +
        "II — Mill three cards.\n" +
        "III — You may discard a card. When you do, return target creature or land card from your graveyard to your hand."

    sagaChapter(1) {
        val permanent = target("nonland permanent", Targets.NonlandPermanent)
        effect = Effects.Destroy(permanent)
    }

    sagaChapter(2) {
        effect = LibraryPatterns.mill(3)
    }

    sagaChapter(3) {
        effect = ReflexiveTriggerEffect(
            action = Effects.Discard(1),
            optional = true,
            reflexiveEffect = Effects.ReturnToHand(EffectTarget.ContextTarget(0)),
            reflexiveTargetRequirements = listOf(
                TargetObject(
                    filter = TargetFilter(
                        GameObjectFilter.CreatureOrLand.ownedByYou(),
                        zone = Zone.GRAVEYARD
                    )
                )
            )
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "170"
        artist = "Clint Lockwood"
        imageUri = "https://cards.scryfall.io/normal/front/1/4/14078a49-2230-4ad7-aea0-0c253813c646.jpg?1743204653"
        ruling("2025-04-04", "You don't choose a target for Awaken the Honored Dead's final chapter ability at the time it triggers. Rather, a second \"reflexive\" ability triggers when you discard a card this way. You choose a target for that ability as it goes on the stack. You may choose the card you just discarded as the target or a card that was already in your graveyard. Each player may respond to this triggered ability as normal.")
    }
}
