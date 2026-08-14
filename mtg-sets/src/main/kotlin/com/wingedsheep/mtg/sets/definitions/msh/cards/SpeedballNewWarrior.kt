package com.wingedsheep.mtg.sets.definitions.msh.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.RetargetChooser
import com.wingedsheep.sdk.scripting.events.SpellCastPredicate
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Speedball, New Warrior — Marvel Super Heroes #227
 * {2}{U/R} · Legendary Creature — Human Hero · 2/2
 *
 * Whenever a player casts a spell that targets Speedball, he gets +2/+2 until end of turn.
 * You may choose new targets for that spell.
 *
 * Modeling notes:
 *  - The trigger watches *every* seat (`Player.Each`), not just yours:
 *    [Triggers.anyPlayerCasts] with [SpellCastPredicate.TargetsSource] fires whenever any player's
 *    just-cast spell chose Speedball as one of its targets. `youCastSpellTargetingSource()` would
 *    have been the Legolas, Master Archer shape — wrong here, since the removal spell that makes
 *    this card interesting is the opponent's.
 *  - The triggering entity of a `SpellCastEvent` is the spell's stack object, so the retarget half
 *    is the plain [Effects.ChangeTriggeringObjectTargets] with [RetargetChooser.Controller] —
 *    *you* (Speedball's controller) get the choice, even when the opponent cast the spell.
 *    That effect walks the spell's target slots one at a time and always offers the current target
 *    among the options, which is exactly the printed "You **may** choose new targets": keeping
 *    every slot is a legal answer, so no separate yes/no gate is needed. Legality is judged from
 *    the *spell's* controller's perspective (CR 115.7b), which the shared retarget logic already
 *    enforces.
 *  - Order matters and follows the printed text: the +2/+2 is applied first, then the retarget
 *    decision pauses resolution. So the pump lands even if the spell (or Speedball) is gone by the
 *    time the retarget resolves, and a spell moved off Speedball still leaves him pumped — the
 *    trigger already fired and is independent of the spell (CR 603.1).
 */
val SpeedballNewWarrior = card("Speedball, New Warrior") {
    manaCost = "{2}{U/R}"
    colorIdentity = "UR"
    typeLine = "Legendary Creature — Human Hero"
    oracleText = "Whenever a player casts a spell that targets Speedball, he gets +2/+2 until " +
        "end of turn. You may choose new targets for that spell."
    power = 2
    toughness = 2

    triggeredAbility {
        trigger = Triggers.anyPlayerCasts(requires = setOf(SpellCastPredicate.TargetsSource))
        effect = Effects.ModifyStats(2, 2, EffectTarget.Self) then
            Effects.ChangeTriggeringObjectTargets(RetargetChooser.Controller)
        description = "Whenever a player casts a spell that targets Speedball, he gets +2/+2 " +
            "until end of turn. You may choose new targets for that spell."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "227"
        artist = "Borja Pindado"
        flavorText = "\"Ah, to be a young and carefree Super Hero! Not a worry in the world for " +
            "this spandex savior! I wish!\""
        imageUri = "https://cards.scryfall.io/normal/front/e/0/e040b456-9853-4e94-9bf8-9374888168bb.jpg?1783902897"
    }
}
