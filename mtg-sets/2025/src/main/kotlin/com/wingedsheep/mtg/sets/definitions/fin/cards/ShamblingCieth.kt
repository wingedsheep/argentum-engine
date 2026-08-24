package com.wingedsheep.mtg.sets.definitions.fin.cards

import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersTapped
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.MayPayManaEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Shambling Cie'th
 * {2}{B}
 * Creature — Mutant Horror
 * 3/3
 * This creature enters tapped.
 * Whenever you cast a noncreature spell, you may pay {B}. If you do, return this card from your
 * graveyard to your hand.
 *
 * The recursion ability functions from the graveyard via `triggerZone = Zone.GRAVEYARD`
 * (MayPayMana), the same shape as Invasion's Pyre Zombie.
 */
val ShamblingCieth = card("Shambling Cie'th") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Mutant Horror"
    oracleText = "This creature enters tapped.\nWhenever you cast a noncreature spell, you may pay {B}. If you do, return this card from your graveyard to your hand."
    power = 3
    toughness = 3

    replacementEffect(EntersTapped())

    triggeredAbility {
        trigger = Triggers.youCastSpell(spellFilter = GameObjectFilter.Noncreature)
        triggerZone = Zone.GRAVEYARD
        effect = MayPayManaEffect(
            cost = ManaCost.parse("{B}"),
            effect = Effects.ReturnToHandFromGraveyard(EffectTarget.Self),
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "117"
        artist = "Nottsuo"
        flavorText = "Cie'th are damned to wander the world unliving and undying, until their corrupted flesh at last can move no more."
        imageUri = "https://cards.scryfall.io/normal/front/f/0/f02ce338-4fe2-44b0-a896-3ed7e6c874a3.jpg?1748706199"
    }
}
