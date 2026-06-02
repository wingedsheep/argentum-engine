package com.wingedsheep.mtg.sets.definitions.tdm.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.ReflexiveTriggerEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Kishla Trawlers — Tarkir: Dragonstorm #50
 * {2}{U} · Creature — Human Citizen · 3/2
 *
 * When this creature enters, you may exile a creature card from your graveyard.
 * When you do, return target instant or sorcery card from your graveyard to your hand.
 *
 * Modeled as a reflexive trigger. The optional action selects a creature card in your
 * graveyard and exiles it (the "may exile" half); the reflexive payoff then targets an
 * instant or sorcery card in your graveyard to return to hand. If the action can't be
 * performed (no creature cards in your graveyard) the may-decision is skipped entirely,
 * so the reflexive return never fires.
 */
val KishlaTrawlers = card("Kishla Trawlers") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Human Citizen"
    power = 3
    toughness = 2
    oracleText = "When this creature enters, you may exile a creature card from your graveyard. " +
        "When you do, return target instant or sorcery card from your graveyard to your hand."

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = ReflexiveTriggerEffect(
            action = Effects.Composite(
                listOf(
                    Effects.SelectTarget(Targets.CreatureCardInYourGraveyard, storeAs = "exiledCreature"),
                    Effects.Exile(EffectTarget.PipelineTarget("exiledCreature"))
                )
            ),
            optional = true,
            reflexiveEffect = Effects.ReturnToHand(EffectTarget.ContextTarget(0)),
            reflexiveTargetRequirements = listOf(
                TargetObject(filter = TargetFilter.InstantOrSorceryInGraveyard.ownedByYou())
            ),
            descriptionOverride = "You may exile a creature card from your graveyard. " +
                "When you do, return target instant or sorcery card from your graveyard to your hand."
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "50"
        artist = "Iris Compiet"
        imageUri = "https://cards.scryfall.io/normal/front/1/9/190fbc55-e8e9-4077-9532-1de7406baabf.jpg?1743204163"
    }
}
