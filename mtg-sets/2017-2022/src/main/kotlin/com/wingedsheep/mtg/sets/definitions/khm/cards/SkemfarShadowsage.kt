package com.wingedsheep.mtg.sets.definitions.khm.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.ModalEffect
import com.wingedsheep.sdk.scripting.effects.Mode
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Skemfar Shadowsage
 * {3}{B}
 * Creature — Elf Cleric
 * 2/5
 * When this creature enters, choose one —
 * • Each opponent loses X life, where X is the greatest number of creatures you control that have a
 *   creature type in common.
 * • You gain X life, where X is the greatest number of creatures you control that have a creature
 *   type in common.
 *
 * The mode is chosen as the trigger goes on the stack; X is read on resolution
 * ([DynamicAmounts.largestSharedCreatureTypeCount], projected — Shadowsage counts itself if it is
 * still under your control).
 */
val SkemfarShadowsage = card("Skemfar Shadowsage") {
    manaCost = "{3}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Elf Cleric"
    oracleText = "When this creature enters, choose one —\n" +
        "• Each opponent loses X life, where X is the greatest number of creatures you control that have a creature type in common.\n" +
        "• You gain X life, where X is the greatest number of creatures you control that have a creature type in common."
    power = 2
    toughness = 5

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = ModalEffect.chooseOne(
            Mode.noTarget(
                Effects.LoseLife(
                    DynamicAmounts.largestSharedCreatureTypeCount(),
                    EffectTarget.PlayerRef(Player.EachOpponent)
                ),
                "Each opponent loses X life, where X is the greatest number of creatures you control that have a creature type in common"
            ),
            Mode.noTarget(
                Effects.GainLife(DynamicAmounts.largestSharedCreatureTypeCount()),
                "You gain X life, where X is the greatest number of creatures you control that have a creature type in common"
            ),
            countsAsModalSpell = false
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "110"
        artist = "Matt Stewart"
        imageUri = "https://cards.scryfall.io/normal/front/c/2/c204130f-0483-49ed-8512-03a74894702e.jpg?1783928240"
        ruling("2021-02-05", "You choose which mode the triggered ability has as you put that ability on the stack. You can't change modes later.")
        ruling("2021-02-05", "The amount of life lost or gained is calculated as the triggered ability resolves based on the creatures you control at that time, including Skemfar Shadowsage itself if it's still under your control. You'll always use the greatest number possible. You don't need to choose any creature types. For example, if you control a Giant Wizard, a Giant Warrior, an Elf Warrior, and a Kor Warrior at that time, each opponent will lose 3 life or you'll gain 3 life.")
    }
}
