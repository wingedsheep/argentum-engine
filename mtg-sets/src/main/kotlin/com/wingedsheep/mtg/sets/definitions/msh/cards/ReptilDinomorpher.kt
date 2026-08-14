package com.wingedsheep.mtg.sets.definitions.msh.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Reptil, Dinomorpher (MSH #182) — {G} Legendary Creature — Human Hero · 1/2
 *
 * Brontosaurus — {3}: Until end of turn, Reptil becomes a Dinosaur Hero with base power and
 * toughness 3/5 and gains vigilance and reach.
 * Tyrannosaurus Rex — {6}: Until end of turn, Reptil becomes a Dinosaur Hero with base power and
 * toughness 6/6 and gains trample.
 *
 * "Brontosaurus" and "Tyrannosaurus Rex" are ability words — flavor only, no rules meaning — so
 * they appear in the oracle text and the ability descriptions but nowhere in the script.
 *
 * Both abilities are the Restless Vinestalk animate shape: [Effects.BecomeCreature] on
 * [EffectTarget.Self] with a `Duration.EndOfTurn`. `creatureTypes` *replaces* the creature's
 * subtypes (a Layer 4 set effect), which is what "becomes a Dinosaur Hero" means — Reptil stops
 * being a Human. Setting *base* P/T is Layer 7b, so it is applied before any +N/+N in Layer 7c:
 * a Reptil with a +1/+1 counter animated with Brontosaurus is a 4/6, not a 3/5. Activating one
 * ability and then the other stacks two same-layer effects, and the later timestamp wins.
 */
val ReptilDinomorpher = card("Reptil, Dinomorpher") {
    manaCost = "{G}"
    colorIdentity = "G"
    typeLine = "Legendary Creature — Human Hero"
    power = 1
    toughness = 2
    oracleText = "Brontosaurus — {3}: Until end of turn, Reptil becomes a Dinosaur Hero with base " +
        "power and toughness 3/5 and gains vigilance and reach.\n" +
        "Tyrannosaurus Rex — {6}: Until end of turn, Reptil becomes a Dinosaur Hero with base " +
        "power and toughness 6/6 and gains trample."

    activatedAbility {
        cost = Costs.Mana("{3}")
        effect = Effects.BecomeCreature(
            target = EffectTarget.Self,
            power = 3,
            toughness = 5,
            keywords = setOf(Keyword.VIGILANCE, Keyword.REACH),
            creatureTypes = setOf("Dinosaur", "Hero"),
            duration = Duration.EndOfTurn,
        )
        description = "Brontosaurus — {3}: Until end of turn, Reptil becomes a Dinosaur Hero with " +
            "base power and toughness 3/5 and gains vigilance and reach."
    }

    activatedAbility {
        cost = Costs.Mana("{6}")
        effect = Effects.BecomeCreature(
            target = EffectTarget.Self,
            power = 6,
            toughness = 6,
            keywords = setOf(Keyword.TRAMPLE),
            creatureTypes = setOf("Dinosaur", "Hero"),
            duration = Duration.EndOfTurn,
        )
        description = "Tyrannosaurus Rex — {6}: Until end of turn, Reptil becomes a Dinosaur Hero " +
            "with base power and toughness 6/6 and gains trample."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "182"
        artist = "Paolo Parente"
        imageUri = "https://cards.scryfall.io/normal/front/4/7/472db3f2-23df-46c1-86c6-f35fbe5ab4e3.jpg?1783902913"
    }
}
