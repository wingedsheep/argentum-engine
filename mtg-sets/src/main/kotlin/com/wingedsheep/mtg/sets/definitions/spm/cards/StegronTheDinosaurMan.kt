package com.wingedsheep.mtg.sets.definitions.spm.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration

/**
 * Stegron the Dinosaur Man
 * {4}{R}
 * Legendary Creature — Dinosaur Villain
 * 5/4
 *
 * Menace
 * Dinosaur Formula — {1}{R}, Discard this card: Until end of turn, target creature
 * you control gets +3/+1 and becomes a Dinosaur in addition to its other types.
 *
 * "Dinosaur Formula" is an ability word (flavor label), so it adds no rules meaning.
 *
 * The second ability functions from the hand: its cost discards this card
 * ([Costs.DiscardSelf]) and it's activated from the hand zone
 * ([activateFromZone] = [Zone.HAND]) — cf. Steel Wrecking Ball for the from-hand
 * DiscardSelf pattern. The effect is a Composite of an until-end-of-turn +3/+1
 * ([Effects.ModifyStats]) and an until-end-of-turn creature-type add
 * ([Effects.AddCreatureType], which grants "Dinosaur" in addition to the target's
 * other types via Layer 4 without removing them).
 */
val StegronTheDinosaurMan = card("Stegron the Dinosaur Man") {
    manaCost = "{4}{R}"
    colorIdentity = "R"
    typeLine = "Legendary Creature — Dinosaur Villain"
    power = 5
    toughness = 4
    oracleText = "Menace (This creature can't be blocked except by two or more creatures.)\n" +
        "Dinosaur Formula — {1}{R}, Discard this card: Until end of turn, target creature you control " +
        "gets +3/+1 and becomes a Dinosaur in addition to its other types."

    keywords(Keyword.MENACE)

    // Dinosaur Formula — {1}{R}, Discard this card (from hand): Until end of turn,
    // target creature you control gets +3/+1 and becomes a Dinosaur in addition to
    // its other types.
    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{1}{R}"), Costs.DiscardSelf)
        activateFromZone = Zone.HAND
        val creature = target("target creature you control", Targets.CreatureYouControl)
        effect = Effects.Composite(
            Effects.ModifyStats(3, 1, creature, Duration.EndOfTurn),
            Effects.AddCreatureType("Dinosaur", creature, Duration.EndOfTurn)
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "95"
        artist = "John Tyler Christopher"
        flavorText = "\"It isss my intention to return the dinosaur to hisss rightful place as master of thisss earth.\""
        imageUri = "https://cards.scryfall.io/normal/front/4/8/485ceacb-fa76-4517-8466-c3c6bf6bcd6e.jpg?1783905330"
    }
}
