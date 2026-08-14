package com.wingedsheep.mtg.sets.definitions.woe.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Territorial Witchstalker
 * {1}{G}
 * Creature — Wolf
 * 2/3
 *
 * Defender
 * At the beginning of combat on your turn, if you control a creature with power 4 or greater,
 * this creature gets +1/+0 until end of turn and can attack this turn as though it didn't have
 * defender.
 *
 * The power check is an intervening-if (CR 603.4): it's tested both when the trigger would go on
 * the stack and again on resolution, so losing the big creature in between fizzles the pump. The
 * filter isn't `excludeSelf` — a Witchstalker already pumped to 4 power satisfies its own
 * condition. Keeping the defender and granting a one-turn bypass (rather than removing the
 * keyword) is the faithful shape: the Wolf still can't attack on a later turn, and effects that
 * care about it having defender still see it.
 */
val TerritorialWitchstalker = card("Territorial Witchstalker") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Wolf"
    power = 2
    toughness = 3
    oracleText = "Defender\n" +
        "At the beginning of combat on your turn, if you control a creature with power 4 or greater, " +
        "this creature gets +1/+0 until end of turn and can attack this turn as though it didn't have defender."

    keywords(Keyword.DEFENDER)

    triggeredAbility {
        trigger = Triggers.BeginCombat
        triggerCondition = Conditions.YouControl(GameObjectFilter.Creature.powerAtLeast(4))
        effect = Effects.ModifyStats(1, 0, EffectTarget.Self) then
            Effects.CanAttackDespiteDefenderThisTurn(EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "189"
        artist = "Ilse Gort"
        imageUri = "https://cards.scryfall.io/normal/front/2/5/2515d53d-7a50-4da3-980d-91d91fea2020.jpg?1783915076"
    }
}
