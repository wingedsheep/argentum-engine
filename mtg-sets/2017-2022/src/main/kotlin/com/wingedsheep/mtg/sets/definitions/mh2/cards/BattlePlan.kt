package com.wingedsheep.mtg.sets.definitions.mh2.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility

/**
 * Battle Plan — Modern Horizons 2 #114
 * {3}{R} · Enchantment
 *
 * At the beginning of combat on your turn, target creature you control gets +2/+0 until end of turn.
 * Basic landcycling {1}{R} ({1}{R}, Discard this card: Search your library for a basic land card, reveal it, put it into your hand, then shuffle.)
 *
 * [Triggers.BeginCombat] is the "at the beginning of combat on your turn" step trigger — a
 * `StepEvent(BEGIN_COMBAT, Player.You)` — so the enchantment fires only in its controller's combat
 * phase, once per turn. The target is chosen when the ability goes on the stack (CR 603.3d), so
 * `Targets.CreatureYouControl` is the target requirement rather than a filter read at resolution.
 *
 * Basic landcycling is the [KeywordAbility.basicLandcycling] flavour of cycling: the shared
 * typecycling machinery narrows the discard-and-search to *basic* land cards, so a nonbasic land
 * that merely has a basic land type is not a legal fetch.
 */
val BattlePlan = card("Battle Plan") {
    manaCost = "{3}{R}"
    colorIdentity = "R"
    typeLine = "Enchantment"
    oracleText = "At the beginning of combat on your turn, target creature you control gets +2/+0 until end of turn.\n" +
        "Basic landcycling {1}{R} ({1}{R}, Discard this card: Search your library for a basic land card, reveal it, put it into your hand, then shuffle.)"

    triggeredAbility {
        trigger = Triggers.BeginCombat
        val t = target("target creature you control", Targets.CreatureYouControl)
        effect = Effects.ModifyStats(2, 0, t)
    }

    keywordAbility(KeywordAbility.basicLandcycling("{1}{R}"))

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "114"
        artist = "Paul Scott Canavan"
        imageUri = "https://cards.scryfall.io/normal/front/2/b/2bc1e907-0e77-42ee-9eca-eae632020204.jpg?1783926850"
    }
}
