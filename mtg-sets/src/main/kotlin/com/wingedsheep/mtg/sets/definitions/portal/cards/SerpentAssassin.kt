package com.wingedsheep.mtg.sets.definitions.portal.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.DestroyEffect
import com.wingedsheep.sdk.scripting.EffectTarget
import com.wingedsheep.sdk.scripting.OnEnterBattlefield
import com.wingedsheep.sdk.scripting.TargetFilter
import com.wingedsheep.sdk.targeting.TargetCreature

/**
 * Serpent Assassin
 * {3}{B}{B}
 * Creature — Snake Assassin
 * 2/2
 * When Serpent Assassin enters the battlefield, you may destroy target nonblack creature.
 */
val SerpentAssassin = card("Serpent Assassin") {
    manaCost = "{3}{B}{B}"
    typeLine = "Creature — Snake Assassin"
    power = 2
    toughness = 2

    triggeredAbility {
        trigger = OnEnterBattlefield()
        optional = true
        target = TargetCreature(unifiedFilter = TargetFilter.Creature.notColor(Color.BLACK))
        effect = DestroyEffect(EffectTarget.ContextTarget(0))
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "108"
        artist = "Ron Spencer"
        flavorText = "Swift, silent, and utterly lethal."
        imageUri = "https://cards.scryfall.io/normal/front/1/0/1018f6ff-5eaa-4fe1-ba20-544df799f5b2.jpg"
    }
}
