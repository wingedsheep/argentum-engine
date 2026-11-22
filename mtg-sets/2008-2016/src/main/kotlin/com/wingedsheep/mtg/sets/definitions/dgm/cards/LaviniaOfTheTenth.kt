package com.wingedsheep.mtg.sets.definitions.dgm.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.PreventActivatedAbilities
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Lavinia of the Tenth — Dragon's Maze #80
 * {3}{W}{U} · Legendary Creature — Human Soldier · 4/4
 *
 * Protection from red
 * When Lavinia enters, detain each nonland permanent your opponents control with mana value 4 or
 * less. (Until your next turn, those permanents can't attack or block and their activated
 * abilities can't be activated.)
 *
 * Detain is spelled out per permanent, the same until-your-next-turn lock Lyev Decree applies,
 * looped over the group. The group is snapshotted at resolution, so permanents an opponent gains
 * later are untouched, and a detained permanent stays detained if it changes controller.
 */
val LaviniaOfTheTenth = card("Lavinia of the Tenth") {
    manaCost = "{3}{W}{U}"
    colorIdentity = "WU"
    typeLine = "Legendary Creature — Human Soldier"
    power = 4
    toughness = 4
    oracleText = "Protection from red\n" +
        "When Lavinia enters, detain each nonland permanent your opponents control with mana value 4 " +
        "or less. (Until your next turn, those permanents can't attack or block and their activated " +
        "abilities can't be activated.)"

    keywordAbility(KeywordAbility.protectionFrom(Color.RED))

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.ForEachInGroup(
            GroupFilter(GameObjectFilter.NonlandPermanent.opponentControls().manaValueAtMost(4)),
            Effects.CantAttackOrBlock(EffectTarget.IterationEntity, Duration.UntilYourNextTurn) then
                Effects.GrantStaticAbility(
                    ability = PreventActivatedAbilities(GameObjectFilter.Permanent.sourceItself()),
                    target = EffectTarget.IterationEntity,
                    duration = Duration.UntilYourNextTurn,
                )
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "80"
        artist = "Willian Murai"
        imageUri = "https://cards.scryfall.io/normal/front/8/1/813f1967-c048-4e6e-9720-216773fde47e.jpg?1783940026"
        ruling(
            "2013-04-15",
            "Activated abilities include a colon and are written in the form \"[cost]: [effect].\" No one can activate any activated abilities, including mana abilities, of a detained permanent."
        )
        ruling("2013-04-15", "The static abilities of a detained permanent still apply. The triggered abilities of a detained permanent can still trigger.")
        ruling("2013-04-15", "If a creature is already attacking or blocking when it's detained, it won't be removed from combat. It will continue to attack or block.")
        ruling("2013-04-15", "If a permanent's activated ability is on the stack when that permanent is detained, the ability will be unaffected.")
        ruling("2013-04-15", "If a noncreature permanent is detained and later turns into a creature, it won't be able to attack or block.")
        ruling("2013-04-15", "The mana value of a creature token is 0 unless that token is a copy of another creature, in which case it copies that creature's mana cost.")
        ruling(
            "2013-04-15",
            "Lavinia's ability doesn't affect permanents an opponent gains control of after its enters-the-battlefield ability has resolved. Similarly, if you gain control of a detained permanent after that ability resolves, it will continue to be detained until your next turn."
        )
    }
}
