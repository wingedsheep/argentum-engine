package com.wingedsheep.mtg.sets.definitions.rtr.cards

import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.Chooser
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Guild Feud — Return to Ravnica #97
 * {5}{R} · Enchantment · Rare
 *
 * At the beginning of your upkeep, target opponent reveals the top three cards of their library,
 * may put a creature card from among them onto the battlefield, then puts the rest into their
 * graveyard. You do the same with the top three cards of your library. If two creatures are put
 * onto the battlefield this way, those creatures fight each other.
 *
 * One pipeline, run strictly in printed order: the targeted opponent ([Chooser.TargetPlayer] —
 * the trigger's only target) reveals and decides first, and their rest is binned before you
 * reveal your own three (the first ruling: you see their pick before you decide). Each put is
 * tracked with `moveTracked`, so the fight only happens when *both* moves actually landed a
 * creature — no creature or one creature means no fight. The fight runs inside the same
 * resolution, so nobody gets priority between the creatures entering and fighting; their
 * enters triggers go on the stack afterwards.
 */
val GuildFeud = card("Guild Feud") {
    manaCost = "{5}{R}"
    colorIdentity = "R"
    typeLine = "Enchantment"
    oracleText = "At the beginning of your upkeep, target opponent reveals the top three cards of " +
        "their library, may put a creature card from among them onto the battlefield, then puts the " +
        "rest into their graveyard. You do the same with the top three cards of your library. If two " +
        "creatures are put onto the battlefield this way, those creatures fight each other."

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.UPKEEP)
        target(Targets.Opponent)
        effect = Effects.Pipeline {
            val theirTop = gather(CardSource.TopOfLibrary(3, Player.TargetOpponent), revealed = true)
            val (theirPick, theirRest) = chooseUpToSplit(
                1,
                from = theirTop,
                chooser = Chooser.TargetPlayer,
                filter = GameObjectFilter.Creature,
                prompt = "You may put a creature card from among them onto the battlefield",
                selectedLabel = "Onto the battlefield",
                remainderLabel = "Into your graveyard",
                showAllCards = true
            )
            val theirCreature = moveTracked(
                theirPick,
                CardDestination.ToZone(Zone.BATTLEFIELD),
                underOwnersControl = true
            )
            move(theirRest, CardDestination.ToZone(Zone.GRAVEYARD))

            val yourTop = gather(CardSource.TopOfLibrary(3), revealed = true)
            val (yourPick, yourRest) = chooseUpToSplit(
                1,
                from = yourTop,
                filter = GameObjectFilter.Creature,
                prompt = "You may put a creature card from among them onto the battlefield",
                selectedLabel = "Onto the battlefield",
                remainderLabel = "Into your graveyard",
                showAllCards = true
            )
            val yourCreature = moveTracked(yourPick, CardDestination.ToZone(Zone.BATTLEFIELD))
            move(yourRest, CardDestination.ToZone(Zone.GRAVEYARD))

            ifNotEmpty(theirCreature) {
                ifNotEmpty(yourCreature) {
                    run(Effects.Fight(theirCreature.asTarget, yourCreature.asTarget))
                }
            }
        }
        description = "At the beginning of your upkeep, target opponent reveals the top three cards " +
            "of their library, may put a creature card from among them onto the battlefield, then puts " +
            "the rest into their graveyard. You do the same with the top three cards of your library. " +
            "If two creatures are put onto the battlefield this way, those creatures fight each other."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "97"
        artist = "Karl Kopinski"
        imageUri = "https://cards.scryfall.io/normal/front/8/e/8e622878-0aea-4401-873e-d34bf05ee98d.jpg?1783940355"
        ruling("2012-10-01", "Guild Feud has one target: the opponent. You'll see what creature that player puts onto the battlefield before revealing the top three cards of your library and deciding whether to put a creature onto the battlefield yourself.")
        ruling("2012-10-01", "If no creatures or only one creature is put onto the battlefield this way, no fight happens.")
        ruling("2012-10-01", "Players can't cast spells or activate abilities between the creatures being put onto the battlefield and them fighting each other. Notably, you can't respond with a spell like Giant Growth.")
        ruling("2012-10-01", "Any abilities that trigger when the two creatures enter will be put on the stack after the fight, even if one or both creatures die.")
    }
}
