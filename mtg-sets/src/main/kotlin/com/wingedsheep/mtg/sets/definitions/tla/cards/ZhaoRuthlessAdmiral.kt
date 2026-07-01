package com.wingedsheep.mtg.sets.definitions.tla.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.firebending
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.ModifyStatsEffect
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Zhao, Ruthless Admiral
 * {2}{B/R}{B/R}
 * Legendary Creature — Human Soldier
 * 3/4
 *
 * Firebending 2 (Whenever this creature attacks, add {R}{R}. This mana lasts until end of combat.)
 * Whenever you sacrifice another permanent, creatures you control get +1/+0 until end of turn.
 *
 * `firebending(2)` is the Avatar set keyword helper (CR 702.189): an attack-triggered combat-duration
 * "add {R}{R}" ability ([com.wingedsheep.sdk.dsl.firebendingAttackTrigger]).
 *
 * The sacrifice payoff is the per-permanent [Triggers.YouSacrificeAnother] (OTHER binding) over any
 * permanent — the Mazirek/Savra template: it fires once for EACH permanent sacrificed, even when
 * several are sacrificed simultaneously (CR 603.2c), so sacrificing three permanents pumps the team
 * +3/+0. "Another" excludes Zhao sacrificing itself; when Zhao is sacrificed alongside other
 * permanents it still reacts to those others, pumping only the creatures you control that remain.
 * The team pump is a [Effects.ForEachInGroup] over [GroupFilter.AllCreaturesYouControl] applying a
 * +1/+0 [ModifyStatsEffect] to each ([EffectTarget.Self] = current iteration creature) until end of
 * turn.
 */
val ZhaoRuthlessAdmiral = card("Zhao, Ruthless Admiral") {
    manaCost = "{2}{B/R}{B/R}"
    colorIdentity = "BR"
    typeLine = "Legendary Creature — Human Soldier"
    power = 3
    toughness = 4
    oracleText = "Firebending 2 (Whenever this creature attacks, add {R}{R}. This mana lasts until end of combat.)\n" +
        "Whenever you sacrifice another permanent, creatures you control get +1/+0 until end of turn."

    firebending(2)

    // Whenever you sacrifice another permanent, creatures you control get +1/+0 until end of turn.
    triggeredAbility {
        trigger = Triggers.YouSacrificeAnother(GameObjectFilter.Permanent)
        effect = Effects.ForEachInGroup(
            filter = GroupFilter.AllCreaturesYouControl,
            effect = ModifyStatsEffect(
                powerModifier = 1,
                toughnessModifier = 0,
                target = EffectTarget.Self,
                duration = Duration.EndOfTurn
            )
        )
        description = "Whenever you sacrifice another permanent, creatures you control get +1/+0 until end of turn."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "252"
        artist = "Yuumei"
        flavorText = "\"We are the sons and daughters of fire, the superior element!\""
        imageUri = "https://cards.scryfall.io/normal/front/3/f/3fd48a57-b0bb-4177-a0f3-bd317a179cbe.jpg?1764121862"
    }
}
