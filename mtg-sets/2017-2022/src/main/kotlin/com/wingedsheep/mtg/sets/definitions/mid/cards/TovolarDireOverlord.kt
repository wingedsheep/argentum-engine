package com.wingedsheep.mtg.sets.definitions.mid.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.daybound
import com.wingedsheep.sdk.dsl.nightbound
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.Chooser
import com.wingedsheep.sdk.scripting.events.Recipient
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Tovolar, Dire Overlord // Tovolar, the Midnight Scourge (Innistrad: Midnight Hunt)
 * {1}{R}{G}
 * Legendary Creature — Human Werewolf // Legendary Creature — Werewolf
 *
 * Front (3/3): Whenever a Wolf or Werewolf you control deals combat damage to a player, draw a card.
 * At the beginning of your upkeep, if you control three or more Wolves and/or Werewolves, it becomes
 * night. Then transform any number of Human Werewolves you control. Daybound.
 *
 * Back (4/4): Whenever a Wolf or Werewolf you control deals combat damage to a player, draw a card.
 * {X}{R}{G}: Target Wolf or Werewolf you control gets +X/+0 and gains trample until end of turn.
 * Nightbound.
 *
 * Implementation:
 *  - The draw trigger is per-source ("whenever a Wolf or Werewolf"), so three Wolves connecting
 *    trigger three times; a Wolf Werewolf matches the any-of-subtypes filter once.
 *  - The upkeep trigger is an intervening "if" (re-checked on resolution). [Effects.BecomeNight]
 *    runs the daybound/nightbound transform cascade in the same call, so by the time the "then
 *    transform" step gathers, every daybound Human Werewolf has already flipped to its (non-Human)
 *    nightbound face. What's left to choose from is exactly the older Innistrad werewolves the
 *    ruling describes; `TransformEffect` itself refuses daybound/nightbound permanents (CR 702.145).
 *    The selection happens on the battlefield, and choosing none is legal.
 */
private val wolfOrWerewolf = GameObjectFilter.Permanent.withAnySubtype("Wolf", "Werewolf")

private const val DRAW_TRIGGER_TEXT =
    "Whenever a Wolf or Werewolf you control deals combat damage to a player, draw a card."

private val TovolarDireOverlordFront = card("Tovolar, Dire Overlord") {
    manaCost = "{1}{R}{G}"
    colorIdentity = "RG"
    typeLine = "Legendary Creature — Human Werewolf"
    power = 3
    toughness = 3
    oracleText = "$DRAW_TRIGGER_TEXT\n" +
        "At the beginning of your upkeep, if you control three or more Wolves and/or Werewolves, " +
        "it becomes night. Then transform any number of Human Werewolves you control.\n" +
        "Daybound (If a player casts no spells during their own turn, it becomes night next turn.)"

    triggeredAbility {
        trigger = Triggers.a(wolfOrWerewolf.youControl()).dealsCombatDamage(Recipient.AnyPlayer)
        effect = Effects.DrawCards(1)
        description = DRAW_TRIGGER_TEXT
    }

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.UPKEEP)
        interveningIf = Conditions.YouControlAtLeast(3, wolfOrWerewolf)
        effect = Effects.Pipeline(
            descriptionOverride = "It becomes night. Then transform any number of Human Werewolves you control."
        ) {
            run(Effects.BecomeNight)
            val humanWerewolves = gather(
                CardSource.ControlledPermanents(
                    Player.You,
                    GameObjectFilter.Permanent.withSubtype("Human").withSubtype("Werewolf"),
                )
            )
            val chosen = chooseAnyNumber(
                from = humanWerewolves,
                chooser = Chooser.Controller,
                useTargetingUI = true,
                prompt = "Transform any number of Human Werewolves you control",
            )
            run(Effects.ForEachInCollection(chosen, Effects.Transform(EffectTarget.IterationEntity)))
        }
        description = "At the beginning of your upkeep, if you control three or more Wolves and/or " +
            "Werewolves, it becomes night. Then transform any number of Human Werewolves you control."
    }

    daybound()

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "246"
        artist = "Chris Rahn"
        imageUri = "https://cards.scryfall.io/normal/front/f/9/f953fad3-0cd1-48aa-8ed9-d7d2e293e6e2.jpg?1783925557"

        ruling("2021-09-24", "The second ability of Tovolar, Dire Overlord seems a little strange because Tovolar and other Human Werewolf creatures in this set transform when it becomes night anyway. Tovolar, however, isn't content to transform Werewolf creatures from just one set. This ability will allow you to transform Human Werewolf creatures from previous visits to Innistrad as well.")
        ruling("2021-09-24", "If a creature you control that's a Wolf and a Werewolf deals combat damage to a player, the first ability triggers only once.")
        ruling("2021-09-24", "The first ability of each of Tovolar's faces triggers once for each Wolf or Werewolf you control that deals combat damage to a player. For example, if you attack with three Wolf creatures and they all deal combat damage to a player, the ability triggers three times.")
        ruling("2021-09-24", "Permanents with daybound and nightbound can't transform via any means other than their daybound and nightbound abilities. Notably, older cards such as Moonmist that instruct a player to transform permanents don't affect permanents with daybound or nightbound.")
    }
}

private val TovolarTheMidnightScourge = card("Tovolar, the Midnight Scourge") {
    manaCost = ""
    colorIdentity = "RG"
    colorIndicator = "RG" // Transformed back face, no mana cost (CR 204).
    typeLine = "Legendary Creature — Werewolf"
    power = 4
    toughness = 4
    oracleText = "$DRAW_TRIGGER_TEXT\n" +
        "{X}{R}{G}: Target Wolf or Werewolf you control gets +X/+0 and gains trample until end of turn.\n" +
        "Nightbound (If a player casts at least two spells during their own turn, it becomes day next turn.)"

    triggeredAbility {
        trigger = Triggers.a(wolfOrWerewolf.youControl()).dealsCombatDamage(Recipient.AnyPlayer)
        effect = Effects.DrawCards(1)
        description = DRAW_TRIGGER_TEXT
    }

    activatedAbility {
        cost = Costs.Mana("{X}{R}{G}")
        val pumped = target(TargetFilter.PermanentYouControl.withAnySubtype("Wolf", "Werewolf"))
        effect = Effects.ModifyStats(DynamicAmounts.xValue(), DynamicAmounts.fixed(0), pumped) then
            Effects.GrantKeyword(Keyword.TRAMPLE, pumped)
        description = "{X}{R}{G}: Target Wolf or Werewolf you control gets +X/+0 and gains trample until end of turn."
    }

    nightbound()

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "246"
        artist = "Chris Rahn"
        imageUri = "https://cards.scryfall.io/normal/back/f/9/f953fad3-0cd1-48aa-8ed9-d7d2e293e6e2.jpg?1783925557"
    }
}

val TovolarDireOverlord: CardDefinition = CardDefinition.doubleFacedCreature(
    frontFace = TovolarDireOverlordFront,
    backFace = TovolarTheMidnightScourge,
)
