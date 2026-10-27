package com.wingedsheep.mtg.sets.definitions.sos.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Social Snub — Secrets of Strixhaven #228
 * {1}{W}{B} · Sorcery
 *
 * When you cast this spell while you control a creature, you may copy this spell.
 * Each player sacrifices a creature of their choice. Each opponent loses 1 life and you gain
 * 1 life.
 *
 * The intervening-if cast trigger (CR 603.4) fires from the stack via
 * `Triggers.self.isCast()` with `triggerRestriction = Conditions.ControlCreature`; its
 * [Effects.May] optionally copies the spell with [Effects.CopyTargetSpell] of the triggering entity
 * (a copy isn't cast, CR 707.10, so it doesn't re-trigger). Resolution is an edict —
 * [Effects.Sacrifice] of a creature for each player (`Player.Each`, each chooses their own) — then
 * each opponent loses 1 life and the controller gains 1.
 */
val SocialSnub = card("Social Snub") {
    manaCost = "{1}{W}{B}"
    colorIdentity = "WB"
    typeLine = "Sorcery"
    oracleText = "When you cast this spell while you control a creature, you may copy this spell.\n" +
        "Each player sacrifices a creature of their choice. Each opponent loses 1 life and you " +
        "gain 1 life."

    triggeredAbility {
        trigger = Triggers.self.isCast()
        triggerRestriction = Conditions.ControlCreature
        effect = Effects.May(Effects.CopyTargetSpell(target = EffectTarget.TriggeringEntity, mayChooseNewTargets = false))
        description = "When you cast this spell while you control a creature, you may copy this spell."
    }

    spell {
        effect = Effects.Sacrifice(
            filter = GameObjectFilter.Creature,
            target = EffectTarget.PlayerRef(Player.Each),
        ) then
            Effects.LoseLife(1, EffectTarget.PlayerRef(Player.EachOpponent)) then
            Effects.GainLife(1)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "228"
        artist = "Raluca Marinescu"
        flavorText = "\"Sorry. This seat's taken.\""
        imageUri = "https://cards.scryfall.io/normal/front/a/0/a04b6900-0436-4920-a0d4-c0186d605ae3.jpg?1775938590"
    }
}
