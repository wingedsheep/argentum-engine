package com.wingedsheep.mtg.sets.definitions.mh3.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.Mode
import com.wingedsheep.sdk.scripting.effects.ModalEffect
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Voltstorm Angel
 * {3}{W}{W}
 * Creature — Angel
 * 4/4
 *
 * Flying
 * When this creature enters, you get {E}{E}{E} (three energy counters).
 * At the beginning of combat on your turn, you may pay {E}{E}. When you do, choose one —
 * • This creature gains vigilance and lifelink until end of turn.
 * • Other creatures you control get +1/+1 until end of turn.
 *
 * The combat ability is a reflexive trigger (CR 603.12), same shape as Hylda of the Icy Crown:
 * the optional {E}{E} payment is the action, and the "choose one —" is a *top-level*
 * [ModalEffect] payload, so its mode is chosen as the reflexive ability is put on the stack
 * (a modal triggered ability announces its mode then, not on resolution). The prompt is skipped
 * when fewer than two energy are available. The +1/+1 mode locks its affected set on resolution
 * (CR 611.2c) via [Effects.ForEachInGroup] — creatures entering later don't get the bonus.
 */
val VoltstormAngel = card("Voltstorm Angel") {
    manaCost = "{3}{W}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Angel"
    power = 4
    toughness = 4
    oracleText = "Flying\nWhen this creature enters, you get {E}{E}{E} (three energy counters).\n" +
        "At the beginning of combat on your turn, you may pay {E}{E}. When you do, choose one —\n" +
        "• This creature gains vigilance and lifelink until end of turn.\n" +
        "• Other creatures you control get +1/+1 until end of turn."

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.GetEnergy(3)
    }

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.BEGIN_COMBAT)
        effect = Effects.ReflexiveTrigger(
            action = Effects.PayExactCounters(CounterType.ENERGY, 2),
            optional = true,
            reflexiveEffect = ModalEffect.chooseOne(
                Mode.noTarget(
                    Effects.GrantKeyword(Keyword.VIGILANCE, EffectTarget.Self, Duration.EndOfTurn) then
                        Effects.GrantKeyword(Keyword.LIFELINK, EffectTarget.Self, Duration.EndOfTurn),
                    "This creature gains vigilance and lifelink until end of turn"
                ),
                Mode.noTarget(
                    Effects.ForEachInGroup(
                        GroupFilter(GameObjectFilter.Creature.youControl(), excludeSelf = true),
                        Effects.ModifyStats(1, 1, EffectTarget.IterationEntity)
                    ),
                    "Other creatures you control get +1/+1 until end of turn"
                ),
            ),
            descriptionOverride = "You may pay {E}{E}. When you do, choose one — this creature " +
                "gains vigilance and lifelink until end of turn; or other creatures you control " +
                "get +1/+1 until end of turn."
        )
        description = "At the beginning of combat on your turn, you may pay {E}{E}. When you do, " +
            "choose one — this creature gains vigilance and lifelink until end of turn; or other " +
            "creatures you control get +1/+1 until end of turn."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "46"
        artist = "Andrew Theophilopoulos"
        imageUri = "https://cards.scryfall.io/normal/front/b/5/b5495f34-423e-4012-808c-b267e6fabf2c.jpg?1783911295"
    }
}
