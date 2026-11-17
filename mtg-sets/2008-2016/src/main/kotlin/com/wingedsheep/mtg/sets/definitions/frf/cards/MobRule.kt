package com.wingedsheep.mtg.sets.definitions.frf.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.effects.ModalEffect
import com.wingedsheep.sdk.scripting.effects.Mode
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Mob Rule
 * {4}{R}{R}
 * Sorcery
 * Choose one —
 * • Gain control of all creatures with power 4 or greater until end of turn. Untap those creatures.
 *   They gain haste until end of turn.
 * • Gain control of all creatures with power 3 or less until end of turn. Untap those creatures.
 *   They gain haste until end of turn.
 *
 * Each mode snapshots its group once and runs gain-control, untap and haste on every member, so
 * "those creatures" stays the set whose power qualified at resolution even if gaining control
 * changes a creature's power (an anthem the caster controls).
 */
private fun mobRuleMode(filter: GroupFilter) = Effects.ForEachInGroup(
    filter,
    Effects.GainControl(EffectTarget.IterationEntity, Duration.EndOfTurn) then
        Effects.Untap(EffectTarget.IterationEntity) then
        Effects.GrantKeyword(Keyword.HASTE, EffectTarget.IterationEntity, Duration.EndOfTurn)
)

val MobRule = card("Mob Rule") {
    manaCost = "{4}{R}{R}"
    colorIdentity = "R"
    typeLine = "Sorcery"
    oracleText = "Choose one —\n" +
        "• Gain control of all creatures with power 4 or greater until end of turn. Untap those creatures. They gain haste until end of turn.\n" +
        "• Gain control of all creatures with power 3 or less until end of turn. Untap those creatures. They gain haste until end of turn."

    spell {
        effect = ModalEffect.chooseOne(
            Mode.noTarget(
                mobRuleMode(GroupFilter(GameObjectFilter.Creature.powerAtLeast(4))),
                "Gain control of all creatures with power 4 or greater until end of turn",
            ),
            Mode.noTarget(
                mobRuleMode(GroupFilter(GameObjectFilter.Creature.powerAtMost(3))),
                "Gain control of all creatures with power 3 or less until end of turn",
            ),
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "109"
        artist = "Jakub Kasper"
        imageUri = "https://cards.scryfall.io/normal/front/1/d/1d76845f-c827-4cb4-b2d0-3f2cf5ee0e81.jpg?1783938686"
        ruling(
            "2014-11-24",
            "You choose which mode you're using as you cast Mob Rule. Once it's cast, you can't change its mode, " +
                "even if creatures' powers change in response."
        )
        ruling("2014-11-24", "Once you gain control of a creature, it doesn't matter what happens to its power.")
        ruling(
            "2014-11-24",
            "Mob Rule can affect creatures you already control or ones that are already untapped. It will untap " +
                "them if applicable and give them haste."
        )
        ruling(
            "2014-11-24",
            "Gaining control of a creature doesn't cause you to gain control of any Auras or Equipment attached to it."
        )
    }
}
