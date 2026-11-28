package com.wingedsheep.mtg.sets.definitions.isd.cards

import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Civilized Scholar // Homicidal Brute (Innistrad #47)
 * {2}{U}
 * Creature — Human Advisor 0/1 // Creature — Human Mutant 5/1
 *
 * Front — "{T}: Draw a card, then discard a card. If a creature card is discarded this way, untap
 *          this creature, then transform it."
 * Back  — "At the beginning of your end step, if this creature didn't attack this turn, tap this
 *          creature, then transform it."
 *
 * The front is a draw → choose-from-hand → discard pipeline; the discarded card stays named in its
 * collection after the move, so the creature check reads exactly the card discarded this way. The
 * untap and transform happen in the same resolution, so there's no priority window between them
 * (ruling).
 *
 * The back's intervening-if reads the permanent's own attack history, which survives transforming
 * (same object): a Scholar that attacked and transformed later that turn doesn't trigger (ruling).
 * A Brute that couldn't attack still taps and transforms (ruling).
 *
 * The back face has no mana cost, so its red comes from a color indicator (CR 204).
 */
private val CivilizedScholarFront = card("Civilized Scholar") {
    manaCost = "{2}{U}"
    colorIdentity = "UR"
    typeLine = "Creature — Human Advisor"
    power = 0
    toughness = 1
    oracleText = "{T}: Draw a card, then discard a card. If a creature card is discarded this way, " +
        "untap this creature, then transform it."

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.Pipeline {
            run(Effects.DrawCards(1))
            val hand = gather(CardSource.FromZone(Zone.HAND, Player.You))
            val discarded = chooseExactly(1, from = hand, prompt = "Choose a card to discard")
            discard(discarded)
            run(
                Effects.If(
                    condition = whenMatches(discarded, GameObjectFilter.Creature),
                    then = Effects.Untap(EffectTarget.Self) then Effects.Transform(EffectTarget.Self),
                )
            )
        }
        description = "Draw a card, then discard a card. If a creature card is discarded this way, " +
            "untap this creature, then transform it."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "47"
        artist = "Michael C. Hayes"
        flavorText = "\"Me, angry? Of course not. Thanks to my research, I'm above such petty emotions now.\""
        imageUri = "https://cards.scryfall.io/normal/front/7/b/7bf864db-4754-433d-9d77-6695f78f6c09.jpg?1783940983"
        ruling(
            "2011-09-22",
            "You don't have priority between untapping Civilized Scholar and transforming it. You can't " +
                "activate the draw-and-discard ability again, for example."
        )
        ruling(
            "2011-09-22",
            "If Civilized Scholar attacks, and later in the turn (but before the beginning of your end " +
                "step), it transforms, Homicidal Brute's last ability won't trigger. This is because the " +
                "creature attacked that turn, even if had its other face up at the time."
        )
        ruling("2011-09-22", "You'll tap and transform Homicidal Brute even if it couldn't attack.")
    }
}

private val HomicidalBrute = card("Homicidal Brute") {
    manaCost = ""
    colorIdentity = "UR"
    colorIndicator = "R" // Transformed back face, no mana cost (CR 204).
    typeLine = "Creature — Human Mutant"
    power = 5
    toughness = 1
    oracleText = "At the beginning of your end step, if this creature didn't attack this turn, tap this " +
        "creature, then transform it."

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.END)
        interveningIf = Conditions.Not(Conditions.SourceAttackedThisTurn)
        effect = Effects.Tap(EffectTarget.Self) then Effects.Transform(EffectTarget.Self)
        description = "At the beginning of your end step, if this creature didn't attack this turn, tap " +
            "this creature, then transform it."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "47"
        artist = "Michael C. Hayes"
        flavorText = "For a moment his basest desires run free, unshackled by reason or conscience."
        imageUri = "https://cards.scryfall.io/normal/back/7/b/7bf864db-4754-433d-9d77-6695f78f6c09.jpg?1783940983"
    }
}

val CivilizedScholar: CardDefinition = CardDefinition.doubleFacedCreature(
    frontFace = CivilizedScholarFront,
    backFace = HomicidalBrute,
)
