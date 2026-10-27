package com.wingedsheep.mtg.sets.definitions.khm.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.DelayedTriggerExpiry
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Harald Unites the Elves — Kaldheim #213
 * {2}{B}{G} · Enchantment — Saga
 *
 * (As this Saga enters and after your draw step, add a lore counter. Sacrifice after III.)
 * I — Mill three cards. You may put an Elf or Tyvar card from your graveyard onto the battlefield.
 * II — Put a +1/+1 counter on each Elf you control.
 * III — Whenever an Elf you control attacks this turn, target creature an opponent controls gets
 * -1/-1 until end of turn.
 *
 * Chapter I is Tyvar, Jubilant Brawler's −2 shape: mill, then an "up to one" choice over the whole
 * graveyard (ruling: a card already there counts too). The choice is held to permanent cards: a
 * Kindred Elf instant or sorcery can never enter the battlefield (CR 400.4a), so offering it would be a
 * choice with no outcome. Chapter III installs a turn-bounded delayed
 * trigger that sees every Elf you control attack this turn, including Elves you gained after it
 * resolved, and picks a fresh target each time it fires.
 */
val HaraldUnitesTheElves = card("Harald Unites the Elves") {
    manaCost = "{2}{B}{G}"
    colorIdentity = "BG"
    typeLine = "Enchantment — Saga"
    oracleText = "(As this Saga enters and after your draw step, add a lore counter. Sacrifice after III.)\n" +
        "I — Mill three cards. You may put an Elf or Tyvar card from your graveyard onto the battlefield.\n" +
        "II — Put a +1/+1 counter on each Elf you control.\n" +
        "III — Whenever an Elf you control attacks this turn, target creature an opponent controls gets " +
        "-1/-1 until end of turn."

    sagaChapter(1) {
        effect = Effects.Pipeline {
            run(Patterns.Library.mill(3))
            val graveyard = gather(
                CardSource.FromZone(
                    Zone.GRAVEYARD,
                    Player.You,
                    GameObjectFilter.Permanent.withSubtype("Elf") or GameObjectFilter.Permanent.withSubtype("Tyvar")
                )
            )
            val chosen = chooseUpTo(
                1,
                from = graveyard,
                showAllCards = true,
                prompt = "You may put an Elf or Tyvar card from your graveyard onto the battlefield",
                selectedLabel = "Put onto the battlefield",
                remainderLabel = "Leave in graveyard"
            )
            move(chosen, CardDestination.ToZone(Zone.BATTLEFIELD))
        }
    }

    sagaChapter(2) {
        effect = Effects.ForEachInGroup(
            GroupFilter(GameObjectFilter.Creature.withSubtype("Elf").youControl()),
            Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.IterationEntity)
        )
    }

    sagaChapter(3) {
        effect = Effects.CreateDelayedTrigger(
            trigger = Triggers.a(GameObjectFilter.Creature.withSubtype("Elf").youControl()).attacks(),
            fireOnce = false,
            expiry = DelayedTriggerExpiry.EndOfTurn
        ) {
            val creature = target(TargetFilter.CreatureOpponentControls)
            effect = Effects.ModifyStats(-1, -1, creature)
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "213"
        artist = "Ryan Pancoast"
        imageUri = "https://cards.scryfall.io/normal/front/e/7/e7c5f681-0145-45e9-b943-ca9784cfdea0.jpg?1783928196"
        ruling(
            "2021-02-05",
            "For the chapter I ability, the Elf or Tyvar card you put onto the battlefield can be one of the " +
                "cards you just milled, or it may be one that was already in your graveyard."
        )
        ruling(
            "2021-02-05",
            "Only Elves you control as the chapter II ability resolves will get a +1/+1 counter. Elves that " +
                "come under your control later in the turn will not."
        )
        ruling(
            "2021-02-05",
            "Conversely, the triggered ability created by the chapter III ability will trigger whenever any " +
                "Elf you control attacks that turn, even if you didn't control that Elf as the chapter III " +
                "ability resolved."
        )
        ruling(
            "2021-02-05",
            "The triggered ability created by the chapter III ability resolves before blockers are declared. " +
                "It may remove opposing creatures before they have the chance to block."
        )
    }
}
