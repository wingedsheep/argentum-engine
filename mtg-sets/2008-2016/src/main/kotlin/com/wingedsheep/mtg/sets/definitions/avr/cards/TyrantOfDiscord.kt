package com.wingedsheep.mtg.sets.definitions.avr.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.MoveType
import com.wingedsheep.sdk.scripting.effects.RepeatCondition

/**
 * Tyrant of Discord
 * {4}{R}{R}{R}
 * Creature — Elemental
 * 7/7
 * When this creature enters, target opponent chooses a permanent they control at random and
 * sacrifices it. If a nonland permanent is sacrificed this way, repeat this process.
 *
 * A do-while loop ([Effects.RepeatWhile]), the Sin, Spira's Punishment shape. Each pass gathers the
 * opponent's permanents, picks one at random, notes whether it is a nonland permanent *before* it
 * leaves (last-known information — an animated land is still a land), then sacrifices it, tracking
 * what actually moved. The loop repeats only when both hold for this pass: something was sacrificed
 * (a permanent that can't be sacrificed, or an opponent who can't sacrifice — Sigarda, Host of
 * Herons — ends the ability per the rulings) and the pick was nonland. The target is chosen once;
 * every pass reads the same opponent. An opponent with no permanents left ends the loop (empty
 * pick, nothing sacrificed).
 */
val TyrantOfDiscord = card("Tyrant of Discord") {
    manaCost = "{4}{R}{R}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Elemental"
    power = 7
    toughness = 7
    oracleText = "When this creature enters, target opponent chooses a permanent they control at random " +
        "and sacrifices it. If a nonland permanent is sacrificed this way, repeat this process."

    triggeredAbility {
        trigger = Triggers.self.enters()
        val opponent = target(Targets.Opponent)
        effect = Effects.RepeatWhile(
            body = Effects.Pipeline {
                val theirs = gather(CardSource.ControlledPermanents(opponent.asPlayer, GameObjectFilter.Permanent))
                val picked = chooseRandom(1, from = theirs)
                selectAll(from = picked, filter = GameObjectFilter.Nonland, name = "tyrantNonlandPick")
                moveTracked(
                    picked,
                    CardDestination.ToZone(Zone.GRAVEYARD),
                    moveType = MoveType.Sacrifice,
                    name = "tyrantSacrificed",
                )
            },
            repeatCondition = RepeatCondition.WhileCondition(
                Conditions.All(
                    Conditions.CollectionContainsMatch("tyrantSacrificed"),
                    Conditions.CollectionContainsMatch("tyrantNonlandPick"),
                )
            ),
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "162"
        artist = "Richard Wright"
        imageUri = "https://cards.scryfall.io/normal/front/e/8/e8f7c8ea-e9c1-4d78-972c-15c4014915a0.jpg?1783940673"
        ruling(
            "2012-05-01",
            "Repeating the process includes the instruction to repeat the process. That is, the opponent " +
                "will keep sacrificing permanents until they sacrifice a land."
        )
        ruling(
            "2012-05-01",
            "Repeating the process does not include targeting an opponent. Only one opponent will have to " +
                "sacrifice permanents."
        )
        ruling(
            "2012-05-01",
            "If any abilities trigger while Tyrant of Discord's \"enters\" ability is resolving, those " +
                "abilities will wait until the \"enters\" ability finishes resolving before going on the " +
                "stack. Starting with the active player, each player puts their abilities on the stack in " +
                "any order."
        )
        ruling(
            "2012-05-01",
            "If the target opponent can't sacrifice permanents (perhaps because of Sigarda, Host of " +
                "Herons), the opponent will choose a permanent at random but not sacrifice it. The ability " +
                "will then finish resolving."
        )
    }
}
