package com.wingedsheep.mtg.sets.definitions.znr.cards

import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Glacial Grasp — Zendikar Rising #59
 * {2}{U} · Instant
 *
 * Tap target creature. Its controller mills two cards. That creature doesn't untap during its
 * controller's next untap step.
 * Draw a card.
 *
 * Crippling Chill's tap + [AbilityFlag.DOESNT_UNTAP] construction with a mill in between; the
 * miller is [Player.ControllerOf] the target creature, resolved at resolution time.
 */
val GlacialGrasp = card("Glacial Grasp") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Tap target creature. Its controller mills two cards. That creature doesn't untap during its " +
        "controller's next untap step. (They put the top two cards of their library into their graveyard.)\n" +
        "Draw a card."

    spell {
        val creature = target(TargetFilter.Creature)
        effect = Effects.Tap(creature) then
            Patterns.Library.mill(2, EffectTarget.PlayerRef(Player.ControllerOf("target creature"))) then
            Effects.GrantKeyword(
                AbilityFlag.DOESNT_UNTAP,
                creature,
                Duration.UntilAfterAffectedControllersNextUntap()
            ) then
            Effects.DrawCards(1)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "59"
        artist = "Tomasz Jedruszek"
        imageUri = "https://cards.scryfall.io/normal/front/3/8/381d84d6-7b34-44cb-a697-54b835ba1281.jpg?1783929398"
        ruling(
            "2020-09-25",
            "If the target creature is an illegal target by the time Glacial Grasp tries to resolve, the spell " +
                "doesn't resolve. No player mills or draws any cards, and the creature won't be stopped from " +
                "untapping. If the target is legal but Glacial Grasp doesn't tap it (most likely because it's " +
                "already tapped), the rest of Glacial Grasp's effects still happen: that creature won't untap " +
                "during its controller's next untap step, that player mills two cards, and you draw a card.",
        )
    }
}
