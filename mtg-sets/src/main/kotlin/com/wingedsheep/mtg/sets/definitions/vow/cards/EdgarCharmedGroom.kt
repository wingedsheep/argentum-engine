package com.wingedsheep.mtg.sets.definitions.vow.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.effects.ConditionalEffect
import com.wingedsheep.sdk.scripting.effects.TransformEffect
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Edgar, Charmed Groom // Edgar Markov's Coffin (Innistrad: Crimson Vow)
 * {2}{W}{B}
 * Legendary Creature — Vampire Noble // Legendary Artifact
 *
 * Front — Edgar, Charmed Groom (4/4)
 *   Other Vampires you control get +1/+1.
 *   When Edgar dies, return it to the battlefield transformed under its owner's control.
 *
 * Back — Edgar Markov's Coffin (Legendary Artifact)
 *   At the beginning of your upkeep, create a 1/1 white and black Vampire creature token with
 *   lifelink and put a bloodline counter on Edgar Markov's Coffin. Then if there are three or more
 *   bloodline counters on it, remove those counters and transform it.
 *
 * The front's anthem is a +1/+1 [ModifyStats] over other Vampires you control (excludeSelf, since
 * Edgar isn't "other"). The dies trigger returns Edgar transformed via
 * [Effects.ReturnSelfFromGraveyardTransformed] (Ojer Taq's idiom); "under its owner's control" is
 * the default for that effect. The back's upkeep trigger is a [Effects.Composite] of token +
 * counter + a [ConditionalEffect] gated on [Conditions.SourceCounterCountAtLeast] 3 that removes the
 * three counters and transforms (Treasure Map's counter-then-transform idiom). Modeled with
 * [CardDefinition.doubleFacedPermanent] because the back is an artifact, not a creature.
 */

private val EdgarCharmedGroomFront = card("Edgar, Charmed Groom") {
    manaCost = "{2}{W}{B}"
    colorIdentity = "WB"
    typeLine = "Legendary Creature — Vampire Noble"
    power = 4
    toughness = 4
    oracleText = "Other Vampires you control get +1/+1.\n" +
        "When Edgar dies, return it to the battlefield transformed under its owner's control."

    staticAbility {
        ability = ModifyStats(
            1, 1,
            GroupFilter(GameObjectFilter.Creature.youControl().withSubtype(Subtype.VAMPIRE), excludeSelf = true),
        )
    }

    triggeredAbility {
        trigger = Triggers.Dies
        effect = Effects.ReturnSelfFromGraveyardTransformed(tapped = false)
        description = "When Edgar dies, return it to the battlefield transformed under its owner's control."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "236"
        artist = "Volkan Baǵa"
        imageUri = "https://cards.scryfall.io/normal/front/6/3/63ba8eef-b834-4031-b0a1-0f8505d53813.jpg?1783924799"
    }
}

private val EdgarMarkovsCoffin = card("Edgar Markov's Coffin") {
    manaCost = ""
    colorIdentity = "WB"
    colorIndicator = "WB" // Transformed back face, no mana cost (CR 204).
    typeLine = "Legendary Artifact"
    oracleText = "At the beginning of your upkeep, create a 1/1 white and black Vampire creature " +
        "token with lifelink and put a bloodline counter on Edgar Markov's Coffin. Then if there are " +
        "three or more bloodline counters on it, remove those counters and transform it."

    triggeredAbility {
        trigger = Triggers.YourUpkeep
        effect = Effects.Composite(
            Effects.CreateToken(
                power = 1,
                toughness = 1,
                colors = setOf(Color.WHITE, Color.BLACK),
                creatureTypes = setOf("Vampire"),
                keywords = setOf(Keyword.LIFELINK),
                imageUri = "https://cards.scryfall.io/normal/front/7/e/7eee78d3-c65f-4454-bd3c-1c55388422f5.jpg?1783924693",
            ),
            Effects.AddCounters("bloodline", 1, EffectTarget.Self),
            ConditionalEffect(
                condition = Conditions.SourceCounterCountAtLeast("bloodline", 3),
                effect = Effects.Composite(
                    Effects.RemoveCounters("bloodline", 3, EffectTarget.Self),
                    TransformEffect(EffectTarget.Self),
                ),
            ),
        )
        description = "At the beginning of your upkeep, create a 1/1 white and black Vampire creature " +
            "token with lifelink and put a bloodline counter on Edgar Markov's Coffin. Then if there " +
            "are three or more bloodline counters on it, remove those counters and transform it."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "236"
        artist = "Volkan Baǵa"
        imageUri = "https://cards.scryfall.io/normal/back/6/3/63ba8eef-b834-4031-b0a1-0f8505d53813.jpg?1783924799"
    }
}

val EdgarCharmedGroom: CardDefinition = CardDefinition.doubleFacedPermanent(
    frontFace = EdgarCharmedGroomFront,
    backFace = EdgarMarkovsCoffin,
)
