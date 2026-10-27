package com.wingedsheep.mtg.sets.definitions.afr.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Volo, Guide to Monsters (AFR #238)
 * {2}{G}{U}  Legendary Creature — Human Wizard  3/2
 *
 * Whenever you cast a creature spell that doesn't share a creature type with a creature you
 * control or a creature card in your graveyard, copy that spell.
 *
 * The "doesn't share" clause is a trigger-time gate (not an intervening "if"), so it is a
 * [triggerRestriction]: no creature you control and no creature card in your graveyard
 * shares a creature type with the triggering spell. A Changeling spell has every creature type on
 * the stack (CR 702.73a), so it always shares one with Volo himself and is never copied.
 */
val VoloGuideToMonsters = card("Volo, Guide to Monsters") {
    manaCost = "{2}{G}{U}"
    colorIdentity = "GU"
    typeLine = "Legendary Creature — Human Wizard"
    power = 3
    toughness = 2
    oracleText = "Whenever you cast a creature spell that doesn't share a creature type with a creature you " +
        "control or a creature card in your graveyard, copy that spell. (A copy of a creature spell becomes a token.)"

    triggeredAbility {
        trigger = Triggers.you.casts(GameObjectFilter.Creature)
        triggerRestriction = Conditions.All(
            Conditions.YouControl(
                GameObjectFilter.Creature.sharingCreatureTypeWith(EffectTarget.TriggeringEntity),
                negate = true
            ),
            Conditions.Not(
                Conditions.GraveyardContains(
                    GameObjectFilter.Creature.sharingCreatureTypeWith(EffectTarget.TriggeringEntity)
                )
            )
        )
        effect = Effects.CopyTargetSpell(target = EffectTarget.TriggeringEntity, mayChooseNewTargets = false)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "238"
        artist = "Zoltan Boros"
        flavorText = "\"The beholder told me I had a good eye for details. Who was I to argue?\""
        imageUri = "https://cards.scryfall.io/normal/front/c/9/c9ae01f9-7461-47b4-aa1e-93bd6ff1bf9e.jpg?1783926441"
    }
}
