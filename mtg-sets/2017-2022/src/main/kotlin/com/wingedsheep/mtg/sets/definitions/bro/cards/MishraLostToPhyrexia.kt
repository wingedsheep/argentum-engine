package com.wingedsheep.mtg.sets.definitions.bro.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.mode
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.effects.Effect
import com.wingedsheep.sdk.scripting.effects.Mode
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Mishra, Lost to Phyrexia — The Brothers' War #163b (meld result)
 * Legendary Artifact Creature — Phyrexian Artificer · 9/9 · black and red (colour indicator)
 *
 * Whenever Mishra enters or attacks, choose three —
 * • Target opponent discards two cards.
 * • Mishra deals 3 damage to any target.
 * • Destroy target artifact or planeswalker.
 * • Creatures you control gain menace and trample until end of turn.
 * • Creatures you don't control get -1/-1 until end of turn.
 * • Create two tapped Powerstone tokens.
 *
 * Melded from Mishra, Claimed by Gix and Phyrexian Dragon Engine (Mishra's attack trigger). It
 * enters tapped and attacking but was never declared as an attacker, so only the enters half of
 * the trigger fires on the meld turn. "Creatures you don't control" uses the house
 * opponents-control group, the same as every other "you don't control" sweep.
 */
val MishraLostToPhyrexia = card("Mishra, Lost to Phyrexia") {
    manaCost = ""
    meldOf("Mishra, Claimed by Gix", "Phyrexian Dragon Engine")
    colorIndicator = "BR"
    colorIdentity = "BR"
    typeLine = "Legendary Artifact Creature — Phyrexian Artificer"
    power = 9
    toughness = 9
    oracleText = "Whenever Mishra enters or attacks, choose three —\n" +
        "• Target opponent discards two cards.\n" +
        "• Mishra deals 3 damage to any target.\n" +
        "• Destroy target artifact or planeswalker.\n" +
        "• Creatures you control gain menace and trample until end of turn.\n" +
        "• Creatures you don't control get -1/-1 until end of turn.\n" +
        "• Create two tapped Powerstone tokens."

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = chooseThree()
        description = TRIGGER_TEXT
    }

    triggeredAbility {
        trigger = Triggers.self.attacks()
        effect = chooseThree()
        description = TRIGGER_TEXT
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "163b"
        artist = "Chris Rahn"
        imageUri = "https://cards.scryfall.io/normal/front/0/2/0220c1e9-07bc-44f0-b39e-ca345ec4ea28.jpg?1783920054"
    }
}

private const val TRIGGER_TEXT = "Whenever Mishra enters or attacks, choose three."

private fun chooseThree(): Effect = Effects.Modal(
    listOf(
        mode("Target opponent discards two cards.") {
            val opponent = target(Targets.Opponent)
            effect = Effects.Discard(2, opponent)
        },
        mode("Mishra deals 3 damage to any target.") {
            val any = target(Targets.Any)
            effect = Effects.DealDamage(3, any)
        },
        mode("Destroy target artifact or planeswalker.") {
            val permanent = target(TargetFilter(GameObjectFilter.Artifact or GameObjectFilter.Planeswalker))
            effect = Effects.Destroy(permanent)
        },
        Mode.noTarget(
            Patterns.Group.grantKeywordToAll(Keyword.MENACE, Filters.Group.creaturesYouControl) then
                Patterns.Group.grantKeywordToAll(Keyword.TRAMPLE, Filters.Group.creaturesYouControl),
            "Creatures you control gain menace and trample until end of turn."
        ),
        Mode.noTarget(
            Patterns.Group.modifyStatsForAll(-1, -1, GroupFilter.AllCreaturesOpponentsControl, Duration.EndOfTurn),
            "Creatures you don't control get -1/-1 until end of turn."
        ),
        Mode.noTarget(Effects.CreatePowerstone(count = 2, tapped = true), "Create two tapped Powerstone tokens."),
    ),
    chooseCount = 3,
)
