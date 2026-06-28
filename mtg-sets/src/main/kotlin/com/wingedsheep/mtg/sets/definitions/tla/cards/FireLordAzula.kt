package com.wingedsheep.mtg.sets.definitions.tla.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.firebending
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CopyTargetSpellEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Fire Lord Azula
 * {1}{U}{B}{R}
 * Legendary Creature — Human Noble
 * 4/4
 *
 * Firebending 2 (Whenever this creature attacks, add {R}{R}. This mana lasts until end of combat.)
 * Whenever you cast a spell while Fire Lord Azula is attacking, copy that spell. You may choose new
 * targets for the copy. (A copy of a permanent spell becomes a token.)
 *
 * `firebending(2)` is the set keyword combat-mana helper (fixed amount). The copy payoff is a
 * `youCastSpell` trigger gated by the intervening-if `Conditions.SourceIsAttacking` so it only fires
 * while Fire Lord Azula is attacking, copying the just-cast spell via `CopyTargetSpellEffect` on the
 * `TriggeringEntity` — the same shape as Storm of Saruman, whose default behaviour already offers the
 * "you may choose new targets for the copy" prompt and turns a copied permanent spell into a token.
 */
val FireLordAzula = card("Fire Lord Azula") {
    manaCost = "{1}{U}{B}{R}"
    colorIdentity = "UBR"
    typeLine = "Legendary Creature — Human Noble"
    power = 4
    toughness = 4
    oracleText = "Firebending 2 (Whenever this creature attacks, add {R}{R}. This mana lasts until end of combat.)\n" +
        "Whenever you cast a spell while Fire Lord Azula is attacking, copy that spell. You may choose " +
        "new targets for the copy. (A copy of a permanent spell becomes a token.)"

    firebending(2)

    triggeredAbility {
        trigger = Triggers.youCastSpell()
        triggerCondition = Conditions.SourceIsAttacking
        effect = CopyTargetSpellEffect(target = EffectTarget.TriggeringEntity)
        description = "Whenever you cast a spell while Fire Lord Azula is attacking, copy that spell. " +
            "You may choose new targets for the copy."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "220"
        artist = "Fahmi Fauzi"
        flavorText = "\"I'm sorry it has to end this way, brother.\""
        imageUri = "https://cards.scryfall.io/normal/front/b/c/bc6146bf-f0c6-4557-af6a-74c643d5fc01.jpg?1764121593"
    }
}
