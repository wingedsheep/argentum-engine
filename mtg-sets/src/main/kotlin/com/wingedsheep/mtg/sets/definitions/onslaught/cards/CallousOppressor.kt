package com.wingedsheep.mtg.sets.definitions.onslaught.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.EffectTarget
import com.wingedsheep.sdk.scripting.EntersWithCreatureTypeChoice
import com.wingedsheep.sdk.scripting.GainControlEffect
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TargetFilter
import com.wingedsheep.sdk.targeting.TargetCreature

/**
 * Callous Oppressor
 * {1}{U}{U}
 * Creature — Cephalid
 * 1/2
 * You may choose not to untap Callous Oppressor during your untap step.
 * As Callous Oppressor enters the battlefield, an opponent chooses a creature type.
 * {T}: Gain control of target creature that isn't of the chosen type for as long as
 * Callous Oppressor remains tapped.
 */
val CallousOppressor = card("Callous Oppressor") {
    manaCost = "{1}{U}{U}"
    typeLine = "Creature — Cephalid"
    power = 1
    toughness = 2
    oracleText = "You may choose not to untap Callous Oppressor during your untap step.\nAs Callous Oppressor enters the battlefield, an opponent chooses a creature type.\n{T}: Gain control of target creature that isn't of the chosen type for as long as Callous Oppressor remains tapped."

    keywords(Keyword.MAY_NOT_UNTAP)
    replacementEffect(EntersWithCreatureTypeChoice(opponentChooses = true))

    activatedAbility {
        cost = Costs.Tap
        target = TargetCreature(
            filter = TargetFilter(GameObjectFilter.Creature.notOfSourceChosenType())
        )
        effect = GainControlEffect(EffectTarget.ContextTarget(0), Duration.WhileSourceTapped())
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "72"
        artist = "Todd Lockwood"
        imageUri = "https://cards.scryfall.io/large/front/b/3/b3dd3ce7-e0e3-4412-9983-ff933584f59b.jpg?1562937464"
    }
}
